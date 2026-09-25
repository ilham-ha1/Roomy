package my.openlab.roomy.platform

import android.os.Process
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

actual val sensorDispatcher: CoroutineDispatcher =
    Executors.newSingleThreadExecutor { job ->
        Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            job.run()
        }, "roomy-sensor").apply { isDaemon = true }
    }.asCoroutineDispatcher()
