plugins {
    kotlin("multiplatform") version "1.4.20"
}

group "de.atennert"
version "20.3"

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
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.1")
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
