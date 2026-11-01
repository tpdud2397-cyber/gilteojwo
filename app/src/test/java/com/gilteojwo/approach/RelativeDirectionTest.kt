package com.gilteojwo.approach

import com.gilteojwo.approach.RelativeDirection.FRONT
import com.gilteojwo.approach.RelativeDirection.FRONT_LEFT
import com.gilteojwo.approach.RelativeDirection.FRONT_RIGHT
import com.gilteojwo.approach.RelativeDirection.LEFT
import com.gilteojwo.approach.RelativeDirection.REAR
import com.gilteojwo.approach.RelativeDirection.REAR_LEFT
import com.gilteojwo.approach.RelativeDirection.REAR_RIGHT
import com.gilteojwo.approach.RelativeDirection.RIGHT
import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeDirectionTest {
    @Test
    fun `북쪽으로 갈 때 북쪽에 있으면 전방`() {
        assertEquals(FRONT, RelativeDirection.of(0.0, 0.0))
    }

    @Test
    fun `북쪽으로 갈 때 동쪽에 있으면 우측`() {
        assertEquals(RIGHT, RelativeDirection.of(0.0, 90.0))
    }

    @Test
    fun `북쪽으로 갈 때 남쪽에 있으면 후방`() {
        assertEquals(REAR, RelativeDirection.of(0.0, 180.0))
    }

    @Test
    fun `북쪽으로 갈 때 서쪽에 있으면 좌측`() {
        assertEquals(LEFT, RelativeDirection.of(0.0, 270.0))
    }

    @Test
    fun `동쪽으로 갈 때 북쪽에 있으면 좌측`() {
        assertEquals(LEFT, RelativeDirection.of(90.0, 0.0))
    }

    @Test
    fun `350도로 갈 때 10도 쪽에 있으면 전방`() {
        assertEquals(FRONT, RelativeDirection.of(350.0, 10.0))
    }

    @Test
    fun `북쪽으로 갈 때 대각선 쪽은 우측 전방·우측 후방·좌측 후방·좌측 전방`() {
        assertEquals(FRONT_RIGHT, RelativeDirection.of(0.0, 45.0)) // 1~2시
        assertEquals(REAR_RIGHT, RelativeDirection.of(0.0, 135.0)) // 4~5시
        assertEquals(REAR_LEFT, RelativeDirection.of(0.0, 225.0)) // 7~8시
        assertEquals(FRONT_LEFT, RelativeDirection.of(0.0, 315.0)) // 10~11시
    }

    @Test
    fun `6시에서 20도 벗어나면 아직 후방이고 30도 벗어나면 우측 후방`() {
        assertEquals(REAR, RelativeDirection.of(0.0, 160.0))
        assertEquals(REAR_RIGHT, RelativeDirection.of(0.0, 150.0))
    }
}
