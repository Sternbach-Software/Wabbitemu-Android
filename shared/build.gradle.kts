plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

kotlin {
    androidTarget()

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    jvm("desktop")

    sourceSets {
        val commonMain by getting
        val androidMain by getting
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
        val desktopMain by getting
    }
}

android {
    namespace = "io.github.angelsl.wabbitemu.shared"
    compileSdk = 36
    defaultConfig {
        minSdk = 21
    }
}
