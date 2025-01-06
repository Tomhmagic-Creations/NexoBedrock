package com.portalgg.nexobedrock

import org.bukkit.entity.Player
import org.geysermc.geyser.api.GeyserApi

class GeyserHandler : BedrockHandler {

    override fun isBedrockPlayer(player: Player): Boolean {
        return GeyserApi.api().isBedrockPlayer(player.uniqueId)
    }
}