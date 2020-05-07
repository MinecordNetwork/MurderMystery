package net.minecord.murdermystery.game.player

import net.minecord.gamesys.game.GameStatus
import net.minecord.gamesys.game.player.GamePlayerStatus
import net.minecord.gamesys.utils.getCfgInt
import net.minecord.gamesys.utils.runTaskAsynchronously
import net.minecord.murdermystery.MurderMystery
import net.minecord.murdermystery.event.GoldPickupEvent
import net.minecord.murdermystery.game.MurderMysteryGame
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.*
import org.bukkit.event.player.*
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class MurderMysteryPlayerListener(private val plugin: MurderMystery): Listener {
    @EventHandler
    fun onGoldPickup(e: EntityPickupItemEvent) {
        if (e.entity !is Player) return

        val player = plugin.gamePlayerManager.get(e.entity as Player) as MurderMysteryPlayer

        if (player.status == GamePlayerStatus.PLAYING && e.item.itemStack.type == plugin.system.goldMineral.type) {
            plugin.runTaskAsynchronously {
                (player.game as MurderMysteryGame?)?.let {
                    val playerGolds: Int = player.getGoldCount()
                    if (playerGolds >= 64)
                        return@runTaskAsynchronously

                    val item = e.item
                    val inventory: Inventory = player.player.inventory
                    it.onGoldPickup(item)
                    item.remove()

                    val currentGolds = inventory.getItem(8)
                    if (currentGolds != null && currentGolds.type == plugin.system.goldMineral.type) {
                        currentGolds.amount = currentGolds.amount + 1
                        inventory.setItem(8, currentGolds)
                    } else inventory.setItem(8, plugin.system.goldMineral)

                    player.player.world.playSound(player.player.location, Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.7f)

                    //val score: Int = plugin.getCfgInt("score.per-gold-pickup")
                    //TODO: Add player score with message "game.score-causes.gold-pickup"
                    Bukkit.getPluginManager().callEvent(GoldPickupEvent(player, true))

                    val keeper = it.bow.keeper
                    if (player.getGoldCount() == 10 && !(keeper != null && keeper == player)) {
                        player.removeGolds(10)
                        val bow = ItemStack(Material.BOW)
                        val arrow = ItemStack(Material.ARROW)
                        if (player.role == MurderMysteryPlayerRole.MURDERER) {
                            if (!inventory.contains(bow)) inventory.setItem(1, bow)
                            val checkStack = inventory.getItem(2)
                            if (checkStack != null && checkStack.type == Material.ARROW) arrow.amount = checkStack.amount + 1
                            inventory.setItem(2, arrow)
                        } else {
                            if (!inventory.contains(bow)) inventory.setItem(0, bow)
                            val checkStack = inventory.getItem(1)
                            if (checkStack != null && checkStack.type == Material.ARROW) arrow.amount = checkStack.amount + 1
                            inventory.setItem(1, arrow)
                        }
                    }
                }
            }
            e.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPrivateMessageSend(e: PlayerCommandPreprocessEvent) {
        val player = plugin.gamePlayerManager.get(e.player)
        if (player.status == GamePlayerStatus.SPECTATING) {
            val cmd = e.message.split(" ").toTypedArray()[0].replace("/", "")
            if (cmd == "r" || cmd == "t" || cmd == "er" || cmd == "reply" || cmd == "ereply" || cmd == "msg" || cmd == "tell" || cmd == "emsg" || cmd == "etell" || cmd == "pm" || cmd == "epm") {
                player.player.sendMessage("You are spectator, private messages are disabled.")
                e.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onGoldMerge(e: ItemMergeEvent) {
        if (e.entity.itemStack.type == plugin.system.goldMineral.type && e.entity.world == plugin.worldManager.bukkitWorld) e.isCancelled = true
    }

    @EventHandler
    fun onMurderKill(e: EntityDamageByEntityEvent) {
        if (e.damager !is Player || e.entity !is Player) return

        val attacker = plugin.gamePlayerManager.get(e.damager as Player) as MurderMysteryPlayer
        val victim = plugin.gamePlayerManager.get(e.entity as Player) as MurderMysteryPlayer

        attacker.game?.let {
            if (it.status == GameStatus.RUNNING && attacker.role == MurderMysteryPlayerRole.MURDERER) {
                val handItem = attacker.player.inventory.itemInMainHand
                if (handItem.type == Material.IRON_SWORD) {
                    it.onPlayerDeath(victim, e.cause)
                    return
                }
            }
            e.isCancelled = true
        }
    }

    @EventHandler
    fun onBowShot(e: EntityShootBowEvent) {
        if (e.entity !is Player) return
        val shooter = plugin.gamePlayerManager.get(e.entity as Player) as MurderMysteryPlayer

        (shooter.game as MurderMysteryGame?)?.let {
            if (shooter == it.bow.keeper) {
                shooter.player.inventory.setItem(1, ItemStack(Material.AIR))
                it.bow.reloadBow()
            }
        }
    }

    @EventHandler
    fun onShot(e: ProjectileLaunchEvent) {
        val entity = e.entity

        if (entity is Arrow) {
            entity.pickupStatus = AbstractArrow.PickupStatus.DISALLOWED
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onSelfHit(e: EntityDamageByEntityEvent) {
        if (e.entity !is Player) return

        val victim = plugin.gamePlayerManager.get(e.entity as Player)
        if (victim.game == null) return

        if (victim.game!!.status != GameStatus.WAITING && victim.game!!.status != GameStatus.STARTING) {
            val damagedBy = e.damager
            if (damagedBy is Projectile && damagedBy.shooter is Player) {
                val shooter = damagedBy.shooter as Player
                if (victim.player == shooter) {
                    e.isCancelled = true
                    return
                }
            }
        }
    }

    @EventHandler
    fun onBowHit(e: ProjectileHitEvent) {
        val projectile = e.entity as? Arrow ?: return
        if (projectile.shooter !is Player) return

        val player = plugin.gamePlayerManager.get(projectile.shooter as Player) as MurderMysteryPlayer
        (player.game as MurderMysteryGame?)?.let {
            if (it.status != GameStatus.RUNNING) return
            if (player.player.inventory.itemInMainHand.type != Material.BOW) return

            if (player == it.bow.keeper) {
                player.player.inventory.setItem(1, ItemStack(Material.AIR))
                it.bow.reloadBow()
            }

            if (e.hitEntity != null && e.hitEntity is Player) {
                val victim = plugin.gamePlayerManager.get(e.hitEntity as Player) as MurderMysteryPlayer
                if (victim.role != MurderMysteryPlayerRole.MURDERER && victim != player) {
                    if (player.role != MurderMysteryPlayerRole.MURDERER) {
                        it.onPlayerDeath(player, EntityDamageEvent.DamageCause.PROJECTILE)
                        //val score: Int = plugin.getCfgInt("score.per-wrong-kill")
                        //TODO: Add bad score to player with "game.score-causes.wrong-kill" message
                    }
                }
                victim.onAttack(player, EntityDamageEvent.DamageCause.PROJECTILE)
                if (victim == player)
                    it.onPlayerDeath(victim, EntityDamageEvent.DamageCause.SUICIDE)
                else
                    it.onPlayerDeath(victim, EntityDamageEvent.DamageCause.PROJECTILE)
            }
        }

        projectile.remove()
    }

    @EventHandler
    fun onDamage(e: EntityDamageByEntityEvent) {
        if (e.damager is Firework) {
            e.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onChat(e: AsyncPlayerChatEvent) {
        val player = plugin.gamePlayerManager.get(e.player)

        player.game?.let {
            if (player.status == GamePlayerStatus.SPECTATING) {
                e.isCancelled = true
            }
        }
    }
}
