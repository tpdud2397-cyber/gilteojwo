package com.gilteojwo.receive

/**
 * Supabase 서버 주소와 공개 키.
 * 공개 키(publishable)는 앱에 넣어도 되는 키다. 표마다 접근 규칙(RLS)이 걸려 있어서
 * 이 키로는 '긴급차량 현재 위치 읽기', '양보 확인 쓰기', '등록 코드가 맞을 때 송신'만 할 수 있다.
 * 모든 권한을 가진 비밀 키(secret, service_role)는 절대 앱이나 저장소에 넣지 않는다.
 */
object ServerConfig {
    const val URL = "https://fywibzrjqjdtntdhungh.supabase.co"
    const val PUBLISHABLE_KEY = "sb_publishable_mgRw67mksqkDiKK5beWAgA_XVNQtqVP"
}
