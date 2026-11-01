package com.gilteojwo

/** 화면에 보여줄 빌드 정보. 최신 APK가 설치됐는지 확인하는 용도. */
object BuildInfo {
    /** GitHub 빌드는 "R8 (커밋 abc1234)", 집 PC 빌드는 "R9 · PC 11/01 12:05" (9 = 다음에 올릴 R번호) */
    fun text(): String = BuildConfig.BUILD_LABEL
}
