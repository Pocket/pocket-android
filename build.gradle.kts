plugins {
    versions()
}

allprojects {
    repositories {
        exclusiveContent {
            forRepository { google() }
            filter {
                includeGroupByRegex("^androidx\\..*")
                includeGroupByRegex("^com\\.android\\..*")
                includeGroupByRegex("^com\\.google\\.(android\\.|firebase|testing\\.platform|ai).*")
            }
        }
        mavenCentral()
    }
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}
