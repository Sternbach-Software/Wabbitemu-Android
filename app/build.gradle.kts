plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    compileSdk = 36
    defaultConfig {
        applicationId = "org.sternbach.software.wabbitemu"
        minSdk = 21
        targetSdk = 36
        versionCode = 7
        versionName = "1.0.7"
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
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
    implementation(libs.kotlin.stdlib)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.drawerlayout)
    implementation(libs.androidx.preference.ktx)
//    implementation 'net.margaritov.preference.colorpicker.ColorPickerPreference:ColorPickerPreference:1.0.0'
    implementation(libs.jsr305)
    implementation(libs.okhttp)
    implementation(libs.okhttp.urlconnection)
    implementation(libs.jsoup)
    implementation(libs.cabparser)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Choose one of the following:
    // Material Design 3
    implementation(libs.androidx.compose.material3)

    // Optional - Integration with activities
    implementation(libs.androidx.activity.compose)
    // Optional - Integration with ViewModels
    implementation(libs.androidx.lifecycle.viewmodel.compose)
}
