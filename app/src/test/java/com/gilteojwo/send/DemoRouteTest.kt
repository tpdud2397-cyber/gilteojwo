package com.gilteojwo.send

import com.gilteojwo.CITY_HALL
import com.gilteojwo.geo.GeoMath
import com.gilteojwo.geo.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 사양서 S-4: 미리 정한 경로를 실제 속도로 재생해 송신한다. 목표(수신 차)는 북쪽을 보고 서 있다 */
class DemoRouteTest {
    @Test
    fun `후방 접근은 목표 남쪽 1,200m에서 북쪽으로 시속 60km로 출발한다`() {
        val fix = DemoRoute.REAR_APPROACH.fixAt(CITY_HALL, elapsedSeconds = 0.0)!!
        val start = GeoPoint(fix.latitude, fix.longitude)

        assertEquals(1200.0, GeoMath.distanceMeters(CITY_HALL, start), 1.0)
        assertEquals(180.0, GeoMath.bearingDegrees(CITY_HALL, start), 1.0) // 목표에서 보면 남쪽
        assertEquals(0.0, fix.headingDegrees, 0.1) // 북쪽으로 달린다
        assertEquals(60.0, fix.speedKmh, 0.1)
    }

    @Test
    fun `후방 접근 30초 뒤에는 남쪽 700m`() {
        val fix = DemoRoute.REAR_APPROACH.fixAt(CITY_HALL, elapsedSeconds = 30.0)!!

        assertEquals(700.0, GeoMath.distanceMeters(CITY_HALL, GeoPoint(fix.latitude, fix.longitude)), 1.0)
    }

    @Test
    fun `목표를 지나 300m 더 가면 경로가 끝난다`() {
        // 1,200m + 300m = 1,500m, 시속 60km(초속 16.7m)로 90초
        assertNull(DemoRoute.REAR_APPROACH.fixAt(CITY_HALL, elapsedSeconds = 91.0))
    }

    @Test
    fun `교차로 측면 접근은 목표 서쪽(왼쪽)에서 동쪽으로 달려 가로지른다`() {
        val fix = DemoRoute.SIDE_CROSSING.fixAt(CITY_HALL, elapsedSeconds = 0.0)!!
        val start = GeoPoint(fix.latitude, fix.longitude)

        assertEquals(270.0, GeoMath.bearingDegrees(CITY_HALL, start), 1.0) // 서쪽
        assertEquals(90.0, fix.headingDegrees, 0.1) // 동쪽으로 달린다
    }
}
