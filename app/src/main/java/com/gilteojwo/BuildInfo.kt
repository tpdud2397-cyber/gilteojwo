package com.gilteojwo

/** 화면에 보여줄 빌드 정보. 최신 APK가 설치됐는지 확인하는 용도. */
object BuildInfo {
    fun text(): String = "빌드 #${BuildConfig.BUILD_NUMBER} (커밋 ${BuildConfig.COMMIT_ID})"
}
