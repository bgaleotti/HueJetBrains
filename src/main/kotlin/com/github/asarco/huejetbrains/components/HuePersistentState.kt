package com.github.asarco.huejetbrains.components

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(
    name = "HueStateComponent",
    storages = [Storage("hue-plugin.xml")]
)

open class HuePersistentState : PersistentStateComponent<HuePersistentState.HueState> {

    // this is how we're going to call the component from different classes
    companion object {
        val instance: HuePersistentState
            get() = ServiceManager.getService(HuePersistentState::class.java)
    }

    // the component will always keep our state as a variable
    var hueState: HueState = HueState()

    override fun getState(): HueState? {
        return hueState
    }

    override fun loadState(state: HueState) {
        hueState = state
    }

    class HueState {
        var hueEnabled = true
        var hueIp = ""
        var hueToken = ""
        var hueLightId = ""
        var hueLightName = ""
    }
}
