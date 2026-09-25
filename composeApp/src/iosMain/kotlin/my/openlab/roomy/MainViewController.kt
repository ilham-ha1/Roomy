package my.openlab.roomy

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() = ComposeUIViewController { App(onReading = RoomyBridge::onReading) }
