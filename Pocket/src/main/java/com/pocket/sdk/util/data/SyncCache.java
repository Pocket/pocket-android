package com.pocket.sdk.util.data;

import com.pocket.sdk.Pocket;
import com.pocket.sdk.api.generated.PocketRemoteStyle;
import com.pocket.sync.source.PendingResult;
import com.pocket.sync.source.Source;
import com.pocket.sync.source.result.SyncException;
import com.pocket.sync.source.subscribe.Subscription;
import com.pocket.sync.thing.Thing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A {@link DataSourceCache} backed by syncing and subscribing a {@link Thing} with {@link Source}.
 *
 * <h3>Usage</h3>
 * To use, you need to help describe what thing to sync, how to get a list of objects from that thing, how to handle paging and making subsets, etc.
 * To create an instance, use the builder pattern starting with {@link #from(Pocket)} to help guide you through these pieces.
 *
 * <h3>Implementation</h3>
 * This takes a Thing and breaks it into {@link Page}s using {@link SubsetApply#subset(Thing, Subset)}.
 * To load each page it will use Pocket.sync() and then subscribe to it as well to keep it up to date.
 * As pages are loaded/updated, it creates the full list by looping through each page, invoking {@link CollectionGet#collectionFrom(Thing)} and appending the results into one list.
 *
 * <h2>Thread Safety</h2>
 * This assumes (but doesn't enforce to avoid performance penalties of checking) all calls to it are on the ui thread.
 * It also assumes that the {@link Pocket.Config#publisher} publishes {@link com.pocket.sync.source.subscribe.Subscriber#onUpdate(Thing)} callbacks to the ui thread.
 * If that is not the case, there will be issues.
 */
public class SyncCache<C, T extends Thing> extends AbsDataSourceCache<C> {
	
	public static final int DEFAULT_PAGE_SIZE = 30;
	
	private final T identity;
	private final PagingStrategy<C> paging;
	private final SubsetApply<T> subsetApply;
	private final CollectionGet<C, T> collection;
	private final Pocket source;
	private final int pageSize;
	private final List<Page<C,T>> pages = new ArrayList<>();
	private final Map<Thing, Subscription> subs = new HashMap<>();

	public boolean forceRemote = false;
	
	public static class Config<C, T extends Thing>  {
		private final T identity;
		private final PagingStrategy<C> paging;
		private final int pageSize;
		private final SubsetApply<T> subsetApply;
		private final CollectionGet<C, T> collection;
		private final Pocket source;

		public Config(
				T identity,
				PagingStrategy<C> paging,
				SubsetApply<T> subsetApply,
				CollectionGet<C, T> collection,
				Pocket source
		) {
			this.pageSize = DEFAULT_PAGE_SIZE;
			this.identity = identity;
			this.paging = paging;
			this.subsetApply = subsetApply;
			this.collection = collection;
			this.source = source;
		}
	}

	private PendingResult<T, SyncException> pendingRefresh;
	
	public SyncCache(Config<C, T> config) {
		super();
		this.pageSize = config.pageSize;
		this.identity = config.identity;
		this.paging = config.paging;
		this.subsetApply = config.subsetApply;
		this.collection = config.collection;
		this.source = config.source;
	}

	/** Create an empty page instance of what the next page should be (identity wise) based on the current config. */
	private Page<C, T> nextPage() {
		if (paging != null) {
			Subset subset = new Subset(paging.getNextPageOffset(getList()), pageSize);
			T pageIdentity = subsetApply.subset(identity, subset);
			return new Page<>(subset, pageIdentity);
		} else {
			return new Page<>(null, identity);
		}
	}
	
	@Override
	protected void doLoadFirstPage() {
		clearData(); // This can also be called when retrying an empty list, so make sure we clear out all data and subscriptions and retry clean
		Page<C, T> first = nextPage();

		// Bind this callback to the first page, this does the initial load plus any future updates.
		var t = first.identity;
		// Also stop any subscription this replaces
		Subscription.stop(subs.put(t, source.bind(forceRemote, t, u -> {
			Throwable error = null;
			try {
				updatePage(first, u);
			} catch (Throwable e) {
				error = e;
			}
			if (pages.isEmpty()) {
				// First time
				check(error != null ? new Status.Error(error) : new Status.Loaded<>(first));
			} else {
				// Update
				invalidateList();
			}
		}, (e, sub) -> check(new Status.Error(e)))));
	}

	/**
	 * This will run after the thing is loaded to check if we are ready to display a result.
	 * We'll wait until it has some result before we do anything.
	 */
	private void check(Status status) {
		Throwable error = null;
		Page<C, T> loaded = null;
		if (status instanceof Status.Error) {
			error = ((Status.Error) status).throwable;
		} else if (status instanceof Status.Loaded) {
			loaded = ((Status.Loaded<C, T>) status).page;
		} else {
			throw new RuntimeException("unexpected status " + status);
		}
		// Ok we have a result, otherwise it would have stopped above
		if (error != null) {
			clearData();
			Throwable e = error;
			setError(new Error() {
				@Override public void retry() { loadFirstPage(); }
				@Override public Throwable getError() { return e;}
			}, LoadState.INITIAL_ERROR);
		} else {
			// Show the list
			pages.add(loaded);
			invalidateList();
		}
	}

	@Override
	protected void doLoadNextPage() {
		Page<C, T> next = nextPage();
		Subscription.stop(subs.put(next.identity, source.bind(forceRemote, next.identity, u -> { // The Subscription.stop() ensures if this ends up replacing an existing subscription it is stopped properly.
			if (!pages.contains(next)) pages.add(next);
			try {
				updatePage(next, u);
			} catch (Throwable t) {
				setError(
						new Error() {
							@Override public void retry() { loadNextPage(); }
							@Override public Throwable getError() { return t; }
						},
						LoadState.LOADED_APPEND_ERROR
				);
			}
			invalidateList();
		},
		(e, sub) -> {
			sub.stop();
			subs.remove(next.identity);
			setError(new Error() {
				@Override public void retry() { loadNextPage(); }
				@Override public Throwable getError() { return e;}
			}, LoadState.LOADED_APPEND_ERROR);
		})));
	}
	
	private void updatePage(Page<C, T> page, T updated) {
		if (paging != null) paging.onDataReturned(page.subset);
		page.data.clear();
		
		List<C> data = collection.collectionFrom(updated);
		if (data != null) page.data.addAll(data);
	}

	/**
	 * Invalidates the current list and notifies any listeners of changes in Item state.
	 */
	private void invalidateList() {
		// Merge all pages together into a single list
		List<C> list = new ArrayList<>();
		for (Page<C, T> page : pages) {
			list.addAll(page.data);
		}
		
		// Check if paging is completed
		boolean isPagingComplete;
		Page<C,T> last = pages.isEmpty() ? null : pages.get(pages.size()-1);
		if (last == null) {
			isPagingComplete = false;
		} else if (paging == null) {
			isPagingComplete = true;
		} else {
			isPagingComplete = paging.isPagingComplete(last.subset, last.data);
		}

		// Apply this new list
		setList(list, isPagingComplete);
	}
	
	@Override
	protected void doRefresh() {
		/*
			Refreshes keep the existing data and only replace it if it is successful.
			Since we are already bound to the first page, all we do is syncRemote it, if there are changes, we'll pick them up automatically.
			If successful, we'll trim off the pages other than the first.
			To keep things simple for now, dependencies are a best attempt refresh and don't have an impact on error or success here.
		 */
		Page<C,T> firstPage = pages.get(0);
		if (firstPage.identity.remote().style == PocketRemoteStyle.LOCAL) {
			pendingRefresh = source.sync(firstPage.identity);
		} else {
			pendingRefresh = source.syncRemote(firstPage.identity);
		}
		pendingRefresh
			.onSuccess(p -> {
				// Our subscription should pick it up any changes
				// Clear all but the first page so they are re-paged as needed
				pages.clear();
				pages.add(firstPage);
				invalidateList();
			})
			.onFailure(e -> setError(new Error() {
				@Override public void retry() { refresh(); }
				@Override public Throwable getError() { return e;}
			}, LoadState.LOADED_REFRESH_ERROR))
			.onComplete(() -> pendingRefresh = null);
	}
	
	private void clearData() {
		for (Subscription sub : subs.values()) {
			sub.stop();
		}
		subs.clear();
		pages.clear();
		if (pendingRefresh != null) {
			pendingRefresh.abandon();
			pendingRefresh = null;
		}
		if (paging != null) paging.onReset();
	}

	public Thing identity() {
		return identity;
	}
	
	/** Describes how to apply a subset to a thing. */
	public interface SubsetApply<T extends Thing> {
		/** @return A new instance of T with the subset applied to it */
		T subset(T identity, Subset subset);
	}
	
	public static class Subset {
		public final int offset;
		public final int count;
		public Subset(int offset, int count) {
			this.offset = offset;
			this.count = count;
		}
	}
	
	private static class Page<C, T extends Thing> {
		public final Subset subset;
		public final T identity;
		public final List<C> data = new ArrayList<>();
		
		private Page(Subset subset, T identity) {
			this.subset = subset;
			this.identity = identity;
		}
		
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Page<?, ?> page = (Page<?, ?>) o;
			return identity.equals(page.identity);
		}
		
		@Override
		public int hashCode() {
			return identity.hashCode();
		}
	}
	
	/** Describes how to convert a thing to a list of objects. */
	public interface CollectionGet<C, T extends Thing> {
		/** @return the list of objects this thing represents. */
		List<C> collectionFrom(T thing);
	}
	
	/**
	 * Calculates the offset to use when requesting the next page of data from the endpoint.
	 * @see PagingStrategies for pre-made common ones.
	 */
	public interface PagingStrategy<C> {
		int getNextPageOffset(List<C> data);
		
		/**
		 * Results have been returned from an endpoint.
		 */
		void onDataReturned(Subset subset);
		
		/**
		 * The {@link DataSourceCache} was reset.
		 */
		void onReset();
		
		boolean isPagingComplete(Subset subset, List<C> lastPage);
	}
	
	public static class PagingStrategies {
		/**
		 * Returns the next index/position that we don't have, based on number
		 * of items in the data.
		 */
		public static class NextPosition<C> implements PagingStrategy<C> {
			@Override
			public int getNextPageOffset(List<C> data) {
				return data.size();
			}
			
			@Override
			public void onDataReturned(Subset subset) {}
			
			@Override
			public void onReset() {}
			
			@Override
			public boolean isPagingComplete(Subset subset, List<C> lastPage) {
				return lastPage.size() < subset.count;
			}
		}
		
		/**
		 * Remembers the last requested offset, and returns the next one,
		 * regardless of how many items are in the data set currently.
		 */
		public static class NextPage<C> implements PagingStrategy<C> {
			
			private int next = 0;
			
			@Override
			public int getNextPageOffset(List<C> data) {
				return next;
			}
			
			@Override
			public void onDataReturned(Subset subset) {
				next = subset.offset + subset.count;
			}
			
			@Override
			public void onReset() {
				next = 0;
			}
			
			@Override
			public boolean isPagingComplete(Subset subset, List<C> lastPage) {
				return lastPage.isEmpty();
			}
		}
	}

	/**
	 * Create a new builder.
	 * @param source The source to sync and subscribe from.
	 * @return The next builder step.
	 */
	public static Builder from(Pocket source) {
		return new Builder(source);
	}
	public static class Builder {
		Pocket source;
		private Builder(Pocket source) {
			this.source = source;
		}
		/**
		 * The thing to sync and subscribe to.
		 * @param identity The thing, only the identity fields are needed.
		 * @return The next builder step.
		 */
		public <T extends Thing> BuilderStep2<T> sync(T identity) {
			return new BuilderStep2<>(identity, source);
		}
	}
	public static class BuilderStep2<T extends Thing> {
		private final T t;
		private final Pocket source;
		private BuilderStep2(T t, Pocket source) {
			this.t = t;
			this.source = source;
		}
		/**
		 * How to convert the thing to a list of data objects to display.
		 * @return The next builder step.
		 */
		public <C> BuilderStep3<C,T> display(CollectionGet<C, T> collection) {
			return new BuilderStep3<>(this, collection);
		}
	}
	public static class BuilderStep3<C, T extends Thing> {
		private final BuilderStep2<T> b;
		private final CollectionGet<C, T> collection;
		private BuilderStep3(BuilderStep2<T> b, CollectionGet<C, T> collection) {
			this.b = b;
			this.collection = collection;
		}
		/**
		 * Page by using a {@link PagingStrategies.NextPage} strategy and describe how to apply a subset (count and offset) to the thing.
		 * @return The next builder step.
		 */
		public BuilderStep4<C,T> pageByOffset(SubsetApply<T> subsetApply) {
			return new BuilderStep4<>(this, new PagingStrategies.NextPage<>(), subsetApply);
		}
		/**
		 * Page by using a {@link PagingStrategies.NextPosition} strategy and describe how to apply a subset (count and offset) to the thing.
		 * @return The next builder step.
		 */
		public BuilderStep4<C,T> pageByPosition(SubsetApply<T> subsetApply) {
			return new BuilderStep4<>(this, new PagingStrategies.NextPosition<>(), subsetApply);
		}
		/**
		 * Do not page, use the identity as is, without modifying or paging.
		 * @return The next builder step.
		 */
		public BuilderStep4<C,T> noPaging() {
			return new BuilderStep4<>(this, null, null);
		}
	}
	public static class BuilderStep4<C, T extends Thing> {
		private final Config<C, T> config;
		
		private BuilderStep4(BuilderStep3<C, T> b, PagingStrategy<C> paging, SubsetApply<T> subsetApply) {
			this.config = new Config<>(b.b.t, paging, subsetApply, b.collection, b.b.source);
		}

		/** @return A config with these settings. */
		public Config<C,T> config() {
			return new Config<>(config.identity, config.paging, config.subsetApply, config.collection, config.source);
		}
		/** @return A cache with these settings. */
		public SyncCache<C,T> build() {
			return new SyncCache<>(config());
		}
	}

	private interface Status {
		class Loaded<C, T extends Thing> implements Status {
			final Page<C, T> page;
			public Loaded(Page<C, T> page) {
				this.page = page;
			}
		}
		class Error implements Status {
			final Throwable throwable;
			public Error(Throwable exception) {
				this.throwable = exception;
			}
		}
	}
}
