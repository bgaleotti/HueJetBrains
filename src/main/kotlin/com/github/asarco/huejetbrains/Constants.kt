package com.github.asarco.huejetbrains

const val HUE_IP = "192.168.1.198"
const val HUE_USERNAME = "2aDKJ0xKqhQlgyzUMnOiPTCYVQM5zTDcqlINihyu"
const val HUE_APP_NAME = "HueJetBrains"
const val HUE_ROOM_NAME = "Nightable Ale"
const val HUE_LIGHT_NAME = "Hue go 1"
val HUE_URI = String.format("http://%s/api/%s/", HUE_IP, HUE_USERNAME)
val HUE_LIGHTS = arrayOf("2", "3", "4", "7")
const val RED = "{\"bri\": 142, \"hue\": 65280, \"sat\": 254}"
const val WHITE = "{\"bri\": 149, \"hue\": 0,\"sat\": 0}"
val COLORS = arrayOf(RED, WHITE)
val REQ = HUE_URI + "lights/%s/state"