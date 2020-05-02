package net.minecord.murdermystery.game

import net.minecord.gamesys.arena.Arena
import net.minecord.gamesys.game.Game
import net.minecord.gamesys.game.GameStatus
import net.minecord.gamesys.utils.runTask
import net.minecord.murdermystery.MurderMystery
import org.bukkit.Location
import org.bukkit.entity.Item
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer

class MurderMysteryGame(override val plugin: MurderMystery, override val arena: Arena) : Game(plugin, arena) {
    private val spawnedGold = ConcurrentHashMap<Location, Item>()
    private val pickedGoldTime = HashMap<Location, Long>()
    private var goldMineralIterator = 0

    private fun getGoldLocations(): MutableList<Location> {
        return locations["golds"]!!
    }

    fun onGoldPickup(i: Item) {
        spawnedGold.forEach { (location: Location?, item: Item) ->
            if (item == i) {
                pickedGoldTime[location] = System.currentTimeMillis() / 1000
                spawnedGold.remove(location)
            }
        }
    }

    private fun startGoldSpawning() {
        object : BukkitRunnable() {
            override fun run() {
                if (status != GameStatus.RUNNING) {
                    cancel()
                    return
                }

                val currentTime = System.currentTimeMillis() / 1000
                val spawnLocations: MutableList<Location> = ArrayList()
                for (location in getGoldLocations()) {
                    if (!spawnedGold.containsKey(location)) {
                        if (pickedGoldTime.containsKey(location)) {
                            val lastSpawn: Long = pickedGoldTime[location]!!
                            if (lastSpawn + plugin.system.getGoldSpawnInterval() > currentTime) continue
                        }
                        spawnLocations.add(location)
                    }
                }

                if (spawnLocations.isNotEmpty()) {
                    plugin.runTask {
                        spawnLocations.forEach(Consumer { location: Location ->
                            goldMineralIterator++
                            val newMineral: ItemStack = plugin.system.getGoldMineral()
                            val mineralMeta = newMineral.itemMeta
                            val lore: MutableList<String> = ArrayList()
                            lore.add("Iterator - $goldMineralIterator")
                            mineralMeta!!.lore = lore
                            newMineral.itemMeta = mineralMeta
                            val item = location.world!!.dropItemNaturally(location, newMineral)
                            item.customName = newMineral.itemMeta!!.displayName
                            item.isCustomNameVisible = true
                            spawnedGold[location] = item
                        })
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0, 20)
    }
}
