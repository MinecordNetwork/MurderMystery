package net.minecord.murdermystery.event

import net.minecord.murdermystery.game.player.MurderMysteryPlayer
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class BowPickupEvent(val murderMysteryPlayer: MurderMysteryPlayer, isAsync: Boolean) : Event(isAsync) {
    private val HANDLERS = HandlerList()

    override fun getHandlers(): HandlerList {
        return HANDLERS
    }

    fun getHandlerList(): HandlerList? {
        return HANDLERS
    }
}
