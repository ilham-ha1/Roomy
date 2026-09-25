package my.openlab.roomy.platform

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import my.openlab.roomy.RoomyBridge
import kotlin.coroutines.CoroutineContext

/** Swift implements this with a real Foundation `Thread` and registers it in RoomyBridge. */
interface SensorThreadRunner {
    fun execute(block: () -> Unit)
}

actual val sensorDispatcher: CoroutineDispatcher = object : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        // Before Swift registers its thread (e.g. previews, tests), fall back to the shared pool.
        val runner = RoomyBridge.sensorThread ?: return Dispatchers.Default.dispatch(context, block)
        runner.execute { block.run() }
    }
}
