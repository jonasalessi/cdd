package com.cdd.settings

import com.intellij.util.messages.Topic

interface IcpSettingsListener {
    fun handleAutoCalculateOnSaveChanged(isEnabled: Boolean)

    companion object {
        val TOPIC = Topic.create(
            "icpSettingsChanged",
            IcpSettingsListener::class.java
        )
    }
}
