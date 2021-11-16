plugins {
    kotlin("multiplatform") version "1.6.0"
}

group "de.atennert"

repositories {
    mavenCentral()
}

kotlin {
    val nativeTarget = when (System.getProperty("os.name")) {
        "Linux" -> linuxX64("native")
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
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
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
    gradleVersion = "6.5.1"
    distributionType = Wrapper.DistributionType.ALL
}
