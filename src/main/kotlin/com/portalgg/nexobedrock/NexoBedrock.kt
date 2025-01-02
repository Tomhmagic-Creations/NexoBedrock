package com.portalgg.nexobedrock

import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.api.events.NexoMechanicsRegisteredEvent
import com.nexomc.nexo.mechanics.MechanicsManager
import com.nexomc.nexo.utils.logs.Logs
import com.portalgg.nexobedrock.mechanics.furniture.BedrockFurnitureFactory
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin

class NexoBedrock : JavaPlugin() {

    override fun onLoad() {
        INSTANCE = this
    }

    companion object {
        private lateinit var INSTANCE: NexoBedrock

        fun instance(): NexoBedrock = INSTANCE
    }

    override fun onEnable() {
        saveDefaultConfig()
        Bukkit.getPluginManager().registerEvents(object : Listener {
            @EventHandler
            fun NexoMechanicsRegisteredEvent.mechanics() {
                reloadConfig()
                MechanicsManager.registerMechanicFactory(BedrockFurnitureFactory(), true)
                Logs.logInfo("Registered <i>\"bedrock_furntiure\" addon-mechanic")
            }
        }, this)
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}
