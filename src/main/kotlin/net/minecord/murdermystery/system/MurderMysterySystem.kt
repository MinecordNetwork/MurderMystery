package net.minecord.murdermystery.system

import net.minecord.gamesys.Gamesys
import net.minecord.gamesys.arena.Arena
import net.minecord.gamesys.game.Game
import net.minecord.gamesys.game.player.GamePlayer
import net.minecord.gamesys.game.sidebar.GameSidebar
import net.minecord.gamesys.system.BaseSystem
import net.minecord.gamesys.utils.ItemBuilder
import net.minecord.gamesys.utils.getMsgString
import net.minecord.murdermystery.MurderMystery
import net.minecord.murdermystery.game.MurderMysteryGame
import net.minecord.murdermystery.game.player.MurderMysteryPlayer
import net.minecord.murdermystery.game.sidebar.MurderMysterySidebar
import org.bukkit.Material
import org.bukkit.entity.Player

class MurderMysterySystem(plugin: Gamesys) : BaseSystem(plugin) {
    val goldMineral = ItemBuilder(Material.PLAYER_HEAD).data(3)
            .skullTexture(plugin.getMsgString("game.items.gold-mineral.skull-texture"))
            .name(plugin.getMsgString("game.items.gold-mineral.name")).make()

    override fun createGame(plugin: Gamesys, arena: Arena): Game {
        return MurderMysteryGame(plugin as MurderMystery, arena)
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

    override fun getArenaBlockMapping(): HashMap<String, Material> {
        val map = hashMapOf<String, Material>()

        map["spawns"] = Material.WHITE_GLAZED_TERRACOTTA
        map["golds"] = Material.YELLOW_GLAZED_TERRACOTTA

        return map
    }

    fun getGoldSpawnInterval(): Int {
        return 50
    }
}
