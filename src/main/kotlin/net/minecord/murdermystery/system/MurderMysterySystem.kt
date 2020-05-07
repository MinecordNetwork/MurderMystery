package net.minecord.murdermystery.system

import net.minecord.gamesys.Gamesys
import net.minecord.gamesys.arena.Arena
import net.minecord.gamesys.game.Game
import net.minecord.gamesys.game.player.GamePlayer
import net.minecord.gamesys.game.sidebar.GameSidebar
import net.minecord.gamesys.system.DefaultSystem
import net.minecord.gamesys.utils.ItemBuilder
import net.minecord.gamesys.utils.getCfgInt
import net.minecord.gamesys.utils.getMsgString
import net.minecord.murdermystery.MurderMystery
import net.minecord.murdermystery.game.MurderMysteryGame
import net.minecord.murdermystery.game.player.MurderMysteryPlayer
import net.minecord.murdermystery.game.sidebar.MurderMysterySidebar
import org.bukkit.Material
import org.bukkit.entity.Player

class MurderMysterySystem(plugin: Gamesys) : DefaultSystem(plugin) {
    val goldMineral = ItemBuilder(Material.PLAYER_HEAD).data(3)
            .skullTexture(plugin.getMsgString("game.items.gold-mineral.skull-texture"))
            .name(plugin.getMsgString("game.items.gold-mineral.name")).make()

    override fun createGame(plugin: Gamesys, arena: Arena): Game {
        return MurderMysteryGame(plugin as MurderMystery, arena)
    }

    override fun createGamePlayer(plugin: Gamesys, player: Player): GamePlayer {
        return MurderMysteryPlayer(plugin as MurderMystery, player)
    }

    override fun createGameSidebar(plugin: Gamesys, game: Game): GameSidebar {
        return MurderMysterySidebar(plugin as MurderMystery, game as MurderMysteryGame)
    }

    override fun getChatPrefix(): String {
        return plugin.getMsgString("prefix")
    }

    override fun getArenaBlockMapping(): HashMap<String, Material> {
        val map = hashMapOf<String, Material>()

        map["spawns"] = Material.WHITE_GLAZED_TERRACOTTA
        map["golds"] = Material.YELLOW_GLAZED_TERRACOTTA

        return map
    }

    fun getGoldSpawnInterval(): Int {
        return plugin.getCfgInt("game.goldSpawnInterval")
    }
}
