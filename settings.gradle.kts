// 빌드에 필요한 도구와 라이브러리를 어디서 받을지 정한다.
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Pleos SDK(Vehicle, FusedLocation, TextToSpeech)가 있는 저장소
        maven("https://nexus-playground.pleos.ai/repository/maven-releases/")
    }
}

rootProject.name = "gilteojwo"
include(":app")
