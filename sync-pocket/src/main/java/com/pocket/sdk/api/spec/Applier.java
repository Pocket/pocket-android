package com.pocket.sdk.api.spec;

import com.pocket.sdk.api.generated.PocketApplier;
import com.pocket.sdk.api.generated.action.Acctchange;
import com.pocket.sdk.api.generated.action.Add;
import com.pocket.sdk.api.generated.action.AddAlias;
import com.pocket.sdk.api.generated.action.AddAnnotation;
import com.pocket.sdk.api.generated.action.ApproveAccess;
import com.pocket.sdk.api.generated.action.Archive;
import com.pocket.sdk.api.generated.action.ClearAdzerkSpocs;
import com.pocket.sdk.api.generated.action.ClearUnleashAssignmentOverride;
import com.pocket.sdk.api.generated.action.Delete;
import com.pocket.sdk.api.generated.action.DeleteAlias;
import com.pocket.sdk.api.generated.action.DeleteAnnotation;
import com.pocket.sdk.api.generated.action.DeregisterPush;
import com.pocket.sdk.api.generated.action.DeregisterPushV2;
import com.pocket.sdk.api.generated.action.FakePremiumStatus;
import com.pocket.sdk.api.generated.action.Favorite;
import com.pocket.sdk.api.generated.action.FetchCompleted;
import com.pocket.sdk.api.generated.action.FollowAllUsers;
import com.pocket.sdk.api.generated.action.FollowUser;
import com.pocket.sdk.api.generated.action.HideAdzerkSpoc;
import com.pocket.sdk.api.generated.action.Logout;
import com.pocket.sdk.api.generated.action.MarkAsNotViewed;
import com.pocket.sdk.api.generated.action.MarkAsViewed;
import com.pocket.sdk.api.generated.action.NotificationAction;
import com.pocket.sdk.api.generated.action.NotificationPushAction;
import com.pocket.sdk.api.generated.action.OverrideUnleashAssignment;
import com.pocket.sdk.api.generated.action.PostDelete;
import com.pocket.sdk.api.generated.action.PostLike;
import com.pocket.sdk.api.generated.action.PostRemoveLike;
import com.pocket.sdk.api.generated.action.PostRemoveRepost;
import com.pocket.sdk.api.generated.action.Purchase;
import com.pocket.sdk.api.generated.action.Readd;
import com.pocket.sdk.api.generated.action.RecentSearch;
import com.pocket.sdk.api.generated.action.RederiveItems;
import com.pocket.sdk.api.generated.action.RegisterPush;
import com.pocket.sdk.api.generated.action.RegisterPushV2;
import com.pocket.sdk.api.generated.action.RegisterSocialToken;
import com.pocket.sdk.api.generated.action.ReportArticleView;
import com.pocket.sdk.api.generated.action.ReportFeedItem;
import com.pocket.sdk.api.generated.action.ResendEmailConfirmation;
import com.pocket.sdk.api.generated.action.ResetOfflineStatuses;
import com.pocket.sdk.api.generated.action.ResolveTweet;
import com.pocket.sdk.api.generated.action.Scrolled;
import com.pocket.sdk.api.generated.action.SetAvatar;
import com.pocket.sdk.api.generated.action.SetSiteLoginStatus;
import com.pocket.sdk.api.generated.action.ShareAdded;
import com.pocket.sdk.api.generated.action.ShareIgnored;
import com.pocket.sdk.api.generated.action.SharePost;
import com.pocket.sdk.api.generated.action.SharedTo;
import com.pocket.sdk.api.generated.action.TagDelete;
import com.pocket.sdk.api.generated.action.TagRename;
import com.pocket.sdk.api.generated.action.TagsAdd;
import com.pocket.sdk.api.generated.action.TagsClear;
import com.pocket.sdk.api.generated.action.TagsRemove;
import com.pocket.sdk.api.generated.action.TagsReplace;
import com.pocket.sdk.api.generated.action.UndoArchive;
import com.pocket.sdk.api.generated.action.UndoDelete;
import com.pocket.sdk.api.generated.action.Unfavorite;
import com.pocket.sdk.api.generated.action.UnfollowUser;
import com.pocket.sdk.api.generated.action.UpdateLoggedInAccount;
import com.pocket.sdk.api.generated.action.UpdateOfflineStatus;
import com.pocket.sdk.api.generated.action.UpdateUserSetting;
import com.pocket.sdk.api.generated.enums.AdzerkPlacementName;
import com.pocket.sdk.api.generated.enums.AttributionTypeId;
import com.pocket.sdk.api.generated.enums.ItemStatus;
import com.pocket.sdk.api.generated.enums.OfflineStatus;
import com.pocket.sdk.api.generated.enums.PositionType;
import com.pocket.sdk.api.generated.enums.SharedItemStatus;
import com.pocket.sdk.api.generated.enums.UserSettingKey;
import com.pocket.sdk.api.generated.thing.AcEmail;
import com.pocket.sdk.api.generated.thing.Account;
import com.pocket.sdk.api.generated.thing.AdzerkSpoc;
import com.pocket.sdk.api.generated.thing.AdzerkSpocs;
import com.pocket.sdk.api.generated.thing.Annotation;
import com.pocket.sdk.api.generated.thing.AttributionSaveInfo;
import com.pocket.sdk.api.generated.thing.FeedItem;
import com.pocket.sdk.api.generated.thing.Friend;
import com.pocket.sdk.api.generated.thing.Get;
import com.pocket.sdk.api.generated.thing.GetSuggestedFollows;
import com.pocket.sdk.api.generated.thing.HiddenSpoc;
import com.pocket.sdk.api.generated.thing.Item;
import com.pocket.sdk.api.generated.thing.ItemMeta;
import com.pocket.sdk.api.generated.thing.LoginInfo;
import com.pocket.sdk.api.generated.thing.Notification;
import com.pocket.sdk.api.generated.thing.NotificationButton;
import com.pocket.sdk.api.generated.thing.Position;
import com.pocket.sdk.api.generated.thing.Post;
import com.pocket.sdk.api.generated.thing.PostCount;
import com.pocket.sdk.api.generated.thing.PostLikeStatus;
import com.pocket.sdk.api.generated.thing.PostRepostStatus;
import com.pocket.sdk.api.generated.thing.Profile;
import com.pocket.sdk.api.generated.thing.RecentFriends;
import com.pocket.sdk.api.generated.thing.RecentSearches;
import com.pocket.sdk.api.generated.thing.SearchQuery;
import com.pocket.sdk.api.generated.thing.SharedItem;
import com.pocket.sdk.api.generated.thing.StfRecipient;
import com.pocket.sdk.api.generated.thing.SyncState;
import com.pocket.sdk.api.generated.thing.Tag;
import com.pocket.sdk.api.generated.thing.Tags;
import com.pocket.sdk.api.generated.thing.Tweet;
import com.pocket.sdk.api.generated.thing.Unleash;
import com.pocket.sdk.api.generated.thing.UnleashAssignment;
import com.pocket.sdk.api.generated.thing.UserFollow;
import com.pocket.sdk.api.generated.thing.UserSetting;
import com.pocket.sdk.api.source.V3Source;
import com.pocket.sdk.api.thing.AccountUtil;
import com.pocket.sdk.api.thing.ItemUtil;
import com.pocket.sdk.api.thing.TagUtil;
import com.pocket.sdk.api.value.DateString;
import com.pocket.sdk.api.value.Timestamp;
import com.pocket.sdk.api.value.UrlString;
import com.pocket.sync.action.Action;
import com.pocket.sync.space.Space;
import com.pocket.sync.thing.Thing;
import com.pocket.sync.value.Include;
import com.pocket.util.java.Safe;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Implementation of all Pocket actions.
 */
class Applier extends PocketApplier {
	
	@Override
	public PocketSpec spec() {
		return (PocketSpec) super.spec();
	}

	@Override
	protected void logout(Logout action, Space space) {
		space.clear();
	}
	
	@Override
	protected void notification_action(NotificationAction action, Space space) {
		notificationAction(space, action.time, action.cxt_notification_id, action.cxt_action_name);
	}
	
	@Override
	protected void notification_push_action(NotificationPushAction action, Space space) {
		notificationAction(space, action.time, action.cxt_notification_id, action.cxt_action_name);
	}
	
	/**
	 * Implementation for both {@link #notification_push_action(NotificationPushAction, Space)} and {@link #notification_action(NotificationAction, Space)}
	 */
	private void notificationAction(Space space, Timestamp action_time, String notification_id, String action_name) {
		Notification n = space.get(spec().things().notification().user_notification_id(notification_id).build());
		if (n != null && n.notification_actions != null) {
			for (NotificationButton a : n.notification_actions) {
				if (a.action_name.equals(action_name)) {
					List<NotificationButton> copy = new ArrayList<>(n.notification_actions);
					int index = n.notification_actions.indexOf(a);
					copy.set(index, a.builder()
							.taken_time(action_time)
							.enabled(false)
							.build());
					space.imprint(n.builder().notification_actions(copy).build());
					break;
				}
			}
		}
	}
	
	@Override
	protected void add(Add action, Space space) {
		Item item = findItem(space, action.item, action.item_id, action.url, spec());
		Item.Builder builder;
		if (item != null) {
			builder = item.builder();
		} else {
			if (action.item != null) {
				item = action.item;
			} else {
				item = ItemUtil.create(action.url.url, spec());
			}
			builder = item.builder();
			if (action.declared.title) {
				builder.given_title(action.title);
			}
		}
		builder.status(ItemStatus.UNREAD);
		builder.time_updated(action.time);
		builder.time_added(action.time);
		if (!StringUtils.isBlank(action.ref_id)) {
			builder.meta(new ItemMeta.Builder()._1(action.ref_id).build());
		} else if (!StringUtils.isBlank(action.tweet_id)) {
			builder.meta(new ItemMeta.Builder()._1(action.tweet_id).build());
		}
		if (action.tags != null) {
			List<Tag> tags = Safe.nonNullCopy(item.tags);
			tags.addAll(TagUtil.cleanTagsStrs(action.tags));
			builder.tags(tags);
		}
		if (action.attribution_detail != null && !action.attribution_detail.isEmpty()) {
			AttributionSaveInfo attr = action.attribution_detail.get(0);
			if (attr.attribution_type_id == AttributionTypeId.POST) {
				Post post = space.get(new Post.Builder().post_id(attr.source_id).build());
				if (post == null && action.post.post_id.equals(attr.source_id)) {
					post = action.post;
				}
				List<Post> posts = Safe.nonNullCopy(item.posts);
				posts.add(post);
				builder.posts(posts);
			}
		}
		space.imprint(builder.build());
	}
	
	@Override
	protected void readd(Readd action, Space space) {
		add(Add.from(action.toJson(V3Source.JSON_CONFIG, Include.DANGEROUS), V3Source.JSON_CONFIG), space);
	}
	
	@Override
	protected void archive(Archive action, Space space) {
		editItem(spec(), space, action.item_id, action.url, found -> found.builder()
				.status(ItemStatus.ARCHIVED)
				.time_read(action.time)
				.time_updated(action.time)
				.build());
	}
	
	@Override
	protected void delete(Delete action, Space space) {
		editItem(spec(), space, action.item_id, action.url, found -> found.builder()
				.status(ItemStatus.DELETED)
				.time_updated(action.time)
				.build());
	}
	
	@Override
	protected void favorite(Favorite action, Space space) {
		editItem(spec(), space, action.item_id, action.url, found -> found.builder()
				.favorite(true)
				.time_updated(action.time)
				.time_favorited(action.time)
				.build());
	}
	
	@Override
	protected void unfavorite(Unfavorite action, Space space) {
		editItem(spec(), space, action.item_id, action.url, found -> found.builder()
				.favorite(false)
				.time_updated(action.time)
				.build());
	}
	
	@Override
	protected void undo_archive(UndoArchive action, Space space) {
		editItem(spec(), space, action.item_id, action.url, found -> found.builder()
				.status(ItemStatus.UNREAD)
				.time_read(null)
				.build());
	}
	
	@Override
	protected void undo_delete(UndoDelete action, Space space) {
		editItem(spec(), space, action.item_id, action.url, found -> found.builder()
				.status(action.old_status)
				.build());
	}
	
	@Override
	protected void scrolled(Scrolled action, Space space) {
		editItem(spec(), space, action.item_id, action.url, found -> {
			PositionType view = action.view;
			Map<String, Position> positions = Safe.nonNullCopy(found.positions);
			Position oldPosition = positions.get(view.toString());
			Position newPosition = new Position.Builder()
					.view(view)
					.percent(action.percent)
					.node_index(action.node_index)
					.page(action.page)
					.section(action.section)
					.time_spent(oldPosition != null ? Safe.value(oldPosition.time_spent) + Safe.value(action.time_spent) : action.time_spent)
					.time_updated(action.time)
					.scroll_position(action.scroll_position)
					.build();
			positions.put(view.toString(), newPosition);
			return found.builder()
					.positions(positions)
					.time_updated(action.time)
					.build();
		});
	}
	
	@Override
	protected void add_annotation(AddAnnotation action, Space space) {
		editItem(spec(), space, action.item_id, action.url, found -> {
			List<Annotation> annotations = Safe.nonNullCopy(found.annotations);
			annotations.add(new Annotation.Builder(action.annotation)
					.created_at(DateString.fromMillis(action.time.millis()))
					.build());
			return found.builder()
					.annotations(annotations)
					.time_updated(action.time)
					.build();
		});
	}

	@Override
	protected void delete_annotation(DeleteAnnotation action, Space space) {
		editItem(spec(), space, action.item_id, action.url, found -> {
			List<Annotation> annotations = Safe.nonNullCopy(found.annotations);
			annotations.remove(new Annotation.Builder().annotation_id(action.annotation_id).build());
			return found.builder()
					.annotations(annotations)
					.time_updated(action.time)
					.build();
		});
	}

	@Override
	protected void tags_add(TagsAdd action, Space space) {
		editItem(spec(), space, action.item_id, action.url, found -> {
			List<Tag> tags = Safe.nonNullCopy(found.tags);
			for (Tag tag : TagUtil.cleanTagsStrs(action.tags)) {
				int index = TagUtil.indexOfTag(tags, tag);
				if (index < 0) {
					tags.add(tag);
				}
			}
			return found.builder()
					.tags(tags)
					.time_updated(action.time)
					.build();
		});
	}
	
	@Override
	protected void tags_replace(TagsReplace action, Space space) {
		editItem(spec(), space, action.item_id, action.url, found -> found.builder()
				.tags(TagUtil.cleanTagsStrs(action.tags))
				.time_updated(action.time)
				.build());
	}
	
	@Override
	protected void tags_remove(TagsRemove action, Space space) {
		editItem(spec(), space, action.item_id, action.url, found -> {
			List<Tag> tags = Safe.nonNullCopy(found.tags);
			for (Tag tag : TagUtil.cleanTagsStrs(action.tags)) {
				int index = TagUtil.indexOfTag(tags, tag);
				if (index >= 0) {
					tags.remove(index);
				}
			}
			return found.builder()
					.tags(tags)
					.time_updated(action.time)
					.build();
		});
	}
	
	@Override
	protected void tags_clear(TagsClear action, Space space) {
		editItem(spec(), space, action.item_id, action.url, found -> found.builder()
				.tags(new ArrayList<>())
				.time_updated(action.time)
				.build());
	}
	
	@Override
	protected void tag_rename(TagRename action, Space space) {
		Tag oldTag = TagUtil.clean(action.old_tag);
		Tag newTag = TagUtil.clean(action.new_tag); // REVIEW who/where should do validation?
		if (oldTag != null && newTag != null) {
			// Update all Item's that have this tag
			space.imprintManyWhere(Item.THING_TYPE, Item.class, found -> TagUtil.indexOfTag(found.tags, oldTag) >= 0, thing -> {
				List<Tag> tags = new ArrayList<>(thing.tags);
				tags.remove(oldTag);
				if (!tags.contains(newTag)) tags.add(newTag);
				return thing.builder()
						.tags(tags)
						.time_updated(action.time)
						.build();
			});
			// Update the tags list
			space.imprintManyWhere(Get.THING_TYPE, Get.class, found -> found.tags != null && found.tags.contains(oldTag.tag), thing -> {
				List<String> tags = new ArrayList<>(thing.tags);
				tags.remove(oldTag.tag);
				if (!tags.contains(newTag.tag)) tags.add(newTag.tag);
				return thing.builder().tags(tags).build();
			});
			Tags tags = space.get(spec().things().tags().build());
			if (tags != null && tags.tags != null) {
				List<String> taglist = new ArrayList<>(tags.tags);
				taglist.remove(oldTag.tag);
				if (!taglist.contains(newTag.tag)) taglist.add(newTag.tag);
				space.imprint(tags.builder().tags(taglist).build());
			}
		}
	}
	
	@Override
	protected void tag_delete(TagDelete action, Space space) {
		Tag tag = TagUtil.clean(action.tag);
		if (tag != null) {
			// Update all Item's that have this tag
			space.imprintManyWhere(Item.THING_TYPE, Item.class, found -> TagUtil.indexOfTag(found.tags, tag) >= 0, thing -> {
				List<Tag> tags = new ArrayList<>(thing.tags);
				tags.remove(TagUtil.indexOfTag(tags, tag));
				return thing.builder()
						.tags(tags)
						.time_updated(action.time)
						.build();
			});
			// Update the tags list
			space.imprintManyWhere(Get.THING_TYPE, Get.class, found -> found.tags != null && found.tags.contains(tag.tag), thing -> {
				List<String> tags = new ArrayList<>(thing.tags);
				tags.remove(tag.tag);
				return thing.builder()
						.tags(tags)
						.build();
			});
			Tags tags = space.get(spec().things().tags().build());
			if (tags != null && tags.tags != null) {
				List<String> taglist = new ArrayList<>(tags.tags);
				taglist.remove(tag.tag);
				space.imprint(tags.builder().tags(taglist).build());
			}
		}
	}
	
	@Override
	protected void post_like(PostLike action, Space space) {
		PostLikeStatus status = spec().things().postLikeStatus()
				.post_id(action.post_id)
				.profile_id(getLoggedInProfile(space).uid)
				.time_added(action.time)
				.build();
		status = getOrDefault(status, space).builder().status(true).build();
		Post post = space.get(new Post.Builder().post_id(action.post_id).build());
		
		space.imprint(status);
		
		if (post != null) {
			PostCount count = post.like_count;
			if (!Safe.value(post.like_status)) {
				count = count.builder().count(Math.max(count.count + 1, 0)).build();
			}
			space.imprint(post.builder()
					.like_status(true)
					.like_count(count)
					.build());
		}
	}
	
	@Override
	protected void post_remove_like(PostRemoveLike action, Space space) {
		PostLikeStatus status = spec().things().postLikeStatus().post_id(action.post_id).profile_id(getLoggedInProfile(space).uid).build();
		status = getOrDefault(status, space).builder().status(false).build();
		Post post = space.get(new Post.Builder().post_id(action.post_id).build());
		
		space.imprint(status);
		
		if (post != null) {
			PostCount count = post.like_count;
			if (Safe.value(post.like_status)) {
				count = count.builder().count(count.count - 1).build();
			}
			space.imprint(post.builder()
					.like_status(false)
					.like_count(count)
					.build());
		}
	}
	
	@Override
	protected void share_post(SharePost action, Space space) {
		if (action.original_post_id != null) {
			// Repost
			Profile user = getLoggedInProfile(space);
			PostRepostStatus status = spec().things().postRepostStatus()
					.post_id(action.original_post_id)
					.profile_id(user.uid)
					.time_added(action.time)
					.build();
			status = getOrDefault(status, space).builder().status(false).build();
			Post post = space.get(new Post.Builder().post_id(action.original_post_id).build());
			
			space.imprint(status);
			
			if (post != null && !post.profile.equals(user)) {
				PostCount count = post.repost_count;
				if (!Safe.value(post.repost_status)) {
					count = count.builder().count(count.count + 1).build();
				}
				space.imprint(post.builder()
						.repost_status(true)
						.repost_count(count)
						.build());
			}
		} else {
			// New Post / Recommendation
			// We can't do this locally, so will have to wait for the server to handle.
		}
	}
	
	@Override
	protected void post_remove_repost(PostRemoveRepost action, Space space) {
		Profile user = getLoggedInProfile(space);
		PostRepostStatus status = spec().things().postRepostStatus().post_id(action.post_id).profile_id(user.uid).build();
		status = getOrDefault(status, space).builder().status(false).build();
		Post post = space.get(new Post.Builder().post_id(action.post_id).build());
		
		space.imprint(status);
		
		if (post != null) {
			PostCount count = post.repost_count;
			if (Safe.value(post.repost_status)) {
				count = count.builder().count(Math.max(0, count.count - 1)).build();
			}
			Post.Builder b = post.builder()
					.repost_status(false)
					.repost_count(count);
			
			if (post.profile.equals(user)) {
				b.deleted(true);
			}
			space.imprint(b.build());
		}
	}
	
	@Override
	protected void post_delete(PostDelete action, Space space) {
		Post post = space.get(new Post.Builder().post_id(action.post_id).build());
		if (post != null) {
			space.imprint(post.builder()
					.deleted(true)
					.build());
		}
	}
	
	@Override
	protected void report_feed_item(ReportFeedItem action, Space space) {
		FeedItem fi = space.get(new FeedItem.Builder().feed_item_id(action.feed_item_id).build());
		if (fi != null) {
			space.imprint(fi.builder().reported(true).build());
		}
	}
	
	@Override
	protected void follow_user(FollowUser action, Space space) {
		String uid = getLoggedInProfile(space).uid;
		List<UserFollow> follows = new ArrayList<>(action.user_list.size());
		for (String follow_uid : action.user_list) {
			follows.add(new UserFollow.Builder()
					.status(true)
					.user_id(uid)
					.follow_user_id(follow_uid)
					.time_updated(action.time)
					.build());
		}
		space.imprint(follows);
	}
	
	@Override
	protected void unfollow_user(UnfollowUser action, Space space) {
		String uid = getLoggedInProfile(space).uid;
		List<UserFollow> follows = new ArrayList<>(action.user_list.size());
		for (String follow_uid : action.user_list) {
			follows.add(new UserFollow.Builder()
					.status(false)
					.user_id(uid)
					.follow_user_id(follow_uid)
					.time_updated(action.time)
					.build());
		}
		space.imprint(follows);
	}
	
	@Override
	protected void follow_all_users(FollowAllUsers action, Space space) {
		// The client may not have all the details, so we just try what we can.
		
		// First make a best effort locally
		String self = getLoggedInProfile(space).uid;
		List<UserFollow> follows = new ArrayList<>();
		Collection<GetSuggestedFollows> sets = space.getAll(GetSuggestedFollows.THING_TYPE, GetSuggestedFollows.class);
		for (GetSuggestedFollows t : sets) {
			if (t.social_service == action.social_service) {
				for (Profile p : t.suggested_follows) {
					if (!p.is_following) {
						follows.add(new UserFollow.Builder()
								.status(true)
								.user_id(self)
								.follow_user_id(p.uid)
								.build());
					}
				}
			}
		}
		if (!follows.isEmpty()) {
			space.imprint(follows);
		}
		
		space.addInvalid(AccountUtil.getuser(spec()));
	}
	
	@Override
	protected void shared_to(SharedTo action, Space space) {
		for (StfRecipient r : action.to) {
			if (r.friend_id != null) {
				Friend f = space.get(spec().things().friend().friend_id(r.friend_id).build());
				if (f != null) {
					space.imprint(f.builder()
							.time_shared(action.time)
							.build());
					RecentFriends rf = space.get(spec().things().recentFriends().build());
					if (rf != null) {
						List<Friend> recents = Safe.nonNullCopy(rf.recent_friends);
						recents.remove(f);
						recents.add(0, f);
						space.imprint(rf.builder().recent_friends(recents).build());
					}
				}
			} else if (r.email != null) {
				AcEmail e = getOrDefault(spec().things().acEmail().email(r.email).build(), space);
				e = e.builder().time_shared(action.time).build();
				space.imprint(e);
			}
		}
	}
	
	@Override
	protected void share_ignored(ShareIgnored action, Space space) {
		SharedItem s = space.get(spec().things().sharedItem().share_id(action.share_id).build());
		if (s != null) {
			space.imprint(s.builder()
					.status(SharedItemStatus.DISCARDED)
					.time_ignored(action.time)
					.build());
		}
	}
	
	@Override
	protected void share_added(ShareAdded action, Space space) {
		final Add add = new Add.Builder().url(action.url)
				.item_id(action.item_id)
				.time(action.time)
				.context(action.context)
				.build();
		add(add, space);
		
		SharedItem si = space.get(new SharedItem.Builder().share_id(action.share_id).build());
		if (si != null) {
			space.imprint(si.builder().status(SharedItemStatus.ACCEPTED).build());
		}
	}
	
	@Override
	protected void recent_search(RecentSearch action, Space space) {
		SearchQuery search = new SearchQuery.Builder().search(action.search)
				.context_key(action.scxt_key)
				.context_value(action.scxt_val)
				.time(action.time)
				.build();
		
		// RecentSearches
		RecentSearches s = getOrDefault(spec().things().recentSearches().build(), space);
		List<SearchQuery> searches = Safe.nonNullCopy(s.searches);
		Iterator<SearchQuery> it = searches.iterator();
		while (it.hasNext()) {
			SearchQuery q = it.next();
			if (StringUtils.equals(search.search, q.search)
					&& ObjectUtils.equals(search.context_key, q.context_key)
					&& StringUtils.equals(search.context_value, q.context_value)) {
				it.remove();
				break;
			}
		}
		searches.add(0, search);
		space.imprint(s.builder().searches(searches).build());
		
		// Get
		space.imprintManyWhere(Get.THING_TYPE, Get.class, thing -> thing.recent_searches != null, get -> {
			List<SearchQuery> gsearches = get.recent_searches;
			Iterator<SearchQuery> it2 = gsearches.iterator();
			while (it2.hasNext()) {
				SearchQuery q = it2.next();
				if (StringUtils.equals(search.search, q.search)
						&& ObjectUtils.equals(search.context_key, q.context_key)
						&& StringUtils.equals(search.context_value, q.context_value)) {
					it2.remove();
					break;
				}
			}
			gsearches.add(0, search);
			return get.builder().recent_searches(gsearches).build();
		});
	}
	
	@Override
	protected void update_user_setting(UpdateUserSetting action, Space space) {
		UserSetting s = getOrDefault(new UserSetting.Builder().key(UserSettingKey.create(action.key)).build(), space);
		space.imprint(s.builder().value(action.value).build());
	}

	@Override
	protected void update_logged_in_account(UpdateLoggedInAccount action, Space space) {
		space.imprint(action.info);
	}
	
	@Override
	protected void update_offline_status(UpdateOfflineStatus action, Space space) {
		Item item = action.item != null ? space.get(action.item) : (Item) space.get(action.item_idkey);
		if (item != null) {
			Item.Builder builder = item.builder();
			if (action.view == null || action.view == PositionType.ARTICLE)
				builder.offline_text(action.status);
			if (action.view == null || action.view == PositionType.WEB)
				builder.offline_web(action.status);
			space.imprint(builder.build());
		}
	}
	
	@Override
	protected void unknown(Action action, Space space) {
		// We don't know how to do this action locally, so don't do anything.
	}
	
	@Override
	protected void fake_premium_status(FakePremiumStatus action, Space space) {
		LoginInfo login = getLoginInfo(space);
		Account.Builder account = new Account.Builder(login.account);
		if (action.declared.premium_status) account.premium_status(action.premium_status);
		if (action.declared.premium_features) account.premium_features(action.premium_features);
		if (action.declared.premium_alltime_status)
			account.premium_alltime_status(action.premium_alltime_status);
		space.imprint(account.build());
	}
	
	@Override
	protected void acctchange(Acctchange action, Space space) {
		space.addInvalid(AccountUtil.getuser(spec()));
	}
	
	@Override
	protected void approve_access(ApproveAccess action, Space space) {
		space.addInvalid(AccountUtil.getuser(spec()));
	}
	
	@Override
	protected void resendEmailConfirmation(ResendEmailConfirmation action, Space space) {
		space.addInvalid(AccountUtil.getuser(spec()));
	}
	
	@Override
	protected void reset_offline_statuses(ResetOfflineStatuses action, Space space) {
		Collection<Item> all = space.getAll(Item.THING_TYPE, Item.class);
		List<Item> updated = new ArrayList<>();
		for (Item item : all) {
			if ((item.offline_text != null && item.offline_text != OfflineStatus.NOT_OFFLINE)
					|| (item.offline_web != null && item.offline_web != OfflineStatus.NOT_OFFLINE)) {
				updated.add(item.builder()
						.offline_text(OfflineStatus.NOT_OFFLINE)
						.offline_web(OfflineStatus.NOT_OFFLINE)
						.build());
			}
		}
		space.imprint(updated);
	}
	
	@Override
	protected void resolve_tweet(ResolveTweet action, Space space) {
		Item item = space.get(action.item);
		if (item != null) {
			List<Tweet> tweets = Safe.nonNullCopy(item.tweets);
			tweets.remove(action.tweet);
			tweets.add(0, action.tweet);
			space.imprint(item.builder().tweets(tweets).build());
		}
	}
	
	@Override
	protected void setAvatar(SetAvatar action, Space space) {
		space.addInvalid(AccountUtil.getuser(spec()));
	}
	@Override @Deprecated protected void set_site_login_status(SetSiteLoginStatus action, Space space) {
		// No-op do not use!
	}

	@Override
	protected void addAlias(AddAlias action, Space space) {
		space.addInvalid(AccountUtil.getuser(spec()));
	}
	
	@Override
	protected void deleteAlias(DeleteAlias action, Space space) {
		space.addInvalid(AccountUtil.getuser(spec()));
	}
	
	@Override
	protected void registerSocialToken(RegisterSocialToken action, Space space) {
		space.addInvalid(AccountUtil.getuser(spec()));
	}
	
	@Override
	protected void purchase(Purchase action, Space space) {
		space.addInvalid(AccountUtil.getuser(spec()));
	}

	@Override
	protected void reportArticleView(ReportArticleView action, Space space) {
	}
	
	@Override
	protected void fetch_completed(FetchCompleted action, Space space) {
		SyncState s = getOrDefault(spec().things().syncState().build(), space);
		space.imprint(s.builder().fetched(true).build());
	}

	/** {@inheritDoc} */
	@Override
	protected void hide_adzerk_spoc(HideAdzerkSpoc action, Space space) {
		// Hide spocs locally immediately.
		for (AdzerkPlacementName name : AdzerkPlacementName.values()) {
			AdzerkSpocs adzerkSpocs = space.get(spec().things().adzerkSpocs().name(name).build());
			if (adzerkSpocs == null) continue;

			List<AdzerkSpoc> spocs = new ArrayList<>(adzerkSpocs.spocs);
			for (Iterator<AdzerkSpoc> iterator = spocs.iterator(); iterator.hasNext(); ) {
				if (action.spoc.equals(iterator.next().decision)) iterator.remove();
			}
			space.imprint(adzerkSpocs.builder().spocs(spocs).build());
		}
		
		// Imprint it to hide in Adzerk.
		space.imprint(
				new HiddenSpoc.Builder().decision(action.spoc)
						.time_hidden(Timestamp.now())
						.build()
		);
	}

	/** {@inheritDoc} */
	@Override protected void clear_adzerk_spocs(ClearAdzerkSpocs action, Space space) {
		for (AdzerkPlacementName name : AdzerkPlacementName.values()) {
			AdzerkSpocs spocs = space.get(spec().things().adzerkSpocs().name(name).build());
			if (spocs != null) {
				space.imprint(spocs.builder().spocs(Collections.emptyList()).build());
			}
		}
	}

	/**
	 * <h3>Effects</h3>
	 * <pre>
	 * Add a mapping from {.assignment.name} to {.assignment} in {Unleash.overridden_assignments}.
	 * </pre>
	 */
	@Override
	protected void override_unleash_assignment(OverrideUnleashAssignment action, Space space) {
		Unleash unleash = space.get(spec().things().unleash().build());
		if (unleash == null) return;
		
		Map<String, UnleashAssignment> overrides = Safe.nonNullCopy(unleash.overridden_assignments);
		overrides.put(action.assignment.name, action.assignment);
		space.imprint(unleash.builder().overridden_assignments(overrides).build());
	}

	/**
	 * <h3>Effects</h3>
	 * <pre>
	 * Remove the mapping for {.name} from {Unleash.overridden_assignments}.
	 * </pre>
	 */
	@Override
	protected void clear_unleash_assignment_override(ClearUnleashAssignmentOverride action, Space space) {
		Unleash unleash = space.get(spec().things().unleash().build());
		if (unleash == null) return;

		Map<String, UnleashAssignment> overrides = Safe.nonNullCopy(unleash.overridden_assignments);
		overrides.remove(action.name);
		space.imprint(unleash.builder().overridden_assignments(overrides).build());
	}

	@Override protected void markAsViewed(MarkAsViewed action, Space space) {
		editItem(spec(), space, null, action.url, found -> found.builder()
				.viewed(true)
				.time_updated(action.time)
				.build());
	}

	@Override protected void markAsNotViewed(MarkAsNotViewed action, Space space) {
		editItem(spec(), space, null, action.url, found -> found.builder()
				.viewed(false)
				.time_updated(action.time)
				.build());
	}

	@Override protected void rederive_items(RederiveItems action, Space space) {
		var rederived = new ArrayList<>(action.items);
		for (var item: action.items) {
			rederived.add(spec().derive().rederive(item, null, null, null));
		}
		space.imprint(rederived);
	}

	/**
	 * Helper for looking for an item and if present, making changes.
	 * @param space Where to look for it
	 * @param itemId If known, the item's item_id
	 * @param url If known, the item's url
	 * @param edit The transformation to apply and imprint back into the space if found. If nothing is found this is not invoked.
	 */
	private static void editItem(PocketSpec spec, Space space, String itemId, UrlString url, Edit<Item> edit) {
		Item inMemory = findItem(space, null, itemId, url, spec);
		if (inMemory != null) {
			Item edited = edit.edit(inMemory);
			space.imprint(edited);
		}
	}
	
	/**
	 * Helper for finding an item in the {@link Space} matching the various inputs of an action that may or may not be present.
	 * Find an item matching whatever information we might have
	 * @param item An item if known
	 * @param itemId An item_id if known
	 * @param url An given_url if known
	 * @return An item if it exists in the {@link Space}
	 */
	private static Item findItem(Space space, Item item, String itemId, UrlString url, PocketSpec spec) {
		if (item != null && item.declared.id_url) {
			return space.get(item);
		} else if (url != null) {
			return space.get(ItemUtil.create(url.url, spec));
		} else if (itemId != null && item == null) {
			return (Item) space.where(Item.THING_TYPE, "item_id", itemId);
		}
		return null;
	}
	
	interface Edit<T extends Thing> {
		T edit(T found);
	}
	
	/**
	 * Look up this thing in space and return it if found. If the space returns null, return the provided thing.
	 */
	private static <T extends Thing> T getOrDefault(T t, Space space) {
		T found = space.get(t);
		return found != null ? found : t;
	}
	
	private static LoginInfo getLoginInfo(Space space) {
		return space.get(new LoginInfo.Builder().build());
	}
	
	private static Profile getLoggedInProfile(Space space) {
		LoginInfo info = getLoginInfo(space);
		return Safe.get(() -> info.account.profile);
	}


	// Some that are not abstract because they have effects but we can ignore locally
	@Override protected void register_push(RegisterPush action, Space space) {}
	@Override protected void deregister_push(DeregisterPush action, Space space) {}
	@Override protected void register_push_v2(RegisterPushV2 action, Space space) {}
	@Override protected void deregister_push_v2(DeregisterPushV2 action, Space space) {}
}
