package com.github.asarco.huejetbrains.configurable

import com.github.asarco.huejetbrains.ConfigTokenStorage
import com.github.asarco.huejetbrains.components.HuePersistentState
import com.intellij.openapi.Disposable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.Configurable.NoScroll
import com.intellij.openapi.ui.ComboBox
import com.intellij.util.ui.FormBuilder
import inkapplications.shade.Shade
import inkapplications.shade.lights.LightStateModification
import kotlinx.coroutines.*
import org.jetbrains.rpc.LOG
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.event.ActionEvent
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import javax.swing.*

class HuePersistenStateConfigurable : Configurable, NoScroll, Disposable {

    private val configState get() = HuePersistentState.instance.state

    private var ipField = JTextField("")
    private var discoveryButton = JButton("Discover")
    private var lightList = ComboBox<LightItem>()
    private var labelPressButton = JLabel()

    private val shade = Shade(storage = ConfigTokenStorage(configState!!))


    override fun createComponent(): JComponent? {
        val formPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent("Enter the IP address of your Hue hub", JPanel(FlowLayout(FlowLayout.LEFT)).also {
                    it.add(ipField)
                    it.add(JLabel(" or "))
                    it.add(discoveryButton)
                })
                .addComponent(labelPressButton)
                .addLabeledComponent("Choose the light", JPanel(FlowLayout(FlowLayout.LEFT)).also {
                    it.add(lightList)
                })
                .panel

        lightList.isEditable = false
        lightList.addActionListener { flashLight(it) }
        discoveryButton.isEnabled = false //TODO: implement discovery
        ipField.text = configState?.hueIp ?: ""

        shade.setBaseUrl("http://${ipField.text}")
        if (configState?.hueToken.isNullOrBlank()) {
            LOG.info("Waiting for button to be pressed")
            labelPressButton.text = "Press the button on the Hue hub"
            try {
                runBlocking { shade.auth.awaitToken() }
                labelPressButton.text = "Success! Registered with the Hue hub!"
            } catch (e: Exception) {
                LOG.warn("Acquiring token failed: ${e.message}")
            }
        }

        try {
            InetAddress.getAllByName(ipField.text)
            runBlocking {
                shade.lights.getLights().forEach { (id, light) -> lightList.addItem(LightItem(id, light.name)) }
            }
            lightList.selectedItem = LightItem(configState?.hueLightId!!, configState?.hueLightName!!)
        } catch (e: Exception) {
            LOG.warn("IP empty or incorrect: ${e.message}")
        }

        return JPanel(BorderLayout()).also { it.add(formPanel, BorderLayout.NORTH) }
    }

    private fun flashLight(event: ActionEvent?) {
        val combo = event?.source as ComboBox<LightItem>
        val (id, name) = combo.selectedItem as LightItem
        CoroutineScope(Dispatchers.Default).launch {
            val currentState = shade.lights.getLight(id).state
            shade.lights.setState(id, LightStateModification(on = false))
            delay(250)
            shade.lights.setState(id, LightStateModification(on = true))
            delay(250)
            shade.lights.setState(id, LightStateModification(on = currentState.on))

        }
    }

    override fun isModified(): Boolean {
        return true
    }

    override fun apply() {
        configState?.hueIp = ipField.text ?: ""
        val selectedLight = lightList.selectedItem as LightItem
        configState?.hueLightId = selectedLight.id
        configState?.hueLightName = selectedLight.name
    }

    override fun getDisplayName(): String = "Hue lights configuration"

    override fun dispose() {
        TODO("Not yet implemented")
    }

    data class LightItem(val id: String, val name: String) {
        override fun toString(): String {
            return name
        }
    }

}