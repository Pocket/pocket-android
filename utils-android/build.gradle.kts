plugins {
    pocketAndroidLib()
}
android {
    namespace = "com.pocket.utils.android"
}
dependencies {
    api(projects.utils)
    api(platform(libs.kotlinx.coroutines.bom))
    api(libs.kotlinx.coroutines.android)
    api(Deps.Google.Material.material)
    api(libs.androidx.core)
}
