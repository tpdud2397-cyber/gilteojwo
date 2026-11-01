package com.gilteojwo.approach

import com.gilteojwo.approach.RelativeDirection.FRONT
import com.gilteojwo.approach.RelativeDirection.LEFT
import com.gilteojwo.approach.RelativeDirection.REAR
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
}
