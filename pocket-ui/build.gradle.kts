plugins {
    pocketAndroidLib()
    kotlinKapt()
    kotlinCompose()
}

android {
    namespace = "com.pocket.ui"
    android {
        testOptions.unitTests.isIncludeAndroidResources = true
        defaultConfig.vectorDrawables.useSupportLibrary = true
    }
    buildFeatures {
        viewBinding = true
        compose = true
    }
    kotlinOptions {
        freeCompilerArgs += "-opt-in=androidx.compose.ui.text.ExperimentalTextApi"
    }
    dataBinding {
        enable = true
    }
}
composeCompiler {
    includeSourceInformation = true
}
dependencies {
    api(projects.analytics)
    implementation(projects.utilsAndroid)

    api(Deps.AirBnb.Lottie.lottie)
    api(Deps.AndroidX.ConstraintLayout.constraintLayout)
    api(Deps.AndroidX.ViewPager2.viewPager2)
    api(Deps.Facebook.Shimmer.shimmer)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.runtime.rxjava2)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.ui.tooling.preview)

    implementation(Deps.AndroidX.SwipeRefreshLayout.swipeRefresh)

    implementation(Deps.Google.FlexBox.flexbox)
    implementation(Deps.Nikartm.imageSupport)
}
