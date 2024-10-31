package com.pocket.app.reader.internal.article

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.fragment.compose.content
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.ai.edge.aicore.DownloadCallback
import com.google.ai.edge.aicore.DownloadConfig
import com.google.ai.edge.aicore.GenerativeAIException
import com.google.ai.edge.aicore.GenerativeModel
import com.google.ai.edge.aicore.generationConfig
import com.pocket.app.reader.internal.originalweb.OriginalWebFragmentArgs
import com.pocket.repository.ArticleRepository
import com.pocket.sdk.util.AbsPocketFragment
import com.pocket.ui.view.AppBar
import com.pocket.ui.view.button.PocketIconButton
import com.pocket.ui.view.button.UpIcon
import com.pocket.ui.view.themed.PocketTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import javax.inject.Inject

@AndroidEntryPoint
class AiFragment : AbsPocketFragment() {
    @Inject lateinit var articleRepository: ArticleRepository

    private val args: OriginalWebFragmentArgs by navArgs()
    private var summary by mutableStateOf("")
    private var isGenerating by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            try {
                val model = GenerativeModel(
                    generationConfig = generationConfig {
                        context = requireContext().applicationContext
                    },
                    downloadConfig = DownloadConfig(SystemOutErrDownloadCallback()),
                )
                val html = articleRepository.getArticleHtml(args.url)
                val text = Jsoup.parse(html).getElementById("RIL_body")?.text().orEmpty()
                val prompt = com.google.ai.edge.aicore.content {
                    text("Summarize:")
                    text(text)
                }
                summary = "Input length: ${text.length} characters, ~${text.length / 4} tokens\n\n"
                val contentResponses = model.generateContentStream(prompt)
                isGenerating = true
                contentResponses.collect {
                    summary += it.text
                }
            } catch (t: Throwable) {
                summary += "⚠️\n${t.message}"
            }
            isGenerating = false
        }
    }

    override fun onCreateViewImpl(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        PocketTheme {
            Column {
                AppBar(
                    Modifier.fillMaxWidth(),
                    {
                        PocketIconButton(onClick = { findNavController().navigateUp() }) {
                            UpIcon()
                        }
                    },
                    { Text("✨AI Danger Zone ⚠️") }
                )
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        summary + if (isGenerating) "✨" else "",
                        Modifier.padding(dimensionResource(com.pocket.ui.R.dimen.pkt_space_md))
                    )
                    Spacer(Modifier.height(dimensionResource(com.pocket.ui.R.dimen.min_touch_target)))
                }
            }
        }
    }

    override fun onBackPressed(): Boolean {
        findNavController().navigateUp()
        return true
    }
}

private class SystemOutErrDownloadCallback : DownloadCallback {
    override fun onDownloadDidNotStart(e: GenerativeAIException) {
        println("AiFragment.onDownloadDidNotStart")
        e.printStackTrace()
    }
    override fun onDownloadPending() {
        println("AiFragment.onDownloadPending")
    }
    override fun onDownloadStarted(bytesToDownload: Long) {
        println("AiFragment.onDownloadStarted bytesToDownload = [${bytesToDownload}]")
    }
    override fun onDownloadProgress(totalBytesDownloaded: Long) {
        println("AiFragment.onDownloadProgress totalBytesDownloaded = [${totalBytesDownloaded}]")
    }
    override fun onDownloadCompleted() {
        println("AiFragment.onDownloadCompleted")
    }
    override fun onDownloadFailed(failureStatus: String, e: GenerativeAIException) {
        println("AiFragment.onDownloadFailed failureStatus = [${failureStatus}]")
        e.printStackTrace()
    }
}
