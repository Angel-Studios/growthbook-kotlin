import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackOutput
import org.jetbrains.kotlin.gradle.targets.js.yarn.yarn

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    id("maven-publish")
    id("signing")
    alias(libs.plugins.dokka)
}

group = "io.growthbook.sdk"
version = "1.1.61"

kotlin {
    androidTarget {
        publishLibraryVariants("release")
    }

    js {
        browser {
            commonWebpackConfig {
                output = KotlinWebpackOutput(
                    library = project.name,
                    libraryTarget = KotlinWebpackOutput.Target.UMD,
                    globalObject = KotlinWebpackOutput.Target.WINDOW
                )
            }
            testTask {
                useMocha()
            }
        }
    }

    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = libs.versions.jvmTarget.get()
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlin.coroutines)
                implementation(libs.bignum)
                implementation(libs.krypto)
                implementation(libs.kotlin.serialization.json)

                api(projects.core)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(libs.kotlin.serialization.json)
                implementation(libs.ktor.json)
                implementation(libs.annotations)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.startup)
                implementation(libs.krypto.android)
            }
        }
        val androidUnitTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation(libs.kotlin.coroutines.test)
                implementation(libs.ktor.client.mock)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(libs.ktor.client.java)
                implementation(libs.krypto.jvm)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(libs.ktor.client)
                implementation(libs.krypto.js)
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

android {
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    namespace = "com.sdk.growthbook"
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()

        consumerProguardFiles("consumer-rules.pro")
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

tasks.dokkaHtml {
    outputDirectory.set(layout.buildDirectory.file("dokka").get().asFile)
}

/**
 * This task deletes older documents
 */
val deleteDokkaOutputDir by tasks.register<Delete>("deleteDokkaOutputDirectory") {
    delete(tasks.dokkaHtml)
}

/**
 * This task creates JAVA Docs for Release
 */
val javadocJar = tasks.register<Jar>("javadocJar") {
    dependsOn(deleteDokkaOutputDir, tasks.dokkaHtml)
    archiveClassifier.set("javadoc")
    from(tasks.dokkaHtml)
}

/**
 * Publishing Task for MavenCentral
 */
publishing {
    repositories {
        maven {
            name = "kotlin"
            val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            credentials {
                username = System.getenv("GB_SONATYPE_USERNAME")
                password = System.getenv("GB_SONATYPE_PASSWORD")
            }
        }
    }

    publications {
        withType<MavenPublication> {
            artifact(javadocJar)
            pom {
                name.set("kotlin")
                description.set("Powerful A/B testing SDK for Kotlin")
                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                url.set("https://github.com/growthbook/growthbook-kotlin")
                issueManagement {
                    system.set("Github")
                    url.set("https://github.com/growthbook/growthbook-kotlin/issues")
                }
                scm {
                    connection.set("https://github.com/growthbook/growthbook-kotlin.git")
                    url.set("https://github.com/growthbook/growthbook-kotlin")
                }
                developers {
                    developer {
                        name.set("Nicholas Pearson")
                        email.set("nicholaspearson918@gmail.com")
                    }
                }
            }
        }
    }
}

/**
 * Signing JAR using GPG Keys
 */
signing {
    isRequired = System.getenv("CI") == "true"
    useInMemoryPgpKeys(
        System.getenv("GPG_PRIVATE_KEY"),
        System.getenv("GPG_PRIVATE_PASSWORD")
    )
    sign(publishing.publications)
}