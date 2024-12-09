plugins {
    kotlinJvm()
    kotlinSerialization()
}

repositories {
    exclusiveContent {
        forRepository { atlassian() }
        filter {
            includeGroup("com.atlassian.prosemirror")
        }
    }
}
dependencies {
    implementation(libs.prosemirror.model)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(Deps.Jetbrains.Kotlin.jUnit)
    testImplementation(libs.prosemirror.state)
    testImplementation(libs.prosemirror.transform)
}

fun RepositoryHandler.atlassian() = maven {
    name = "Atlassian"
    setUrl("https://maven.artifacts.atlassian.com/")
}
