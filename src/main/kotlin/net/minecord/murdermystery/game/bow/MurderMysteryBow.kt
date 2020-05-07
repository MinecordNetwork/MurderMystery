package net.minecord.murderMurderMystery.game.bow

import net.minecord.gamesys.utils.getCfgInt
import net.minecord.gamesys.utils.getMsgString
import net.minecord.gamesys.utils.runTask
import net.minecord.murdermystery.MurderMystery
import net.minecord.murdermystery.event.BowPickupEvent
import net.minecord.murdermystery.game.MurderMysteryGame
import net.minecord.murdermystery.game.player.MurderMysteryPlayer
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.craftbukkit.v1_15_R1.boss.CraftBossBar
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask

class MurderMysteryBow(private val plugin: MurderMystery, private val game: MurderMysteryGame, detective: MurderMysteryPlayer) {
    private val bow = ItemStack(Material.BOW)
    private val reloadBar = CraftBossBar(plugin.getMsgString("game.bow-reloading"), BarColor.GREEN, BarStyle.SEGMENTED_6)
    private var droppedBow: ArmorStand? = null
    private var bowReloading: BukkitTask? = null
    private var dropRotation: BukkitTask? = null
    private var pickupChecking: BukkitTask? = null
    var keeper: MurderMysteryPlayer? = null

    init {
        keeper = detective
        val meta = bow.itemMeta
        meta!!.setDisplayName(plugin.getMsgString("game.items.detective-bow"))
        bow.itemMeta = meta
        reloadBar.addPlayer(detective.player)
        reloadBar.progress = 0.0
        reloadBar.setTitle(plugin.getMsgString("game.bow-reloading"))
        reloadBar.isVisible = false
    }

    fun dropBow() {
        val currentKeeper = keeper ?: return
        cancelReloading()
        val dropLocation = currentKeeper.player.location
        game.players.forEach {
            it.player.sendTitle("", plugin.getMsgString("game.bow-was-dropped-title"), 0, 50, 10)
        }

        object : BukkitRunnable() {
            override fun run() {
                droppedBow = dropLocation.world!!.spawnEntity(dropLocation, EntityType.ARMOR_STAND) as ArmorStand
                droppedBow!!.isVisible = false
                droppedBow!!.setItemInHand(ItemStack(Material.BOW))
                droppedBow!!.isCollidable = false

                dropRotation = object : BukkitRunnable() {
                    var counter = 0
                    override fun run() {
                        counter += 1
                        val newLocation = droppedBow!!.location.clone()
                        var newYaw = newLocation.yaw + 4
                        if (newYaw >= 180) {
                            newYaw = -179f
                            counter = -179
                        }
                        newLocation.yaw = newYaw
                        droppedBow!!.teleport(newLocation)
                    }
                }.runTaskTimer(plugin, 0, 1)

                pickupChecking = object : BukkitRunnable() {
                    var dropLoc = droppedBow!!.location
                    override fun run() {
                        game.getInnocents().forEach {
                            val l: Location = it.player.location
                            if (l.x + 1 >= dropLoc.x && l.x - 1 <= dropLoc.x) {
                                if (l.z + 1 >= dropLoc.z && l.z - 1 <= dropLoc.z) {
                                    if (l.y + 1.5 >= dropLoc.y && l.y - 1.5 <= dropLoc.y) {
                                        pickupBow(it as MurderMysteryPlayer)
                                    }
                                }
                            }
                        }
                    }
                }.runTaskTimerAsynchronously(plugin, 5, 4)
                giveLocators()
            }
        }.runTask(plugin)

        keeper = null
    }

    private fun pickupBow(player: MurderMysteryPlayer) {
        keeper = player
        reloadBar.addPlayer(player.player)
        pickupChecking?.cancel()
        dropRotation?.cancel()

        plugin.runTask {
            droppedBow!!.remove()
            droppedBow = null
        }

        player.player.world.playSound(player.player.location, Sound.ENTITY_ITEM_PICKUP, 0.5f, 0.7f)
        game.players.forEach { it.player.sendTitle("", plugin.getMsgString("game.bow-was-taken-title"), 0, 50, 10) }
        takeLocators()
        giveBow()

        Bukkit.getPluginManager().callEvent(BowPickupEvent(player, true))
    }

    fun giveBow() {
        keeper?.apply {
            player.inventory.setItem(0, bow)
            player.inventory.setItem(1, ItemStack(Material.ARROW))
        }
    }

    fun reloadBow() {
        val max: Int = plugin.getCfgInt("game.detectiveBowReload")
        reloadBar.progress = 0.0
        reloadBar.isVisible = true
        bowReloading = object : BukkitRunnable() {
            var counter = 0
            override fun run() {
                if (keeper == null) {
                    cancel()
                    return
                } else if (counter >= max) {
                    reloadBar.isVisible = false
                    keeper?.apply {
                        player.inventory.setItem(1, ItemStack(Material.ARROW))
                        player.playSound(player.location, Sound.ITEM_ARMOR_EQUIP_ELYTRA, 1f, 1.3f)
                    }
                    cancel()
                    return
                }
                counter++
                reloadBar.progress = counter.toDouble() / max
            }
        }.runTaskTimerAsynchronously(plugin, 0, 20)
    }

    private fun cancelReloading() {
        bowReloading?.cancel()
        reloadBar.isVisible = false
        reloadBar.removeAll()
    }

    fun giveLocators() {
        val locator = ItemStack(Material.COMPASS)
        val meta = locator.itemMeta
        meta?.apply { setDisplayName(plugin.getMsgString("game.items.bow-locator")) }
        locator.itemMeta = meta
        game.getInnocents().forEach {
            it.player.inventory.setItem(4, locator)
            it.player.compassTarget = droppedBow!!.location
        }
    }

    private fun takeLocators() {
        game.getInnocents().forEach { it.player.inventory.setItem(4, ItemStack(Material.AIR)) }
    }

    fun getDroppedBow(): ArmorStand? {
        return droppedBow
    }

    fun destroy() {
        if (droppedBow != null) {
            plugin.runTask {
                droppedBow!!.remove()
                droppedBow = null
            }
        }
        cancelReloading()
        dropRotation?.cancel()
        pickupChecking?.cancel()
        bowReloading?.cancel()
    }
}
