plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    compileSdk = 35
    defaultConfig {
        applicationId = "org.sternbach.software.wabbitemu"
        minSdk = 21
        targetSdk = 35
        versionCode = 7
        versionName = "1.0.7"
    }
    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
        }
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/jni/CMakeLists.txt")
        }
    }
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            pickFirsts.add("LICENSE.Apachev2")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    namespace = "io.github.angelsl.wabbitemu"
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.drawerlayout)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.jsr305)
    implementation(libs.okhttp)
    implementation(libs.okhttp.urlconnection)
    implementation(libs.jsoup)
    implementation(libs.cab.parser)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
}
