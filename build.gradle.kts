plugins {
    kotlin("multiplatform") version "1.5.10"
}

group "de.atennert"
version "21.2"

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
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3")
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
