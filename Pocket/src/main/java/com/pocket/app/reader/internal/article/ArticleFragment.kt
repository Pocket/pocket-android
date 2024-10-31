package com.pocket.app.reader.internal.article

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.webkit.*
import android.webkit.WebView.HitTestResult
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.ideashower.readitlater.R
import com.ideashower.readitlater.databinding.FragmentArticleBinding
import com.pocket.analytics.Tracker
import com.pocket.analytics.ViewableImpressionScrollListener
import com.pocket.analytics.appevents.ArticleViewEvents
import com.pocket.app.AppMode
import com.pocket.app.auth.AuthenticationActivity
import com.pocket.app.home.slates.overflow.RecommendationOverflowBottomSheetFragment
import com.pocket.app.premium.Premium
import com.pocket.app.reader.Reader
import com.pocket.app.reader.ReaderFragment
import com.pocket.app.reader.internal.article.find.FindTextViewModel
import com.pocket.app.reader.internal.article.highlights.HighlightsBottomSheetFragment
import com.pocket.app.reader.internal.article.image.ImageViewerActivity
import com.pocket.app.reader.internal.article.recommendations.EndOfArticleRecommendationsAdapter
import com.pocket.app.reader.internal.article.recommendations.EndOfArticleRecommendationsViewModel
import com.pocket.app.reader.internal.article.recommendations.RecommendationSpacingDecorator
import com.pocket.app.reader.internal.article.textsettings.TextSettingsBottomSheetFragment
import com.pocket.app.settings.Theme
import com.pocket.app.share.ShareDialogFragment
import com.pocket.app.share.show
import com.pocket.sdk.api.generated.thing.Image
import com.pocket.sdk.preferences.AppPrefs
import com.pocket.sdk.tts.Listen
import com.pocket.sdk.util.AbsPocketFragment
import com.pocket.sdk.util.view.UpDownAnimator
import com.pocket.sdk2.api.legacy.PocketCache
import com.pocket.ui.util.DimenUtil
import com.pocket.ui.view.dialog.DialogView
import com.pocket.util.android.*
import com.pocket.util.collectWhenCreated
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


/**
 * Article view uses a local html file from the assets directory called article-mobile.html
 * That html file has an empty body with some css and javascript files attached.
 * We load the html of the article into the web view by using a javascript function.
 */
@AndroidEntryPoint
class ArticleFragment : AbsPocketFragment(), Reader.NavigationEventHandler, NoObfuscation {

    @Inject lateinit var displaySettingsManager: DisplaySettingsManager
    @Inject lateinit var theme: Theme
    @Inject lateinit var listen: Listen
    @Inject lateinit var pocketCache: PocketCache
    @Inject lateinit var premium: Premium
    @Inject lateinit var appPrefs: AppPrefs
    @Inject lateinit var appMode: AppMode
    @Inject lateinit var tracker: Tracker
    @Inject lateinit var clipboard: Clipboard

    private val navController: NavController?
        get() = (parentFragment as? NavHostFragment)?.navController

    private val viewModel: ArticleViewModel by viewModels()
    private val endOfArticlesViewModel: EndOfArticleRecommendationsViewModel by viewModels()
    private val findTextViewModel: FindTextViewModel by viewModels()

    private val args: ArticleFragmentArgs by navArgs()

    private val readerFragment: ReaderFragment?
        get() = parentFragment?.parentFragment as? ReaderFragment

    private var _binding: FragmentArticleBinding? = null
    private val binding: FragmentArticleBinding
        get() = _binding!!

    private val articleViewHtmlPath: String
        get() {
            val classKey = FormFactor.getClassKey(true)
            val formFactor = if (classKey != null) {
                "-$classKey"
            } else {
                ""
            }
            return "file:///android_asset/html/article-mobile${formFactor}.html"
        }

    // used to disable scroll change listener temporarily
    // useful when we need to programmatically scroll to a position
    private var allowScrollChange: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showsDialog = false
    }

    override fun onCreateViewImpl(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentArticleBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        binding.endOfArticleViewModel = endOfArticlesViewModel
        binding.findTextViewModel = findTextViewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("Navigation", "ArticleFragment")
        tracker.track(ArticleViewEvents.screenView())
        setupEventObserver()
        setupToolbar()
        setupWebView()
        setupEndOfArticle()
        setupScrollListener()
        viewModel.onInitialized(args.url)
        endOfArticlesViewModel.onInitialized(args.url)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPaused(binding.nestedScrollView.scrollY)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onBackPressed(): Boolean {
        if (findTextViewModel.uiState.value.visible) {
            binding.findInPageToolbar.input().setText("")
            findTextViewModel.onCloseClicked()
            hideKeyboard()
            return true
        }
        return super.onBackPressed()
    }

    override fun handleNavigationEvent(event: Reader.NavigationEvent) {
        if (event.addToBackstack) {
            when (event) {
                is Reader.NavigationEvent.GoToArticle ->
                    navController?.navigateSafely(ArticleFragmentDirections.enterArticle(event.url))
                is Reader.NavigationEvent.GoToCollection ->
                    navController?.navigateSafely(ArticleFragmentDirections.enterCollection(event.url))
                is Reader.NavigationEvent.GoToOriginalWeb ->
                    navController?.navigateSafely(ArticleFragmentDirections.enterOriginalWeb(event.url))
            }
        } else {
            when (event) {
                is Reader.NavigationEvent.GoToArticle ->
                    navController?.navigateSafely(ArticleFragmentDirections.switchToArticle(event.url))
                is Reader.NavigationEvent.GoToCollection ->
                    navController?.navigateSafely(ArticleFragmentDirections.switchToCollection(event.url))
                is Reader.NavigationEvent.GoToOriginalWeb ->
                    navController?.navigateSafely(ArticleFragmentDirections.switchToOriginalWeb(event.url))
            }
        }
    }

    private fun setupEventObserver() {
        viewModel.events.collectWhenCreated(viewLifecycleOwner) { handleEvent(it) }
        endOfArticlesViewModel.events.collectWhenCreated(viewLifecycleOwner) { handleEvent(it) }
        findTextViewModel.events.collectWhenCreated(viewLifecycleOwner) { handleEvent(it) }
    }

    private fun handleEvent(event: ArticleScreen.Event) {
        when (event) {
            is ArticleScreen.Event.ExecuteJavascript -> {
                Log.d("Javascript", "executing command: ${event.command}")
                binding.webView.executeJS(event.command)
            }
            is ArticleScreen.Event.GoBack -> readerFragment?.onBackPressed()
            is ArticleScreen.Event.GoToOriginalWebView ->
                navController?.navigateSafely(ArticleFragmentDirections.switchToOriginalWeb(args.url))
            is ArticleScreen.Event.GoToAi -> {
                navController?.navigateSafely(ArticleFragmentDirections.switchToAi(args.url))
            }
            is ArticleScreen.Event.GoToSignIn -> {
                AuthenticationActivity.startActivity(requireContext(), true)
            }
            is ArticleScreen.Event.ShowHighlightsUpsell -> {
                DialogView(context).bind()
                    .title(R.string.lb_annotations_upsell_t)
                    .message(
                        getString(
                            R.string.lb_annotations_upsell_m,
                            pocketCache.annotations_per_article_limit()
                        )
                    )
                    .buttonPrimary(R.string.lb_annotations_upsell_cta) {
                        premium.showUpgradeScreen(context, null)
                    }
                    .buttonSecondary(R.string.ac_maybe_later, null)
                    .showAsAlertDialog(null, true)
            }
            is ArticleScreen.Event.ShowHighlightBottomSheet -> showHighlightOverlay()
            is ArticleScreen.Event.ShowShare -> {
                ShareDialogFragment.show(
                    childFragmentManager,
                    event.item,
                    event.quote,
                )
            }
            is ArticleScreen.Event.ShowTextSettingsBottomSheet -> {
                TextSettingsBottomSheetFragment.newInstance().show(
                    childFragmentManager,
                    TextSettingsBottomSheetFragment::class.simpleName
                )
            }
            is ArticleScreen.Event.ShowTextFinder -> {
                findTextViewModel.onShow()
                binding.findInPageToolbar.input().requestFocus()
                Handler(Looper.getMainLooper()).postDelayed({ showKeyboard(binding.findInPageToolbar.input()) }, 200)
            }
            is ArticleScreen.Event.OpenNewUrl -> readerFragment?.openUrl(
                url = event.url,
                queueManager = event.queueManager
            )
            is ArticleScreen.Event.OpenOverflowBottomSheet ->
                RecommendationOverflowBottomSheetFragment.newInstance(
                    url = event.url,
                    title = event.title,
                    corpusRecommendationId = event.corpusRecommendationId
                )
                    .show(
                        childFragmentManager,
                        RecommendationOverflowBottomSheetFragment::class.simpleName
                    )
            is ArticleScreen.Event.ScrollToSavedPosition -> {
                binding.nestedScrollView.scrollTo(0, event.position)
                allowScrollChange = true
            }
            is ArticleScreen.Event.OpenImage -> {
                ImageViewerActivity.open(
                    activity,
                    event.articleImages.map { articleImage ->
                        Image.Builder()
                            .image_id(articleImage.imageId)
                            .src(articleImage.originalUrl)
                            .caption(articleImage.caption)
                            .credit(articleImage.credit)
                            .build()
                    },
                    event.startingId
                )
            }
            is ArticleScreen.Event.ShowSavedToast -> {
                Toast.makeText(context, getString(R.string.ts_add_added), Toast.LENGTH_SHORT).show()
            }
            is ArticleScreen.Event.ShowArchivedToast -> {
                Toast.makeText(context, getString(R.string.ts_item_archived), Toast.LENGTH_SHORT).show()
            }
            is ArticleScreen.Event.ShowReAddedToast -> {
                Toast.makeText(context, getString(R.string.ts_item_readded), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setupToolbar(
            toolbarEvents = viewModel.toolbar.toolbarEvents,
            lifecycleOwner = viewLifecycleOwner,
            readerFragment = readerFragment,
            listen = listen,
            url = args.url,
            toolbarInteractions = viewModel.toolbar,
            toolbarOverflowInteractions = viewModel.toolbar,
            toolbarUiStateHolder = viewModel.toolbar,
        )
        binding.findInPageToolbar.apply {
            forward().setOnClickListener { findTextViewModel.onNextClicked() }
            back().setOnClickListener { findTextViewModel.onPreviousClicked() }
            input().addTextChangedListener {
                findTextViewModel.onTextChanged(it.toString())
            }
            cancel().setOnClickListener {
                input().setText("")
                findTextViewModel.onCloseClicked()
                hideKeyboard()
            }
        }
    }

    private fun setupEndOfArticle() {
        val impressionScrollListener = ViewableImpressionScrollListener(viewLifecycleOwner)
        binding.nestedScrollView.setOnScrollChangeListener(impressionScrollListener)
        binding.endOfArticleList.adapter = EndOfArticleRecommendationsAdapter(
            viewLifecycleOwner,
            endOfArticlesViewModel,
            impressionScrollListener,
        )
        binding.endOfArticleList.addItemDecoration(RecommendationSpacingDecorator())
        if (FormFactor.isTablet(requireContext())) {
            binding.endOfArticleList.layoutManager = GridLayoutManager(context, 2)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        if (appMode.isForInternalCompanyOnly) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
        binding.webView.settings.apply {
            allowFileAccess = true
            javaScriptEnabled = true
        }
        binding.webView.addJavascriptInterface(
            ArticleJsInterface(),
            ARTICLE_JS_INTERFACE_NAME
        )
        binding.webView.apply {
            webViewClient = ArticleViewClient()
            loadUrl(articleViewHtmlPath)
            onHighlightActionModeClicked = {
                // called when you highlight text (by long pressing text)
                // and click the highlight button from the popup
                viewModel.onActionModeHighlightClicked()
            }
            onShareActionModeClicked = { selectedText ->
                viewModel.onActionModeShareClicked(selectedText)
            }
            setOnLongClickListener {
                val result = hitTestResult
                if (result.type == HitTestResult.SRC_ANCHOR_TYPE) {
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    AlertDialog.Builder(context)
                        .setTitle(result.extra)
                        .setItems(
                            arrayOf(
                                getString(R.string.mu_read_later),
                                getString(R.string.mu_copy_link),
                            )
                        ) { _, item ->
                            when (item) {
                                0 -> result.extra?.let { url -> viewModel.onSaveClicked(url) }
                                1 -> result.extra?.let { url ->
                                    clipboard.setText(url, getString(R.string.nm_link))
                                }
                            }
                        }.show()
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun setupScrollListener() {
        if (!appPrefs.READER_AUTO_FULLSCREEN.get()) {
            return
        }
        val toolbarAnimator = UpDownAnimator(binding.toolbar)
        binding.nestedScrollView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            if (!allowScrollChange) return@setOnScrollChangeListener

            if (scrollY > oldScrollY) {
                // scrolling down
                toolbarAnimator.hide()
                if (!binding.nestedScrollView.canScrollVertically(0)) {
                    // scrolled to bottom
                    readerFragment?.previousNextAnimator?.show()
                } else {
                    readerFragment?.previousNextAnimator?.hide()
                }
            } else {
                // scrolling up
                toolbarAnimator.show()
                readerFragment?.previousNextAnimator?.show()
            }
        }
    }

    private fun showHighlightOverlay() {
        HighlightsBottomSheetFragment.newInstance(
            url = args.url,
            onHighlightClicked = { highlightId: String ->
                viewModel.onHighlightClickedFromOverlay(highlightId)
            },
            onHighlightDeleted = {
                viewModel.onHighlightDeleted()
            },
        ).show(childFragmentManager, HighlightsBottomSheetFragment::class.simpleName)
    }

    private inner class ArticleJsInterface {
        /**
         * Called when the article html has been loaded.  We can load in the images and videos now.
         */
        @JavascriptInterface
        fun onReady() {
            viewModel.onArticleHtmlLoadedIntoWebView(displaySettingsManager.getImageWidth(activity))
        }

        @JavascriptInterface
        fun onError() {
            viewModel.onJavascriptError()
        }

        @JavascriptInterface
        fun getHorizontalMargin(): Int = displaySettingsManager.getHorizontalMargin(binding.webView)

        @JavascriptInterface
        fun getMaxMediaHeight(): Int {
            return DimenUtil.pxToDpInt(requireContext(), binding.nestedScrollView.height.toFloat())
        }

        @JavascriptInterface
        fun onRequestedHighlightPatch(patch: String, text: String) {
            viewModel.onHighlightPatchRequested(patch, text)
        }

        @JavascriptInterface
        fun onHighlightClicked(json: String) {
            showHighlightOverlay()
        }

        @JavascriptInterface
        fun scrollToPosition(
            position: Float,
        ) {
            allowScrollChange = false
            binding.nestedScrollView.scrollTo(
                0,
                DimenUtil.dpToPxInt(
                    requireContext(),
                    position
                ) - resources.getDimension(com.pocket.ui.R.dimen.pkt_app_bar_height).toInt()
            )
            allowScrollChange = true
        }

        @JavascriptInterface
        fun onTextSearch(
            count: Int,
        ) {
            findTextViewModel.onTextHighlighted(count)
        }

        @JavascriptInterface
        fun log(log: String) {
            Log.d("Javascript-log", log)
        }

        /**
         * this is called when a user clicks a link to switch to web view (type==2)
         */
        @JavascriptInterface
        fun setViewType(type: Int) {
            if (type == 2) {
                readerFragment?.openUrl(
                    url = args.url,
                    queueManager = null,
                    forceOpenInWebView = true
                )
            }
        }
    }

    private inner class ArticleViewClient : WebViewClient() {
        /**
         * This gets called once the local html file has loaded into the web view
         * From here, we call the setup javascript functions and let the view model
         * know we are ready to load the article html
         */
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            if (_binding == null) return

            viewModel.onInitialPageLoaded(
                theme.get(binding.webView),
                resources.displayMetrics.density,
            )
        }

        /**
         * If the user opens any links, handle it here
         */
        @Suppress("ReturnCount")
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?,
        ): Boolean {
            if (request?.url?.host == "getpocket.com") {
                // ignore getpocket.com links
                return true
            }
            val url = request?.url?.toString()
            if ((url?.startsWith("isril:") == true) || (url?.startsWith("ISRIL:") == true)) {
                viewModel.onInternalLinkClicked(url)
                return true
            }
            if (url == "http://ideashower.com/support/read-it-later/report-pages-not-saving-well-offline-here/") {
                viewModel.toolbar.onReportArticleClicked()
                return true
            }
            if (url != articleViewHtmlPath) {
                url?.let {
                    viewModel.onArticleLinkOpened(it)
                    readerFragment?.openUrl(it)
                }
                return true
            }
            return super.shouldOverrideUrlLoading(view, request)
        }
    }

    companion object {
        const val ARTICLE_JS_INTERFACE_NAME = "PocketAndroidArticleInterface"
    }
}