package com.pocket.app.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.fragment.compose.content
import androidx.navigation.fragment.findNavController
import com.ideashower.readitlater.R
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.pocket.sdk.util.AbsPocketFragment
import com.pocket.ui.view.AppBar
import com.pocket.ui.view.button.PocketIconButton
import com.pocket.ui.view.button.UpIcon
import com.pocket.ui.view.themed.PocketTheme

class OpenSourceLicensesFragment : AbsPocketFragment() {
    override fun onCreateViewImpl(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        PocketTheme {
            Column {
                AppBar(
                    navigationIcon = {
                        PocketIconButton(onClick = { findNavController().navigateUp() }) {
                            UpIcon()
                        }
                    },
                    title = { Text(stringResource(R.string.setting_oss)) },
                )
                LibrariesContainer(
                    Modifier.fillMaxSize(),
                    showVersion = false,
                )
            }
        }
    }
}
