#!kotlinc -script
import java.io.File
import kotlin.system.exitProcess

fun bash(command: String): Process {
    return ProcessBuilder("/usr/bin/env", "bash", "-c", command).start()
}

val versionCatalog = File("gradle/libs.versions.toml")
if (bash("git diff --cached --name-only | grep -q \"^${versionCatalog.path}\$\"").waitFor() != 0) {
    // Version catalog wasn't modified. Nothing to do.
    exitProcess(0)
}

val sorted = versionCatalog.readText()
    // Split into sections on the empty line.
    .split("\n\n")
    .map {
        // Sort each section.
        it.split("\n")
            .sorted()
            .joinToString("\n")
    }
    // Join sections back into a single string.
    .joinToString(separator = "\n", postfix = "\n")

// Overwrite the version catalog with the sorted version.
versionCatalog.writeText(sorted)

bash("git add -- ${versionCatalog.path}").waitFor()
exitProcess(0)
