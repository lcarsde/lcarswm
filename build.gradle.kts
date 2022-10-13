import java.io.ByteArrayOutputStream

plugins {
    kotlin("multiplatform") version "1.7.20"
}

group "de.atennert"

repositories {
    mavenCentral()
}

kotlin {
    val nativeTarget = when (System.getProperty("os.name")) {
        "Linux" -> {
            val architecture: String = ByteArrayOutputStream().use { os ->
                project.exec {
                    commandLine("uname", "-m")
                    standardOutput = os
                }
                os.toString().trim()
            }
            when {
                architecture == "x86_64" -> linuxX64("native")
                architecture.startsWith("arm64") -> linuxArm64("native")
                architecture.startsWith("aarch64") -> linuxArm64("native")
                else -> throw GradleException("Host CPU architecture not supported: $architecture.\n" +
                        "If you think, it should work, please:\n" +
                        "1. check your CPU architecture with \"uname -a\",\n" +
                        "2. find the corresponding target in this list: https://kotlinlang.org/docs/mpp-dsl-reference.html#targets,\n" +
                        "3. add a it to the architectures in build.gradle.kts,\n" +
                        "4. run the build and\n" +
                        "5. if it works, please create a ticket with the changes on https://github.com/lcarsde/lcarswm/issues to have it added permanently.")
            }
        }
        else -> throw GradleException("Host OS is not supported.")
    }

    nativeTarget.apply {
        binaries {
            executable {
                entryPoint = "main"
            }
        }
        compilations.getByName("main") {
            val xlib by cinterops.creating
        }
    }
    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1")
            }
        }
        commonTest {
            dependencies {
                implementation( "org.jetbrains.kotlin:kotlin-test-common")
            }
        }
        val nativeMain by getting
        val nativeTest by getting
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "7.1.1"
    distributionType = Wrapper.DistributionType.ALL
}
