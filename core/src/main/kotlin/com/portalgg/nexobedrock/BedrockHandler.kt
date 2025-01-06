package com.portalgg.nexobedrock

import org.bukkit.entity.Player

interface BedrockHandler {

    fun isBedrockPlayer(player: Player): Boolean {
        return false
    }

    class EmptyBedrockHandler: BedrockHandler {
        override fun isBedrockPlayer(player: Player): Boolean {
            return false
        }
    }
}