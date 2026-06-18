package de.timonso.tprandom.client

import com.mojang.brigadier.Command
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

class TprandomClient : ClientModInitializer {

    override fun onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                ClientCommands.literal("randomteleport")
                    .executes { context ->
                        val client = Minecraft.getInstance()
                        val connection = client.connection
                        val self = client.player
                        if (connection == null || self == null) {
                            return@executes 0
                        }

                        val selfName = self.gameProfile.name
                        val targets = connection.onlinePlayers
                            .map { it.profile.name }
                            .filter { it != selfName }

                        if (targets.isEmpty()) {
                            context.source.sendError(
                                Component.literal("No other players are online to teleport to.")
                            )
                            return@executes 0
                        }

                        val target = targets.random()
                        connection.sendCommand("tp $target")

                        context.source.sendFeedback(
                            Component.literal("Randomly teleporting to $target.")
                        )
                        Command.SINGLE_SUCCESS
                    }
            )
        }
    }
}
