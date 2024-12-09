/**
 * Allows referencing dependencies in gradle files in a type-safe and discoverable way.
 *
 * Deprecated: We're gradually migrating to version catalog
 */
@Suppress("MemberNameEqualsClassName")
object Deps {
    object Jetbrains {
        object Kotlin {
            const val jUnit = "org.jetbrains.kotlin:kotlin-test-junit"
            const val test = "org.jetbrains.kotlin:kotlin-test-common"
        }
    }
    object AndroidX {
        object ViewPager2 {
            private const val VERSION = "1.0.0"
            const val viewPager2 = "androidx.viewpager2:viewpager2:$VERSION"
        }
        object ConstraintLayout {
            private const val VERSION = "2.1.4"
            const val constraintLayout = "androidx.constraintlayout:constraintlayout:$VERSION"
        }
        object Test {
            private const val VERSION = "1.4.0"
            const val rules = "androidx.test:rules:$VERSION"
            object Espresso {
                private const val VERSION = "3.4.0"
                const val idlingResource = "androidx.test.espresso:espresso-idling-resource:$VERSION"
            }
        }
        object Lifecycle {
            private const val VERSION = "2.5.1"
            const val viewmodel = "androidx.lifecycle:lifecycle-viewmodel:$VERSION"
            const val viewmodelKtx = "androidx.lifecycle:lifecycle-viewmodel-ktx:$VERSION"
            const val viewmodelCompose = "androidx.lifecycle:lifecycle-viewmodel-compose:$VERSION"
        }
        object SwipeRefreshLayout {
            private const val VERSION = "1.1.0"
            const val swipeRefresh = "androidx.swiperefreshlayout:swiperefreshlayout:$VERSION"
        }
    }
    object Android {
        object Tools {
            private const val VERSION = "30.2.1"
            const val sdkCommon = "com.android.tools:sdk-common:$VERSION"
            const val common = "com.android.tools:common:$VERSION"
        }
        object InstallReferrer {
            private const val VERSION = "2.2"
            const val installReferrer = "com.android.installreferrer:installreferrer:$VERSION"
        }
    }
    object Square {
        object JavaPoet {
            private const val VERSION = "1.13.0"
            const val javaPoet = "com.squareup:javapoet:$VERSION"
        }
    }
    object Google {
        object FlexBox {
            private const val VERSION = "3.0.0"
            const val flexbox = "com.google.android.flexbox:flexbox:$VERSION"
        }
        object Tink {
            private const val VERSION = "1.2.2"
            const val tink = "com.google.crypto.tink:tink-android:$VERSION"
        }
        object Guava {
            private const val VERSION = "31.1-android"
            const val guava = "com.google.guava:guava:$VERSION"
        }
        object Material {
            private const val VERSION = "1.8.0"
            const val material = "com.google.android.material:material:$VERSION"
        }
        object Play {
            const val core = "com.google.android.play:core:1.10.3"
        }
        object GMS {
            object Plus {
                private const val VERSION = "17.0.0"
                const val plus = "com.google.android.gms:play-services-plus:$VERSION"
            }
        }
        object JUniversalCharDet {
            private const val VERSION = "1.0.3"
            const val juniversalchardet = "com.googlecode.juniversalchardet:juniversalchardet:$VERSION"
        }
    }
    object AirBnb {
        object Lottie {
            private const val VERSION = "5.2.0"
            const val lottie = "com.airbnb.android:lottie:$VERSION"
        }
    }
    object Facebook {
        object Shimmer {
            private const val VERSION = "0.5.0"
            const val shimmer = "com.facebook.shimmer:shimmer:$VERSION"
        }
    }
    object Picocli {
        private const val VERSION = "4.6.3"
        const val picocli = "info.picocli:picocli:$VERSION"
        const val codeGen = "info.picocli:picocli-codegen:$VERSION"
    }
    object Apache {
        object Commons {
            private const val VERSION = "3.5"
            const val commonsLang = "org.apache.commons:commons-lang3:$VERSION"
        }
        object Mime4j {
            private const val VERSION = "0.8.5"
            const val core = "org.apache.james:apache-mime4j-core:$VERSION"
        }
    }
    object Commons {
        object IO {
            private const val VERSION = "2.6"
            const val commonsIo = "commons-io:commons-io:$VERSION"
        }
        object Codec {
            private const val VERSION = "1.10"
            const val commonsCodec = "commons-codec:commons-codec:$VERSION"
        }
    }
    object JUnit {
        private const val VERSION = "4.13.2"
        const val junit = "junit:junit:$VERSION"
    }
    object Mockito {
        private const val VERSION = "2.24.0"
        const val core = "org.mockito:mockito-core:$VERSION"
        object Kotlin {
            private const val VERSION = "2.2.0"
            const val mockitoKotlin = "com.nhaarman.mockitokotlin2:mockito-kotlin:$VERSION"
        }
    }
    object RxJava {
        private const val VERSION = "2.2.21"
        const val rxJava = "io.reactivex.rxjava2:rxjava:$VERSION"
        object RxAndroid {
            private const val VERSION = "2.1.1"
            const val rxAndroid = "io.reactivex.rxjava2:rxandroid:$VERSION"
        }
    }
    object Jackson {
        private const val VERSION = "2.8.6"
        const val core = "com.fasterxml.jackson.core:jackson-core:$VERSION"
        const val databind = "com.fasterxml.jackson.core:jackson-databind:$VERSION"
    }
    object ThreeTen {
        private const val VERSION = "1.6.0:no-tzdb"
        const val threeTenBp = "org.threeten:threetenbp:$VERSION"
    }
    object Jooq {
        private const val VERSION = "0.9.14"
        const val joor = "org.jooq:joor:$VERSION"
    }
    object JakeWharton {
        object ThreeTenAbp {
            private const val VERSION = "1.4.0"
            const val threeTen = "com.jakewharton.threetenabp:threetenabp:$VERSION"
        }
    }
    object AssertJ {
        private const val VERSION = "2.6.0"
        const val core = "org.assertj:assertj-core:$VERSION"
    }
    object JSoup {
        private const val VERSION = "1.14.3"
        const val jsoup = "org.jsoup:jsoup:$VERSION"
    }
    object Nikartm {
        private const val VERSION = "2.0.0"
        const val imageSupport = "io.github.nikartm:image-support:$VERSION"
    }
    object MockK {
        private const val VERSION = "1.12.4"
        const val mockk = "io.mockk:mockk:$VERSION"
    }
}
