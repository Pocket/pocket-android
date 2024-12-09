import tasks.*

plugins {
    kotlinJvm()
    licensee()
}
licensee {
    allow("Apache-2.0")
    allowUrl("https://raw.githubusercontent.com/apollographql/apollo-kotlin/main/LICENSE") { because("self-hosted MIT") }
    allowUrl("http://www.antlr.org/license.html") { because("self-hosted BSD-3-Clause") }
}

dependencies {
    implementation(projects.syncParser)
    implementation(Deps.Apache.Commons.commonsLang)
    implementation(Deps.Square.JavaPoet.javaPoet)
    implementation(Deps.Jackson.core)
    implementation(Deps.Jackson.databind)
    implementation(libs.okio)
}

/**
 * Builds jar responsible for generating production code from schema.
 */
registerJarTask(
    taskName = TaskNames.POCKET_GEN_JAR,
    mainClass = "com.pocket.sync.print.java.pocket.AndroidClassGenerator",
    jarName = "sync-pocket-class-generator",
    configuration = configurations
)
/**
 * Builds jar responsible for generating test code from schema.
 */
registerJarTask(
    taskName = TaskNames.SYNC_TESTS_GEN_JAR,
    mainClass = "com.pocket.sync.print.java.tests.SyncTestsGenerator",
    jarName = "sync-tests-class-generator",
    configuration = configurations
)
registerPocketGenJarPublish()
registerSyncTestsGenJarPublish()
registerGenerateExamples(sourceSets.main.get().runtimeClasspath.asPath)
