plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

repositories {
    mavenCentral()
}

kotlin {
    jvm {
        setDefaultConfigs()
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.okio)
                implementation("com.apollographql.apollo3:apollo-ast:3.8.6")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(libs.okio.fakefilesystem)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
            }
        }
        all {
            languageSettings {
                optIn("com.apollographql.apollo3.annotations.ApolloExperimental")
            }
        }
    }
}

tasks.register<Test>("test") {
    description = "Runs the tests for all targets and creates an aggregated report"
    dependsOn("allTests")
    testClassesDirs = project.objects.fileCollection() // Empty list of files. Leaving this null/unset results in an exception.
}
