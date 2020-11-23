plugins {
    kotlin("multiplatform") version "1.4.20"
}

group "de.atennert"
version "20.3"

repositories {
    mavenCentral()
}

kotlin {
    linuxX64("native") {
        binaries {
            executable()
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
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "6.5.1"
    distributionType = Wrapper.DistributionType.ALL
}
