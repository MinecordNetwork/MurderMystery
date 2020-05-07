package net.minecord.murdermystery.game.player

import net.minecord.gamesys.game.player.GamePlayer
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
}
