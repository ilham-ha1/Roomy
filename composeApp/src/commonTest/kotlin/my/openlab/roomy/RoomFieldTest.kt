package my.openlab.roomy

import my.openlab.roomy.domain.model.RoomField
import my.openlab.roomy.domain.model.SensorPoint
import my.openlab.roomy.domain.model.SliceAxis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoomFieldTest {

    private val twoSensors = RoomField(
        listOf(SensorPoint(0.0, 0.5, 0.5, 20.0), SensorPoint(1.0, 0.5, 0.5, 30.0)),
    )

    @Test
    fun sensorPositionReturnsItsOwnReading() {
        assertEquals(30.0, twoSensors.celsiusAt(1.0, 0.5, 0.5))
    }

    @Test
    fun midpointBlendsEqually() {
        assertEquals(25.0, twoSensors.celsiusAt(0.5, 0.5, 0.5), absoluteTolerance = 1e-9)
    }

    @Test
    fun sliceAxesMapToTheRightCoordinates() {
        // X slice fixes x, so it sees only the cold sensor's side.
        assertEquals(20.0, twoSensors.celsiusOnSlice(SliceAxis.X, 0.0, u = 0.5, v = 0.5))
        // Z slice: u runs along x, so the right edge is warmer than the left.
        val grid = twoSensors.slice(SliceAxis.Z, 0.5, cells = 4)
        assertTrue(grid[0].last() > grid[0].first())
    }

    @Test
    fun demoCeilingIsWarmerThanFloor() {
        val room = RoomField.demoAround(24.0)
        assertEquals(24.0, room.celsiusAt(0.5, 0.5, 0.5))
        assertTrue(room.celsiusAt(0.5, 0.5, 0.95) > room.celsiusAt(0.5, 0.5, 0.05))
    }
}
