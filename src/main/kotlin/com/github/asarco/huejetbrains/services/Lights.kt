package com.github.asarco.huejetbrains.services

import inkapplications.shade.Shade

class Lights(val shade: Shade) {
    suspend fun listLights() {
        shade.lights.getLights().forEach { (id, light) ->
            println("Found light $id named ${light.name}")
        }
    }
}
