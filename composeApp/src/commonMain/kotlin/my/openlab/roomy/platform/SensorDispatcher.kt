package my.openlab.roomy.platform

import kotlinx.coroutines.CoroutineDispatcher

/**
 * One dedicated, manually configured thread for sensor work (polling, parsing).
 * Android builds it in Kotlin; iOS gets it from Swift through RoomyBridge.
 */
expect val sensorDispatcher: CoroutineDispatcher
