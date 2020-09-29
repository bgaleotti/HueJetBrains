package com.github.asarco.huejetbrains.listeners

import com.github.ajalt.colormath.Ansi256
import com.github.asarco.huejetbrains.ConfigTokenStorage
import com.github.asarco.huejetbrains.components.HuePersistentState
import com.intellij.execution.testframework.AbstractTestProxy
import com.intellij.execution.testframework.TestStatusListener
import inkapplications.shade.Shade
import inkapplications.shade.constructs.Coordinates
import inkapplications.shade.constructs.percent
import inkapplications.shade.lights.LightState
import inkapplications.shade.lights.LightStateModification
import kotlinx.coroutines.*

class TestResultListener : TestStatusListener() {

    private val configState get() = HuePersistentState.instance.state
    private val shade = Shade(storage = ConfigTokenStorage(configState!!), initBaseUrl = "http://${configState?.hueIp}")

    override fun testSuiteFinished(root: AbstractTestProxy?) {

        val lightId = configState?.hueLightId!!
        val currentLightState = runBlocking { shade.lights.getLight(lightId) }.state
        if (root != null) {
            if (root.isPassed) {
                CoroutineScope(Dispatchers.Default).launch { testsPassed(lightId, shade, currentLightState) }
            } else {
                CoroutineScope(Dispatchers.Default).launch { testsFailed(lightId, shade, currentLightState) }
            }
        }
    }

    private suspend fun testsFailed(lightId: String, shade: Shade, prevLightState: LightState) {
        pulseLights(shade, lightId, prevLightState, 160)
    }

    private suspend fun testsPassed(lightId: String, shade: Shade, prevLightState: LightState) {
        pulseLights(shade, lightId, prevLightState, 40)
    }

    private suspend fun pulseLights(shade: Shade, lightId: String, prevLightState: LightState, colour: Int) {
        repeat(8) {
            shade.lights.setState(lightId, LightStateModification(on = true, brightness = 85.percent,
                    cieColorCoordinates = Coordinates(Ansi256(colour))))
            delay(200)
            shade.lights.setState(lightId, LightStateModification(on = false))
            delay(100)
        }
        shade.lights.setState(lightId, LightStateModification(on = prevLightState.on,
                cieColorCoordinates = prevLightState.cieColorCoordinates, brightness = prevLightState.brightness))
    }
}
