import tasks.registerCopyMergedManifestTask
import utils.*
import utils.pocket.*

plugins {
    pocketAndroidApp()
    kotlinKapt()
    kotlinCompose()
    hilt()
    safeArgsKotlin()
    kotlinSerialization()
    sentry()
    licensee()
}

val versionMajor = 8 // Max value of 200
val versionMinor = 26 // Max of two digits
val versionPatch = 15 // Max of two digits
val versionBuild = 0 // Max of three digits

// See usage and more details below, but this produces version numbers like 6.2.4.1 and codes like 60204001
// Also see Versioning section in README_WORKFLOW.md.

// Sensitive strings that should only be put into specific builds:
val serverDevSuffix = ".readitlater.com"

android {
    signingConfigs {
        register(SigningConfigs.TEAM) {
            storeFile = file("alpha.keystore")
            storePassword = getSecret("SIGNING_CONFIG_TEAM_STORE_PASSWORD")
            keyAlias = "team"
            keyPassword = getSecret("SIGNING_CONFIG_TEAM_KEY_PASSWORD")
        }
    }

    namespace = "com.ideashower.readitlater"
    defaultConfig {
        applicationId = "com.ideashower.readitlater.pro"

        buildStringField("GIT_SHA", getGitSha())
        buildStringField("API_KEY_PHONE", getSecret("API_KEY_PHONE"))
        buildStringField("API_KEY_TABLET", getSecret("API_KEY_TABLET"))
        buildStringField("API_KEY_AMAZON_PHONE", getSecret("API_KEY_AMAZON_PHONE"))
        buildStringField("API_KEY_AMAZON_TABLET", getSecret("API_KEY_AMAZON_TABLET"))
        buildStringField("API_DEV_SUFFIX", "")
        buildStringField("UA_PM", "Free")
        buildStringField("AC_I", getSecret("APP_CENTER_PROD"))
        buildStringField("ADJUST_APP_TOKEN", getSecret("ADJUST_APP_TOKEN"))
        buildStringField("ADJUST_SIGN_UP_EVENT_TOKEN", getSecret("ADJUST_SIGN_UP_EVENT_TOKEN"))
        buildStringField("SENTRY_DSN", getSecret("SENTRY_DSN"))

        buildBooleanField("I_B", false) // I_B means isTeamBeta?


        resString("google_api_key", getSecret("GOOGLE_API_KEY"))
        resString("google_crash_reporting_api_key", getSecret("GOOGLE_API_KEY"))

        // MMMmmppbbb
        versionCode = versionMajor * 10000000 + versionMinor * 100000 + versionPatch * 1000 + versionBuild
        // MMM.mm.pp.bbb
        versionName = "$versionMajor.$versionMinor.$versionPatch.$versionBuild"

        vectorDrawables.useSupportLibrary = true // https://medium.com/@chrisbanes/appcompat-v23-2-age-of-the-vectors-91cbafa87c88#.m9i38hx27
        resourceConfigurations.addAll(
            arrayOf(
                "de",
                "es",
                "es-rES",
                "fr",
                "fr-rCA",
                "it",
                "ja",
                "ko",
                "nl",
                "pl",
                "pt",
                "pt-rBR",
                "ru",
                "zh",
                "zh-rCN",
                "zh-rTW"
            )
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions.add(FlavorDimensions.TARGET)
    productFlavors {
        // Development
        register(Flavors.DEVELOP) {
            isDefault = true
            dimension = FlavorDimensions.TARGET
            applicationIdSuffix = ".dev"

            buildStringField("MARKET_KEY", "play")
            buildStringField("API_DEV_SUFFIX", serverDevSuffix)

            buildBooleanField("I_B", true) // I_B means isTeamBeta?
        }

        register(Flavors.TEAM_REVIEW) {
            dimension = FlavorDimensions.TARGET
            applicationId = "com.pocket.team.review"

            buildStringField("MARKET_KEY", "team-review")
            buildStringField("API_DEV_SUFFIX", serverDevSuffix)
            // Note that the backend uses the prefix of "Free Team" to distinguish that it is
            // an Alpha/Team build
            buildStringField("UA_PM", "Free Team Review")

            buildBooleanField("I_B", true) // I_B means isTeamBeta?

            resString("nm_icon_alpha", "Pocket Review")
        }

        // A team build variant that uses our default package name so you can test premium
        // purchasing through google play. (Requires whitelisting the google account you are
        // purchasing with on Google Play"s console)
        register(Flavors.PREMIUM_REVIEW) {
            dimension = FlavorDimensions.TARGET

            buildStringField("MARKET_KEY", "team-review")
            buildStringField("API_DEV_SUFFIX", serverDevSuffix)
            // Note that the backend uses the prefix of "Free Team" to distinguish that it is
            // an Alpha/Team build
            buildStringField("UA_PM", "Free Team Review")

            buildBooleanField("I_B", true) // I_B means isTeamBeta?

            resString("nm_icon_alpha", "Pocket Review")
        }

        // Alpha (FKA Team Beta A)
        register(Flavors.TEAM_A) {
            dimension = FlavorDimensions.TARGET
            applicationId = "com.pocket.team.a"

            buildStringField("MARKET_KEY", "team-a")
            buildStringField("API_DEV_SUFFIX", serverDevSuffix)
            // Note that the backend uses the prefix of "Free Team" to distinguish that it is
            // an Alpha/Team build
            buildStringField("UA_PM", "Free Team Alpha A")
            buildStringField("AC_I", getSecret("APP_CENTER_TEAM_A"))

            buildBooleanField("I_B", true) // I_B means isTeamBeta?

            resString("nm_icon_alpha", "Pocket ⍺")
        }

        // Production Variant — Google Play
        register(Flavors.PLAY) {
            dimension = FlavorDimensions.TARGET
            buildStringField("MARKET_KEY", "play")
        }
    }

    buildTypes {
        getByName(BuildTypes.DEBUG) {
            isMinifyEnabled = false
            isDebuggable = true
            signingConfig = signingConfigs.getByName(SigningConfigs.TEAM)
            matchingFallbacks.add("release")
        }

        register(BuildTypes.TEAM_RELEASE) {
            isMinifyEnabled = true
            isDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.txt")
            signingConfig = signingConfigs.getByName(SigningConfigs.TEAM)
            matchingFallbacks.add("release")
        }

        register(BuildTypes.UNSIGNED_RELEASE) {
            isMinifyEnabled = true
            isDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.txt")
            signingConfig = null
            matchingFallbacks.add("release")
        }
    }

    setupVariantFilters()

    sourceSets {
        getByName(Flavors.TEAM_A) {
            manifest.srcFile("src/team/AndroidManifest.xml")
            java.srcDir("src/team/java")
            res.srcDir("src/team/res")
        }

        getByName(Flavors.TEAM_REVIEW) {
            manifest.srcFile("src/team/AndroidManifest.xml")
            java.srcDir("src/team/java")
            res.srcDir("src/team/res")
        }

        getByName(Flavors.PREMIUM_REVIEW) {
            manifest.srcFile("src/team/AndroidManifest.xml")
            java.srcDir("src/team/java")
            res.srcDir("src/team/res")
        }
    }

    packaging {
        resources { 
            merges.add("META-INF/LICENSE.txt")
            merges.add("META-INF/LICENSE.txt")
            merges.add("META-INF/LICENSE")
            merges.add("META-INF/NOTICE.txt")
            merges.add("META-INF/NOTICE")
            merges.add("META-INF/ASL2.0")
            excludes.add("build-data.properties") // Unless we have this, the tink library causes build issues
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }
    dataBinding {
        enable = true
    }
}

sentry {
    autoInstallation {
        enabled.set(false)
    }
    tracingInstrumentation {
        enabled.set(false)
    }
    includeDependenciesReport.set(false)
    ignoredBuildTypes.set(setOf(BuildTypes.DEBUG))
}

licensee {
    allow("Apache-2.0")
    allow("MIT")
    allowUrl("https://jsoup.org/license") { because("self-hosted MIT") }
    allow("BSD-2-Clause")
    allowUrl("http://opensource.org/licenses/BSD-2-Clause")
    allowUrl("https://github.com/braze-inc/braze-android-sdk/blob/master/LICENSE") { because("self-hosted BSD") }
    allowUrl("https://github.com/facebook/shimmer-android/blob/master/LICENSE") { because("self-hosted BSD") }
    allowUrl("https://raw.githubusercontent.com/ThreeTen/threetenbp/master/LICENSE.txt") { because("self-hosted BSD") }
    allow("MPL-1.1")
    allow("CC0-1.0")
    allowUrl("https://developer.android.com/studio/terms.html") { because("Android SDK") }
    allowUrl("https://developer.android.com/guide/playcore/license") { because("Play Core SDK ToS") }
}

dependencies {
    implementation(project(Deps.Pocket.syncPocketAndroid))
    implementation(project(Deps.Pocket.analytics))
    implementation(project(Deps.Pocket.pocketUi))

    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.accompanist.drawablepainter)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.fragment.compose)
    implementation(libs.androidx.navigation.fragment)

    implementation(libs.androidx.browser)
    implementation(libs.androidx.media)
    implementation(libs.androidx.work)

    implementation(Deps.AndroidX.Lifecycle.viewmodel)
    implementation(Deps.AndroidX.Lifecycle.viewmodelKtx)
    implementation(Deps.AndroidX.Lifecycle.viewmodelCompose)

    implementation(libs.kotlinx.serialization.json)

    implementation(Deps.Google.GMS.Plus.plus)
    implementation(libs.google.play.billing)
    implementation(Deps.Android.InstallReferrer.installReferrer)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    implementation(libs.dagger.hilt)
    kapt(libs.dagger.hilt.compiler)

    implementation(libs.okhttp)
    implementation(libs.okhttp.logginginterceptor)

    implementation(Deps.Nikartm.imageSupport)

    implementation(libs.markwon)
    implementation(Deps.Jooq.joor)
    
    implementation(Deps.RxJava.RxAndroid.rxAndroid)
    implementation(Deps.Commons.IO.commonsIo)
    implementation(Deps.Apache.Mime4j.core)
    implementation(Deps.JSoup.jsoup)
    implementation(Deps.Google.JUniversalCharDet.juniversalchardet)

    implementation(Deps.JakeWharton.ThreeTenAbp.threeTen)

    implementation(libs.adjust)
    implementation(libs.braze)

    debugImplementation(libs.leakcanary)

    add("${BuildTypes.DEBUG}Implementation", libs.appcenter.distribute)
    add("${BuildTypes.TEAM_RELEASE}Implementation", libs.appcenter.distribute)
    // Use a no-op implementation for builds we upload to Google Play.
    // See: https://learn.microsoft.com/en-us/appcenter/sdk/distribute/android#prepare-your-google-play-build
    add("${BuildTypes.UNSIGNED_RELEASE}Implementation", libs.appcenter.distribute.play)

    testImplementation(Deps.Mockito.core)
    testImplementation(Deps.AssertJ.core)
    testImplementation(Deps.Jetbrains.Kotlin.jUnit)
    testImplementation(Deps.MockK.mockk)
    testImplementation(platform(libs.kotlinx.coroutines.bom))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(Deps.Jetbrains.Kotlin.test)
    testImplementation(libs.turbine)

    androidTestImplementation(Deps.AndroidX.Test.rules)
    androidTestImplementation(Deps.Jetbrains.Kotlin.jUnit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(Deps.AndroidX.Test.Espresso.idlingResource)

    implementation(Deps.Google.Play.core)

    implementation(platform(libs.sentry.bom))
    implementation(libs.sentry)
    implementation(libs.sentry.okhttp)
}

registerCopyMergedManifestTask()
