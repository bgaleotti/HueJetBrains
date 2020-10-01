package com.github.asarco.huejetbrains.configurable

import com.github.asarco.huejetbrains.ConfigTokenStorage
import com.github.asarco.huejetbrains.FLASH_DELAY
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
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class HuePersistenStateConfigurable : Configurable, NoScroll, Disposable {

    private val configState get() = HuePersistentState.instance.state

    private var enableCheck = JCheckBox("Enable Hue JetBrains")
    private var ipField = JTextField("", 10)
    private var discoveryButton = JButton("Discover")
    private var lightList = ComboBox<LightItem>()
    private var labelPressButton = JLabel()

    private val shade = Shade(storage = ConfigTokenStorage(configState!!))

    override fun createComponent(): JComponent? {
        val formPanel = FormBuilder.createFormBuilder()
            .addComponent(enableCheck)
            .addLabeledComponent(
                "Enter the IP address of your Hue bridge",
                JPanel(FlowLayout(FlowLayout.LEFT)).also {
                    it.add(ipField)
                    it.add(JLabel(" or "))
                    it.add(discoveryButton)
                }
            )
            .addComponent(labelPressButton)
            .addLabeledComponent("Choose the light", JPanel(FlowLayout(FlowLayout.LEFT)).also {
                it.add(lightList)
            })
            .panel

        // properties and listeners for components
        lightList.isEditable = false
        enableCheck.addActionListener { disableEnableControls(it, formPanel) }
        enableCheck.isSelected = configState?.hueEnabled ?: false
        lightList.addActionListener { flashLight(it) }
        discoveryButton.addActionListener { discover() }
        ipField.text = configState?.hueIp ?: ""
        ipField.document.addDocumentListener(docListener)

        loopComponents(formPanel, enableCheck.isSelected)
        enableCheck.isEnabled = true

        try {
            setBaseUrl()
        } catch (e: Exception) {
            LOG.warn("Incorrect or no IP configured")
        }
        CoroutineScope(Dispatchers.Default).launch { register() }
        fillLights()

        return JPanel(BorderLayout()).also { it.add(formPanel, BorderLayout.NORTH) }
    }

    private val docListener = object : DocumentListener {
        override fun insertUpdate(e: DocumentEvent?) {
            fillLightsListen()
        }

        override fun removeUpdate(e: DocumentEvent?) {
            fillLightsListen()
        }

        override fun changedUpdate(e: DocumentEvent?) {
            fillLightsListen()
        }
    }

    private fun setBaseUrl() {
        InetAddress.getAllByName(ipField.text) // Throws exception if IP is incorrect
        shade.setBaseUrl("http://${ipField.text}")
    }

    // Register with the hue bridge
    private suspend fun register() {
        if (configState?.hueToken.isNullOrBlank()) {
            LOG.info("Waiting for button to be pressed")
            labelPressButton.text = "Press the button on the Hue bridge..."
            try {
                val job = GlobalScope.launch { shade.auth.awaitToken() }
                job.join()
                labelPressButton.text = "Success! Registered with the Hue bridge!"
            } catch (e: Exception) {
                labelPressButton.text = "Failed to register with Hue bridge"
                LOG.warn("Acquiring token failed: ${e.message}")
            }
        }
    }

    // executed when something changes in the ipField, therefore it might be triggered many times.
    private fun fillLightsListen() {
        if (ipField.text.length >= 7) { // 7 is the min length an IP address can have (x.x.x.x)
            try {
                setBaseUrl()
                CoroutineScope(Dispatchers.Default).launch {
                    register()
                    fillLights()
                }
            } catch (e: Exception) {
                LOG.warn("Error setting IP or reading lights: ${e.message}")
            }
        }
    }

    // read the list of lights from the bridge
    private fun fillLights() {
        if (!ipField.text.isNullOrBlank()) {
            LOG.warn("Reading list of lights")
            CoroutineScope(Dispatchers.Default).launch {
                // remove and re-add action listeners so they don't trigger while populating the list
                val actionListeners = lightList.actionListeners
                actionListeners.forEach { lightList.removeActionListener(it) }
                shade.lights.getLights().forEach { (id, light) -> lightList.addItem(LightItem(id, light.name)) }
                lightList.selectedItem = LightItem(configState?.hueLightId!!, configState?.hueLightName!!)
                actionListeners.forEach { lightList.addActionListener(it) }
            }
        }
    }

    // Discover Hue bridge on network (called when "Discover" button is pressed)
    private fun discover() {
        runBlocking {
            LOG.warn("Trying to discover Hue bridge")
            val devices = shade.discovery.getDevices()
            when (devices.size) {
                0 -> labelPressButton.text = "No devices found!"
                1 -> {
                    LOG.warn("Found device ${devices.first().id} at IP ${devices.first().ip}")
                    ipField.text = devices.first().ip
                }
                else -> labelPressButton.text = "More than 1 device found. Please enter the IP manually"
            }
        }
    }

    // Disable or enable all controls depending on the checkbox state
    private fun disableEnableControls(event: ActionEvent, component: JPanel) {
        val checkBox = event.source as JCheckBox
        loopComponents(component as JComponent, checkBox.isSelected)
        checkBox.isEnabled = true
    }

    // loop recursively through all components to enable or disable
    private fun loopComponents(component: JComponent, selected: Boolean) {
        component.components.forEach {
            it.isEnabled = selected
            if (it is JComponent) {
                loopComponents(it as JComponent, selected)
            }
        }
    }

    // flash lights when selected in the dropdown
    private fun flashLight(event: ActionEvent) {
        val combo = event.source as ComboBox<*>
        val (id, name) = combo.selectedItem as LightItem
        CoroutineScope(Dispatchers.Default).launch {
            val currentState = shade.lights.getLight(id).state
            LOG.warn("State of light $id : $name is ${currentState.on}")
            shade.lights.setState(id, LightStateModification(on = false))
            delay(FLASH_DELAY)
            shade.lights.setState(id, LightStateModification(on = true))
            delay(FLASH_DELAY)
            shade.lights.setState(id, LightStateModification(on = currentState.on))
        }
    }

    override fun isModified(): Boolean {
        return true
    }

    override fun apply() {
        configState?.hueEnabled = enableCheck.isSelected
        configState?.hueIp = ipField.text ?: ""
        lightList.selectedItem?.let {
            val selectedLight = it as LightItem
            configState?.hueLightId = selectedLight.id
            configState?.hueLightName = selectedLight.name
        }
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
