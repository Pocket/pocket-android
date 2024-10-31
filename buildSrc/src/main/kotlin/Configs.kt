import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

object AndroidConfigs {
    const val CompileSdkVersion = 34
    const val MinSdkVersion = 31
    const val TargetSdkVersion = 34
}

object KotlinConfigs {
    val jvmTarget : JvmTarget = JvmTarget.JVM_11
    const val FreeCompilerArgs = "-Xjvm-default=all"
}

object JavaConfigs {
    val javaVersion = JavaVersion.VERSION_11
}

/**
 * Set android configurations for a [BaseExtension].
 * [BaseExtension] is a parent class of [LibraryExtension] and [BaseAppModuleExtension] so this
 * method can be used within an 'android {}' block from an android app and an android library.
 */
fun BaseExtension.setDefaultConfigs() {
    compileSdkVersion(AndroidConfigs.CompileSdkVersion)
    defaultConfig {
        minSdk = AndroidConfigs.MinSdkVersion
        targetSdk = AndroidConfigs.TargetSdkVersion
    }
    compileOptions {
        sourceCompatibility = JavaConfigs.javaVersion
        targetCompatibility = JavaConfigs.javaVersion
    }
}

fun KotlinProjectExtension.setDefaultConfigs() {
    compilerOptions {
        jvmTarget.set(KotlinConfigs.jvmTarget)
        freeCompilerArgs.add(KotlinConfigs.FreeCompilerArgs)
    }
}

private fun KotlinProjectExtension.compilerOptions(configure: Action<KotlinJvmCompilerOptions>) {
    when (this) {
        is KotlinJvmProjectExtension -> compilerOptions(configure)
        is KotlinAndroidProjectExtension -> compilerOptions(configure)
        else -> throw RuntimeException("Default configuration doesn't handle this Kotlin module type.")
    }
}
