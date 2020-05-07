package net.minecord.murdermystery.game.player

import net.minecord.gamesys.game.GameStatus
import net.minecord.gamesys.game.player.GamePlayer
import net.minecord.gamesys.utils.chat.colored
import net.minecord.gamesys.utils.getMsgString
import net.minecord.murdermystery.MurderMystery
import org.bukkit.entity.Player

class MurderMysteryPlayer(override val plugin: MurderMystery, player: Player): GamePlayer(plugin, player) {
    var role = MurderMysteryPlayerRole.INNOCENT

    fun getGoldCount(): Int {
        var golds = 0
        player.inventory.forEach { itemStack -> if (itemStack != null && itemStack.type == plugin.system.goldMineral.type) golds += itemStack.amount }
        return golds
    }

    fun removeGolds(number: Int) {
        for (i in number downTo 1) {
            player.inventory.forEach { itemStack -> if (itemStack != null && itemStack.type == plugin.system.goldMineral.type) itemStack.amount = itemStack.amount - 1 }
        }
    }

    override fun onChat(message: String): Boolean {
        if (game != null) {
            val currentGame = game!!

            if (currentGame.status == GameStatus.RUNNING) {
                if (isAlive()) {
                    currentGame.players.forEach {
                        it.player.sendMessage(plugin.getMsgString("chat.game.alive").replace("%name%", this.player.name).replace("%displayName%", this.player.displayName).replace("%score%", "ScoreString").colored().replace("%message%", message))
                    }

                } else {
                    sendMessageToOtherPlayers(message, "chat.game.dead")
                }

            } else {
                sendMessageToOtherPlayers(message, "chat.game.alive")
            }

        } else {
            sendMessageToOtherPlayers(message, "chat.lobby")
        }

        return true
    }

    private fun sendMessageToOtherPlayers(message: String, chat: String) {
        plugin.gamePlayerManager.players.values.forEach {
            val playerGame = it.game

            if (!it.isAlive() || playerGame == null || playerGame.status == GameStatus.WAITING || playerGame.status == GameStatus.STARTING || playerGame.status == GameStatus.ENDING) {
                it.player.sendMessage(plugin.getMsgString(chat).replace("%name%", this.player.name).replace("%displayName%", this.player.displayName).replace("%score%", "ScoreString").colored().replace("%message%", message))
            }
        }
    }
}
