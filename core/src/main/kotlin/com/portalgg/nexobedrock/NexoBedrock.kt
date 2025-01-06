package com.portalgg.nexobedrock

import com.nexomc.nexo.api.events.NexoItemsLoadedEvent
import com.nexomc.nexo.utils.PluginUtils
import com.nexomc.nexo.utils.logs.Logs
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin

class NexoBedrock : JavaPlugin() {

    lateinit var packetManager: IBedrockFurniturePacketManager
        private set
    lateinit var bedrockHandler: BedrockHandler
        private set
    //var debug = false
    //    private set
    //var offset = -0.6
    //    private set

    override fun onLoad() {
        INSTANCE = this
    }

    companion object {
        private lateinit var INSTANCE: NexoBedrock

        fun instance(): NexoBedrock = INSTANCE
    }

    override fun onEnable() {
        saveDefaultConfig()
        packetManager = IBedrockFurniturePacketManager.manager()

        /*Bukkit.getPluginManager().registerEvents(object : Listener {
            @EventHandler
            fun NexoItemsLoadedEvent.onLoaded() {
                reloadConfig()
                debug = config.getBoolean("debug")
                offset = config.getDouble("offset")
            }
        }, this)*/

        when {
            PluginUtils.isEnabled("floodgate") -> bedrockHandler = FloodgateHandler()
            PluginUtils.isEnabled("Geyser-Spigot") -> bedrockHandler = GeyserHandler()
            else -> {
                bedrockHandler = BedrockHandler.EmptyBedrockHandler()
                Logs.newline()
                Logs.logError("--------------------------------------------------------")
                Logs.logError("Could not find either Floodgate or Geyser-Spigot plugin!")
                Logs.logError("Furniture-packets will not be sent correctly!")
                Logs.logError("--------------------------------------------------------")
                Logs.newline()
            }
        }
        Bukkit.getPluginManager().registerEvents(NexoBedrockFurnitureListener(), this)
    }
}
