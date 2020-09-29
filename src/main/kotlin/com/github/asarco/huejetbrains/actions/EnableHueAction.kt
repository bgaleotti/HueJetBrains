package com.github.asarco.huejetbrains.actions

import com.github.asarco.huejetbrains.HUE_IP
import com.github.asarco.huejetbrains.services.Lights
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import inkapplications.shade.Shade
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class EnableHueAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        //val apiKey = Hue.hueBridgeConnectionBuilder(HUE_IP).initializeApiConnection(HUE_APP_NAME)
        val shade = Shade()
        runBlocking {
            shade.discovery.getDevices().forEach {
                println("found device: ${it.id} ip: ${it.ip}")
            }
            shade.setBaseUrl("http://$HUE_IP")
            val token = shade.auth.awaitToken()
        }
        val lights = Lights(shade)
        GlobalScope.launch { lights.listLights() }

    }


}