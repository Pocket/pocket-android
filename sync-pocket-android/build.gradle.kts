import tasks.registerSyncTestGenTask

plugins {
    pocketAndroidLib()
}
android {
    namespace = "com.pocket.sdk"
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

android.unitTestVariants.all {
    project.registerSyncTestGenTask(this)
}

dependencies {
    api(projects.syncPocket)
    api(projects.syncAndroid)

    testImplementation(Deps.JUnit.junit)
    testImplementation(Deps.Mockito.core)
    testImplementation(libs.robolectric)
    testImplementation(Deps.Commons.IO.commonsIo)
    testImplementation(Deps.Jetbrains.Kotlin.jUnit)
}
