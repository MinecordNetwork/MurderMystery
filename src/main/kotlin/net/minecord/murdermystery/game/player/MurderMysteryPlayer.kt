package net.minecord.murdermystery.game.player

import net.minecord.gamesys.Gamesys
import net.minecord.gamesys.game.player.GamePlayer
import org.bukkit.entity.Player

class MurderMysteryPlayer(plugin: Gamesys, player: Player): GamePlayer(plugin, player) {
    var role = MurderMysteryPlayerRole.INNOCENT
}
