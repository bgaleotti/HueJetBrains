package com.github.asarco.huejetbrains.listeners

import com.github.ajalt.colormath.Ansi256
import com.github.asarco.huejetbrains.*
import com.github.asarco.huejetbrains.components.HuePersistentState
import com.intellij.execution.testframework.AbstractTestProxy
import com.intellij.execution.testframework.TestStatusListener
import inkapplications.shade.Shade
import inkapplications.shade.constructs.Coordinates
import inkapplications.shade.constructs.percent
import inkapplications.shade.lights.LightState
import inkapplications.shade.lights.LightStateModification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TestResultListener : TestStatusListener() {

    private val configState get() = HuePersistentState.instance.state
    private val shade = Shade(storage = ConfigTokenStorage(configState!!), initBaseUrl = "http://${configState?.hueIp}")

    override fun testSuiteFinished(root: AbstractTestProxy?) {

        if (configState?.hueEnabled == true) {
            val lightId = configState?.hueLightId!!
            if (root != null) {
                if (root.isPassed) {
                    CoroutineScope(Dispatchers.Default).launch {
                        val currentLightState = shade.lights.getLight(lightId).state
                        testsPassed(lightId, shade, currentLightState)
                    }
                } else {
                    CoroutineScope(Dispatchers.Default).launch {
                        val currentLightState = shade.lights.getLight(lightId).state
                        testsFailed(lightId, shade, currentLightState)
                    }
                }
            }
        }
    }

    private suspend fun testsFailed(lightId: String, shade: Shade, prevLightState: LightState) {
        pulseLights(shade, lightId, prevLightState, RED)
    }

    private suspend fun testsPassed(lightId: String, shade: Shade, prevLightState: LightState) {
        pulseLights(shade, lightId, prevLightState, GREEN)
    }

    private suspend fun pulseLights(shade: Shade, lightId: String, prevLightState: LightState, colour: Int) {
        repeat(PULSE_REPEATS) {
            shade.lights.setState(lightId, LightStateModification(on = true, brightness = PULSE_MAX_BRIGHT.percent,
                cieColorCoordinates = Coordinates(Ansi256(colour))))
            delay(PULSE_DELAY_IN)
            shade.lights.setState(lightId, LightStateModification(on = false))
            delay(PULSE_DELAY_OUT)
        }
        shade.lights.setState(lightId, LightStateModification(on = prevLightState.on,
            cieColorCoordinates = prevLightState.cieColorCoordinates, brightness = prevLightState.brightness))
    }
}
