plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// GitHub Actions에서 빌드하면 빌드 번호(R번호)와 커밋 번호가 들어간다. 집 PC에서 빌드하면 1, "local".
val buildNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
val commitId = System.getenv("GITHUB_SHA")?.take(7) ?: "local"

android {
    namespace = "com.gilteojwo"
    compileSdk = 34

    // 안드로이드 차량용 표준 기능(주행 제한, 속도·기어 읽기)을 쓰기 위한 라이브러리
    useLibrary("android.car")

    defaultConfig {
        applicationId = "com.gilteojwo"
        minSdk = 28
        targetSdk = 34
        versionCode = buildNumber
        versionName = "R$buildNumber"
        buildConfigField("int", "BUILD_NUMBER", "$buildNumber")
        buildConfigField("String", "COMMIT_ID", "\"$commitId\"")
    }

    // 개발용 서명 키: 매번 같은 도장을 찍어서 APK를 덮어 설치할 수 있게 한다 (비밀 아님, 시험용).
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    // 설치 파일 두 종류: 폰용(송신)과 차량용(수신 + Pleos 송신 모드)
    flavorDimensions += "device"
    productFlavors {
        create("phone") {
            dimension = "device"
            applicationIdSuffix = ".phone"
        }
        create("car") {
            dimension = "device"
            applicationIdSuffix = ".car"
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Pleos SDK는 차량용에만 넣는다 (폰용에는 넣지 않음)
    "carImplementation"("ai.pleos.playground:Vehicle:2.3.2")
    "carImplementation"("ai.pleos.playground:FusedLocation:1.1.3")
    "carImplementation"("ai.pleos.playground:TextToSpeech:3.3.1")
}
