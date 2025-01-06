package com.portalgg.nexobedrock

import org.bukkit.entity.Player
import org.geysermc.floodgate.api.FloodgateApi

class FloodgateHandler : BedrockHandler {

    override fun isBedrockPlayer(player: Player): Boolean {
        return FloodgateApi.getInstance().isFloodgatePlayer(player.uniqueId)
    }
}