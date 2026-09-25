package my.openlab.roomy.domain.model

/** A sensor placed in the room. Coordinates are normalised 0..1: x = width, y = depth, z = height. */
data class SensorPoint(val x: Double, val y: Double, val z: Double, val celsius: Double)

/** Which room axis the slice plane is perpendicular to. */
enum class SliceAxis { X, Y, Z }

/**
 * Temperature anywhere in the room, estimated from a few sensors by inverse-distance weighting.
 * ponytail: IDW is O(sensors) per sample; precompute a voxel grid if sensors grow past ~100.
 */
class RoomField(val sensors: List<SensorPoint>) {

    init {
        require(sensors.isNotEmpty()) { "RoomField needs at least one sensor" }
    }

    fun celsiusAt(x: Double, y: Double, z: Double): Double {
        var weighted = 0.0
        var total = 0.0
        for (s in sensors) {
            val dx = x - s.x
            val dy = y - s.y
            val dz = z - s.z
            val d2 = dx * dx + dy * dy + dz * dz
            if (d2 < 1e-9) return s.celsius
            val w = 1.0 / d2
            weighted += w * s.celsius
            total += w
        }
        return weighted / total
    }

    /** Point on a slice plane at [position] along [axis]; [u] runs left to right, [v] bottom to top. */
    fun celsiusOnSlice(axis: SliceAxis, position: Double, u: Double, v: Double): Double = when (axis) {
        SliceAxis.X -> celsiusAt(position, u, v)
        SliceAxis.Y -> celsiusAt(u, position, v)
        SliceAxis.Z -> celsiusAt(u, v, position)
    }

    /** [cells] x [cells] samples of one slice, cell centres, row 0 at the top. */
    fun slice(axis: SliceAxis, position: Double, cells: Int): Array<DoubleArray> =
        Array(cells) { row ->
            val v = 1.0 - (row + 0.5) / cells
            DoubleArray(cells) { col -> celsiusOnSlice(axis, position, (col + 0.5) / cells, v) }
        }

    companion object {
        /**
         * The live sensor in the middle plus 8 corner sensors: warmer near the ceiling and
         * the window (x = 1), a cold draught at the door corner (x = 0, y = 0).
         * ponytail: made-up layout; replace with real sensor positions once the gateway has them.
         */
        fun demoAround(celsius: Double): RoomField {
            val ends = listOf(0.0, 1.0)
            val corners = ends.flatMap { x ->
                ends.flatMap { y ->
                    ends.map { z ->
                        SensorPoint(x, y, z, celsius + (z - 0.5) * 4 + (x - 0.5) * 3 - (1 - x) * (1 - y) * 2)
                    }
                }
            }
            return RoomField(corners + SensorPoint(0.5, 0.5, 0.5, celsius))
        }
    }
}
