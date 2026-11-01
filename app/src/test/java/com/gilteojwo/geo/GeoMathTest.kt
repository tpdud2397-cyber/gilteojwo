package com.gilteojwo.geo

import com.gilteojwo.CITY_HALL
import com.gilteojwo.moved
import org.junit.Assert.assertEquals
import org.junit.Test

class GeoMathTest {
    @Test
    fun `위도 0_001도 차이는 약 111m`() {
        val distance = GeoMath.distanceMeters(GeoPoint(37.0, 127.0), GeoPoint(37.001, 127.0))
        assertEquals(111.2, distance, 0.5)
    }

    @Test
    fun `서울시청에서 북쪽으로 300m 옮긴 점까지는 300m`() {
        assertEquals(300.0, GeoMath.distanceMeters(CITY_HALL, CITY_HALL.moved(300.0, 0.0)), 1.0)
    }

    @Test
    fun `북쪽에 있는 점의 방위는 0도`() {
        assertEquals(0.0, GeoMath.bearingDegrees(CITY_HALL, CITY_HALL.moved(100.0, 0.0)), 0.5)
    }

    @Test
    fun `동쪽에 있는 점의 방위는 90도`() {
        assertEquals(90.0, GeoMath.bearingDegrees(CITY_HALL, CITY_HALL.moved(0.0, 100.0)), 0.5)
    }

    @Test
    fun `남쪽에 있는 점의 방위는 180도`() {
        assertEquals(180.0, GeoMath.bearingDegrees(CITY_HALL, CITY_HALL.moved(-100.0, 0.0)), 0.5)
    }

    @Test
    fun `서쪽에 있는 점의 방위는 270도`() {
        assertEquals(270.0, GeoMath.bearingDegrees(CITY_HALL, CITY_HALL.moved(0.0, -100.0)), 0.5)
    }

    @Test
    fun `350도와 10도는 20도 차이`() {
        assertEquals(20.0, GeoMath.angleDifference(350.0, 10.0), 0.001)
    }

    @Test
    fun `90도와 270도는 180도 차이`() {
        assertEquals(180.0, GeoMath.angleDifference(90.0, 270.0), 0.001)
    }

    @Test
    fun `한 점에서 동쪽으로 500m 간 점은 거리 500m, 방위 90도`() {
        val moved = GeoMath.destination(CITY_HALL, 90.0, 500.0)

        assertEquals(500.0, GeoMath.distanceMeters(CITY_HALL, moved), 0.5)
        assertEquals(90.0, GeoMath.bearingDegrees(CITY_HALL, moved), 0.5)
    }
}
