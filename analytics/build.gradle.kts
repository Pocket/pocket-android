plugins {
    pocketAndroidLib()
}
android {
    namespace = "com.pocket.analytics"
}
dependencies {
    implementation(projects.utilsAndroid)
    implementation(libs.snowplow.android.tracker)
    implementation(libs.androidx.core)
    implementation(libs.okhttp)
}
