plugins {
    pocketAndroidLib()
}
android {
    namespace = "com.pocket.sync.android"
}
dependencies {
    api(projects.sync)
    api(projects.utilsAndroid)

    api(Deps.Google.Tink.tink)
    
    implementation(Deps.Commons.IO.commonsIo)
}
