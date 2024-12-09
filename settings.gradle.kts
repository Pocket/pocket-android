plugins {
    id("org.danilopianini.gradle-pre-commit-git-hooks") version "2.0.16"
}

gitHooks {
    preCommit {
        from(file("scripts/pre-commit.sh"))
    }
    createHooks(overwriteExisting = true)
}

include(":Pocket")
include(":pocket-ui")
include(":project-tools")
include(":utils")
include(":utils-android")
include(":sync")
include(":sync-gen")
include(":sync-android")
include(":sync-pocket")
include(":sync-pocket-android")
include(":analytics")
include(":sync-parser")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
