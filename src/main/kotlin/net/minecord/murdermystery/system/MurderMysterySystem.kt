package net.minecord.murdermystery.system

import net.minecord.gamesys.Gamesys
import net.minecord.gamesys.arena.Arena
import net.minecord.gamesys.game.Game
import net.minecord.gamesys.game.player.GamePlayer
import net.minecord.gamesys.game.sidebar.GameSidebar
import net.minecord.gamesys.system.BaseSystem
import net.minecord.murdermystery.game.MurderMysteryGame
import net.minecord.murdermystery.game.player.MurderMysteryPlayer
import net.minecord.murdermystery.game.sidebar.MurderMysterySidebar
import org.bukkit.entity.Player

class MurderMysterySystem(plugin: Gamesys) : BaseSystem(plugin) {
    override fun createGame(plugin: Gamesys, arena: Arena): Game {
        return MurderMysteryGame(plugin, arena)
    }

    override fun createGamePlayer(plugin: Gamesys, player: Player): GamePlayer {
        return MurderMysteryPlayer(plugin, player)
    }

    override fun createGameSidebar(plugin: Gamesys, game: Game): GameSidebar {
        return MurderMysterySidebar(plugin, game)
    }

    override fun getChatPrefix(): String {
        return "&c&lMurder &f&l●&7"
    }

    override fun dropItemsAfterDeath(): Boolean {
        return false
    }

    override fun isHungerBarDisabled(): Boolean {
        return true
    }

    override fun isItemThrowingAllowed(): Boolean {
        return false
    }
}
