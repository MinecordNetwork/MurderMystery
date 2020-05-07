package net.minecord.murdermystery.game.sidebar

import net.minecord.gamesys.game.GameStatus
import net.minecord.gamesys.game.player.GamePlayer
import net.minecord.gamesys.game.sidebar.GameSidebar
import net.minecord.gamesys.utils.getMsgString
import net.minecord.gamesys.utils.runTaskTimerAsynchronously
import net.minecord.murdermystery.MurderMystery
import net.minecord.murdermystery.game.MurderMysteryGame
import net.minecord.murdermystery.game.player.MurderMysteryPlayer
import org.bukkit.scheduler.BukkitTask
import java.util.*

class MurderMysterySidebar(override val plugin: MurderMystery, override val game: MurderMysteryGame) : GameSidebar(plugin, game) {
    private val animatedBoard: BukkitTask
    private val currentScroll = mutableListOf<String>()
    private val scrollPlayers = 5
    private var currentIndex = 0

    init {
        animatedBoard = plugin.runTaskTimerAsynchronously({
            scrollPlayers()
        }, 0, 20)
    }

    override fun addPlayer(player: GamePlayer) {
        if (currentScroll.size < scrollPlayers) currentScroll.add(player.player.name)
        super.addPlayer(player)
    }

    override fun removePlayer(player: GamePlayer) {
        if (currentScroll.contains(player.player.name)) currentScroll.remove(player.player.name)
        super.removePlayer(player)
    }

    private fun scrollPlayers() {
        if (game.status == GameStatus.RUNNING || game.status == GameStatus.ENDING) {
            animatedBoard.cancel()
            return
        }
        if (game.players.size <= scrollPlayers) return
        currentScroll.clear()
        val players: List<GamePlayer> = game.players
        var i = 0
        while (i < scrollPlayers) {
            if (players.size <= currentIndex + i) {
                if (currentIndex >= players.size) currentIndex = 0
                var tmp = 0
                while (i < scrollPlayers) {
                    currentScroll.add(players[currentIndex + tmp].player.name)
                    i++
                    tmp++
                }
                update()
                return
            }
            currentScroll.add(players[currentIndex + i].player.name)
            i++
        }
        update()
        currentIndex++
    }

    override fun getTitle(player: GamePlayer): String {
        return plugin.getMsgString("scoreboard.game.title").replace("%arena%", game.arena.name)
    }

    override fun getLines(player: GamePlayer): HashMap<String, Int> {
        val list = super.getLines(player)

        val model: String = plugin.getMsgString("scoreboard.game.model")
        val separator: String = plugin.getMsgString("scoreboard.game.separator")

        if (game.status == GameStatus.WAITING || game.status == GameStatus.STARTING) {
            list["&7----------------"] = 23

            var limit = 11
            val playerList = game.players
            playerList.filter { limit-- > 0 }.forEach {
                list["&a${it.player.name}"] = 0
            }

            list["&7&7&7----------------"] = -1
            list["&bIP: &fmc.minecord.net"] = -2
            list["&7&7&7&7----------------"] = -3

        } else {
            val detective = game.detective
            val gamePlayer = player as MurderMysteryPlayer

            list["&r&r$separator"] = 8
            list[model.replace("%key%", plugin.getMsgString("scoreboard.game.string.role")).replace("%value%", gamePlayer.role.toString().toLowerCase().capitalize())] = 7
            list["&e$separator"] = 6
            list[model.replace("%key%", plugin.getMsgString("scoreboard.game.string.innocents-left")).replace("%value%", game.getInnocents().size.toString())] = 5
            list[model.replace("%key%", plugin.getMsgString("scoreboard.game.string.detective")).replace("%value%", if (!detective.isAlive()) "&cDead" else "&aAlive")] = 4
            //TODO: Add score
            //list[model.replace("%key%", plugin.getMsgString("scoreboard.game.string.score")).replace("%value%", mysteryPlayer.getCurrentScore().toString() + "")] = 2
            list["&r$separator"] = 1
            list[plugin.getMsgString("scoreboard.game.string.last-line")] = 0
            list["&r&b$separator"] = -1
        }
        return list
    }

    override fun destroy() {
        super.destroy()

        animatedBoard.cancel()
    }
}
