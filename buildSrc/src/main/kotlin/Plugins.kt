/**
 * Define plugins here for easy use in other modules
 */

import org.gradle.kotlin.dsl.kotlin
import org.gradle.plugin.use.PluginDependenciesSpec
import org.gradle.plugin.use.PluginDependencySpec

fun PluginDependenciesSpec.pocketAndroidLib(): PluginDependencySpec =
    id("com.pocket.android.library")

fun PluginDependenciesSpec.pocketAndroidApp(): PluginDependencySpec =
    id("com.pocket.android.application")

fun PluginDependenciesSpec.kotlinJvm(): PluginDependencySpec =
    id("com.pocket.kotlin.jvm")

fun PluginDependenciesSpec.kotlinKapt(): PluginDependencySpec =
    id("org.jetbrains.kotlin.kapt")

fun PluginDependenciesSpec.versions(): PluginDependencySpec =
    id("com.pocket.versions")

fun PluginDependenciesSpec.hilt(): PluginDependencySpec =
    id("dagger.hilt.android.plugin")

fun PluginDependenciesSpec.safeArgsKotlin(): PluginDependencySpec =
    id("androidx.navigation.safeargs.kotlin")

fun PluginDependenciesSpec.kotlinSerialization(): PluginDependencySpec =
    kotlin("plugin.serialization")

fun PluginDependenciesSpec.kotlinCompose(): PluginDependencySpec =
    id("org.jetbrains.kotlin.plugin.compose")

fun PluginDependenciesSpec.licensee(): PluginDependencySpec =
    id("app.cash.licensee")

fun PluginDependenciesSpec.aboutLibraries(): PluginDependencySpec =
    id("com.mikepenz.aboutlibraries.plugin")

fun PluginDependenciesSpec.sentry(): PluginDependencySpec =
    id("io.sentry.android.gradle")