import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "io.github.scottlz0310.shoptools"
    compileSdk = 37

    val keystoreProps = Properties().also { props ->
        val propsFile = rootProject.file("keystore.properties")
        if (propsFile.exists()) props.load(propsFile.reader(Charsets.UTF_8))
    }

    signingConfigs {
        create("release") {
            val storeFilePath = keystoreProps["storeFile"] as? String
            if (storeFilePath != null) {
                storeFile = file(storeFilePath)
                storePassword = keystoreProps["storePassword"] as String
                keyAlias = keystoreProps["keyAlias"] as String
                keyPassword = keystoreProps["keyPassword"] as String
            }
        }
    }

    defaultConfig {
        applicationId = "io.github.scottlz0310.shoptools"
        minSdk = 26
        targetSdk = 37
        versionCode = 4
        versionName = "1.2.0"

        testInstrumentationRunner = "io.github.scottlz0310.shoptools.HiltTestRunner"
    }

    buildTypes {
        debug {
            // Codecov へ送るユニットテストのカバレッジ（createDebugUnitTestCoverageReport）を出力する。
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
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

    buildFeatures {
        compose = true
    }

    lint {
        // 既定は Warning で、昇格しなければ targetSdk が古くなっても CI は落ちない（#77）。
        // 新しい Android API が出るたび CI が赤くなるのは意図どおりで、引き上げの検証を促す通知になる。
        error += setOf("OldTargetApi", "ExpiredTargetSdkVersion")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)

    // Compose BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    // Navigation
    implementation(libs.navigation.compose)

    // Lifecycle / ViewModel
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // Hilt
    implementation(libs.hilt.android)
    // Hilt 2.60 の生成 Java ソースが参照するコンパイル時専用 annotation。
    compileOnly(libs.errorprone.annotations)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.lifecycle.viewmodel.compose)

    // DataStore
    implementation(libs.datastore.preferences)

    // Material Components (テーマベースライン)
    implementation(libs.material)

    // CameraX
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // ML Kit
    implementation(libs.mlkit.text.recognition.japanese)

    // Tests
    testImplementation(libs.junit)

    // Instrumentation tests (Compose UI)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    // compose-ui-test が引く espresso-core 3.5.0 は、API 37 で廃止された隠し API
    // (InputManager.getInstance) を呼び NoSuchMethodException になるため 3.7.0 に固定する。
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.hilt.android.testing)
    // Hilt の生成 Java ソースが参照するコンパイル時専用 annotation（main と同じ理由）。
    androidTestCompileOnly(libs.errorprone.annotations)
    kspAndroidTest(libs.hilt.compiler)
    debugImplementation(libs.compose.ui.test.manifest)
}
