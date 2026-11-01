import java.text.SimpleDateFormat
import java.util.Date

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// 화면 맨 위에 보여줄 빌드 이름. 항상 R번호로 보여준다 (사용자 요청, 11/01).
// GitHub Actions에서 빌드하면 "R8 (커밋 abc1234)"처럼 R번호(GitHub 빌드 번호)와 커밋 번호를 쓴다.
// 집 PC에서 빌드하면 "R9 · PC 11/01 12:05"처럼 쓴다. 9 = GitHub가 남긴 R태그 중 가장 큰 번호 + 1,
// 즉 이 코드를 올리면 GitHub가 만들 다음 번호다. (태그를 받으려면 PC에서 먼저 git fetch --tags)
val runNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()
val latestTagNumber = runCatching {
    ProcessBuilder("git", "tag", "--list", "R*").directory(rootDir).start()
        .inputStream.bufferedReader().readLines()
        .mapNotNull { it.trim().removePrefix("R").toIntOrNull() }
        .maxOrNull()
}.getOrNull() ?: 0
val rNumber = runNumber ?: (latestTagNumber + 1)
val buildLabel = if (runNumber != null) {
    "R$runNumber (커밋 ${System.getenv("GITHUB_SHA")?.take(7)})"
} else {
    "R$rNumber · PC ${SimpleDateFormat("MM/dd HH:mm").format(Date())}"
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
        // 버전 번호 = R번호. 같은 R번호의 GitHub 판을 PC 판 위에 그대로 덮어 설치할 수 있다
        versionCode = rNumber
        versionName = if (runNumber != null) "R$runNumber" else "R$rNumber-PC"
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

    // 서버(Supabase)와 웹소켓으로 실시간 연결하는 도구 (차량용 수신)
    "carImplementation"("com.squareup.okhttp3:okhttp:4.12.0")

    // 계산이 맞는지 PC에서 자동으로 확인하는 시험 도구 (app/src/test)
    testImplementation("junit:junit:4.13.2")
}
