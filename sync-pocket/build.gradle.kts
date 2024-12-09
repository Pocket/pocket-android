import tasks.SYNC_GEN_TASK_NAME
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import tasks.getSyncPocketGenTaskOutputDir
import tasks.registerSyncPocketGenTask

plugins {
    kotlinJvm()
}

registerSyncPocketGenTask()

kotlin {
    tasks.withType<KotlinCompile> {
        dependsOn(SYNC_GEN_TASK_NAME)
    }
}

sourceSets {
    main {
        java.srcDir(getSyncPocketGenTaskOutputDir())
    }
}

dependencies {
    api(projects.sync)
    
    api(libs.okhttp)
    implementation(libs.okhttp.urlconnection)
    implementation(libs.okio)

    testImplementation(Deps.Jetbrains.Kotlin.test)
    testImplementation(Deps.Jetbrains.Kotlin.jUnit)
    testImplementation(Deps.Mockito.Kotlin.mockitoKotlin)
    testImplementation(Deps.Commons.Codec.commonsCodec)
    testImplementation(libs.okhttp.mockwebserver)
}
