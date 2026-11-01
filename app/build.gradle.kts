import java.text.SimpleDateFormat
import java.util.Date

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// 화면 맨 위에 보여줄 빌드 이름.
// GitHub Actions에서 빌드하면 "R8 (커밋 abc1234)"처럼 R번호와 커밋 번호를 쓴다.
// 집 PC에서 빌드하면 R번호가 없으니 "PC 빌드 (11/01 12:05)"처럼 만든 시각을 쓴다.
val runNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()
val buildLabel = if (runNumber != null) {
    "R$runNumber (커밋 ${System.getenv("GITHUB_SHA")?.take(7)})"
} else {
    "PC 빌드 (${SimpleDateFormat("MM/dd HH:mm").format(Date())})"
}

android {
    namespace = "com.gilteojwo"
    compileSdk = 34

    // 안드로이드 차량용 표준 기능(주행 제한, 속도·기어 읽기)을 쓰기 위한 라이브러리
    useLibrary("android.car")

    defaultConfig {
        applicationId = "com.gilteojwo"
        minSdk = 28
        targetSdk = 34
        // PC 빌드는 번호 1. GitHub 판(R2 이상)을 그 위에 덮어 설치할 수 있다
        versionCode = runNumber ?: 1
        versionName = if (runNumber != null) "R$runNumber" else "PC 빌드"
        buildConfigField("String", "BUILD_LABEL", "\"$buildLabel\"")
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

    // 계산이 맞는지 PC에서 자동으로 확인하는 시험 도구 (app/src/test)
    testImplementation("junit:junit:4.13.2")
}
