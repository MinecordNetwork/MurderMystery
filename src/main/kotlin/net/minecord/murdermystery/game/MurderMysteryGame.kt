package net.minecord.murdermystery.game

import com.sk89q.worldedit.EditSession
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.world.block.BlockTypes
import net.minecord.gamesys.arena.Arena
import net.minecord.gamesys.game.Game
import net.minecord.gamesys.game.GameStatus
import net.minecord.gamesys.game.player.GamePlayer
import net.minecord.gamesys.game.player.GamePlayerStatus
import net.minecord.gamesys.utils.*
import net.minecord.murderMurderMystery.game.bow.MurderMysteryBow
import net.minecord.murdermystery.MurderMystery
import net.minecord.murdermystery.game.player.MurderMysteryPlayer
import net.minecord.murdermystery.game.player.MurderMysteryPlayerRole
import org.bukkit.*
import org.bukkit.boss.BarColor
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer

class MurderMysteryGame(override val plugin: MurderMystery, override val arena: Arena) : Game(plugin, arena) {
    lateinit var murderer: MurderMysteryPlayer
    lateinit var detective: MurderMysteryPlayer
    private var hero: MurderMysteryPlayer? = null
    lateinit var bow: MurderMysteryBow
    private val spawnedGold = ConcurrentHashMap<Location, Item>()
    private val pickedGoldTime = HashMap<Location, Long>()
    private var goldMineralIterator = 0
    private var aliveRewardTask: BukkitTask? = null
    private var goldSpawningTask: BukkitTask? = null
    private var gameCountdownTask: BukkitTask? = null

    override fun onArenaLoaded(editSession: EditSession, origin: Location, lobbyLocation: Location) {
        super.onArenaLoaded(editSession, origin, lobbyLocation)

        getGoldLocations().forEach {
            editSession.setBlock(BlockVector3.at(it.blockX, it.blockY, it.blockZ), BlockTypes.AIR?.defaultState)
        }
    }

    override fun onPlayerLeft(player: GamePlayer) {
        super.onPlayerLeft(player)

        player as MurderMysteryPlayer

        player.role = MurderMysteryPlayerRole.INNOCENT

        if (status == GameStatus.RUNNING) {
            if (bow.keeper == player) {
                bow.dropBow()
            }
        }
    }

    override fun onGameStart() {
        super.onGameStart()

        players.forEach {
            it.player.sendTitle("&e&lGame Started".colored(), "&f&lSolve the murder mystery!".colored(), 0, 80, 20)
        }

        startGameCountdown()
        startGoldSpawning()

        murderer = players.random() as MurderMysteryPlayer
        murderer.role = MurderMysteryPlayerRole.MURDERER
        //TODO: Increase murderer stats

        detective = getInnocents().random() as MurderMysteryPlayer
        detective.role = MurderMysteryPlayerRole.DETECTIVE
        bow = MurderMysteryBow(plugin, this, detective)

        //TODO: Increase detective stats
        //TODO: Increase innocents stats

        val receiveSword: Int = plugin.getCfgInt("game.giveSwordToMurderer")
        object : BukkitRunnable() {
            var counter = receiveSword
            override fun run() {
                if (status != GameStatus.RUNNING) {
                    cancel()
                    return
                }
                if (counter <= 0) {
                    giveSwordToMurderer()
                    cancel()
                    return
                } else if (counter <= 5) players.forEach {
                    it.player.sendTitle(ChatColor.YELLOW.toString() + "" + ChatColor.BOLD + counter, plugin.getMsgString("game.sword.receive-title"), 0, 25, 10)
                    it.player.playSound(it.player.location, Sound.UI_BUTTON_CLICK, 10f, 1f)
                }
                counter--
            }
        }.runTaskTimerAsynchronously(plugin, 0, 20)

        val receivePlayerLocator: Int = plugin.getCfgInt("game.givePlayerLocator")
        object : BukkitRunnable() {
            var counter = receivePlayerLocator
            override fun run() {
                if (status != GameStatus.RUNNING) {
                    cancel()
                    return
                }
                if (counter <= 0) {
                    giveLocatorToMurderer()
                    cancel()
                    return
                } else if (counter <= 5) players.forEach {
                    it.player.sendTitle(ChatColor.YELLOW.toString() + "" + ChatColor.BOLD + counter, plugin.getMsgString("game.locator.receive-title"), 0, 25, 10)
                    it.player.playSound(it.player.location, Sound.UI_BUTTON_CLICK, 10f, 1f)
                }
                counter--
            }
        }.runTaskTimerAsynchronously(plugin, 0, 20)

        aliveRewardTask = object : BukkitRunnable() {
            override fun run() {
                val score: Int = plugin.getCfgInt("score.per-30-sec-alive")
                //TODO: Foreach innocents and add score with message "game.score-causes.30-sec-alive"
            }
        }.runTaskTimerAsynchronously(plugin, 600, 600)

        plugin.runTaskLaterAsynchronously({
            players.forEach {it as MurderMysteryPlayer
                it.player.sendTitle(plugin.getMsgString("game.start.you-are-${it.role.toString().toLowerCase()}-title"), "", 0, 60, 20)
            }
        }, 60)

        sidebar.update()
    }

    override fun onGameEnd(winner: GamePlayer?) {
        super.onGameEnd(winner)

        aliveRewardTask?.cancel()
        goldSpawningTask?.cancel()
        gameCountdownTask?.cancel()
        spawnedGold.values.forEach { it.remove() }
        spawnedGold.clear()
        bow.destroy()
    }

    override fun onPlayerDeath(player: GamePlayer, cause: EntityDamageEvent.DamageCause?, killer: GamePlayer?) {
        super.onPlayerDeath(player, cause, killer)

        player as MurderMysteryPlayer
        val murderKiller = killer as MurderMysteryPlayer?

        //TODO: Increase death stats
        //val deathScore: Int = plugin.getCfgInt("score.per-death")
        //TODO: Add death score

        if (murderKiller != null) {
            if (murderKiller.role == MurderMysteryPlayerRole.MURDERER || player.role == MurderMysteryPlayerRole.MURDERER) {
                //val score: Int = plugin.getCfgInt("score.per-kill")
                //TODO: Add score to killer
            }
            if (player.role == MurderMysteryPlayerRole.MURDERER) {
                hero = murderKiller
            }
            //TODO: Increase kill stats
        }

        instantFirework(FireworkEffect.builder().withColor(Color.RED).build(), player.player.location)

        if (bow.keeper == player) bow.dropBow()

        if (status == GameStatus.RUNNING) {
            if (getInnocents().isEmpty()) {
                onEndCountdownStart(murderer)
            } else if (!murderer.isAlive()) {
                onEndCountdownStart()
            }
        }
    }

    override fun onDeathMessageSent(player: GamePlayer, cause: EntityDamageEvent.DamageCause?, killer: GamePlayer?) {
        if (killer != null) {
            sendMessage(plugin.getMsgString("game.kill").replace("%victim%", player.player.name))
        } else {
            sendMessage(plugin.getMsgString("game.death").replace("%victim%", player.player.name))
        }
    }

    override fun onPlayerStartsToRespawn(player: GamePlayer, cause: EntityDamageEvent.DamageCause?, killer: GamePlayer?) {
        player.player.sendTitle(plugin.getMsgString("game.death-screen.title"), plugin.getMsgString("game.death-screen.subtitle"), 0, 60, 10)
        player.player.setItemOnCursor(null)
        player.player.inventory.clear()
        player.player.gameMode = GameMode.SPECTATOR
        player.status = GamePlayerStatus.SPECTATING
        plugin.runTaskAsynchronously {
            player.player.sendMessage(plugin.system.getChatPrefix() + " " + plugin.getMsgString("game.spectator-join"))
            player.player.sendMessage(plugin.system.getChatPrefix() + " " + plugin.getMsgString("game.spectator-chat-only"))
            player.player.playSound(player.player.location, Sound.ENTITY_ENDER_DRAGON_GROWL, 10f, 1f)
            sidebar.update()
        }
    }

    override fun onEndCountdownStart(winner: GamePlayer?) {
        super.onEndCountdownStart(winner)

        gameCountdownTask?.cancel()

        val scorePerWin: Int = plugin.getCfgInt("score.per-win")
        if (winner == null) {
            val heroName = hero?.player?.name ?: ""
            sendMessage(plugin.getMsgString("game.win.innocents-message").replace("%arena%", arena.name))
            bar.setTitle(plugin.getMsgString("game.win.innocents-title").replace("%arena%", arena.name))
            plugin.getMsgStringList("summary-innocents").forEach {
                sendMessage(it.replace("%murderer%", murderer.player.name).replace("%murdererScore%", "").replace("%detective%", detective.player.name).replace("%detectiveScore%", "").replace("%hero%", heroName).replace("%heroScore%", ""))
            }
        } else {
            Bukkit.broadcastMessage(plugin.system.getChatPrefix() + " " + plugin.getMsgString("game.win.murderer-message").replace("%player%", murderer.player.name).replace("%arena%", arena.name))
            bar.setTitle(plugin.getMsgString("game.win.murderer-title").replace("%player%", murderer.player.name).replace("%arena%", arena.name))
            plugin.getMsgStringList("summary-murderer").forEach {
                sendMessage(it.replace("%murderer%", murderer.player.name).replace("%murdererScore%", "").replace("%detective%", detective.player.name).replace("%detectiveScore%", ""))
            }
        }
        //TODO: Add score with message "game.score-causes.win" and increase win stats

        var countdown = getEndCountdown()
        object : BukkitRunnable() {
            override fun run() {
                when {
                    countdown <= 0 -> {
                        cancel()
                        return
                    }
                    else -> {
                        players.forEach {
                            it.player.playSound(it.player.location, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 10f, 1f)
                            if (winner != null) {
                                it.player.sendTitle("&c&l${countdown}".colored(), plugin.getMsgString("game.win.murderer-title").replace("%player%", winner.player.name).replace("%arena%", arena.name), 0, 60, 20)
                            } else {
                                it.player.sendTitle("&c&l${countdown}".colored(), plugin.getMsgString("game.win.innocents-title").replace("%arena%", arena.name), 0, 60, 20)
                            }
                        }
                        bar.progress = countdown.toDouble() / getEndCountdown().toDouble()
                    }
                }
                countdown--
            }
        }.runTaskTimerAsynchronously(plugin, 0, 20)
    }

    override fun getMinimumPlayers(): Int {
        return 3
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
        goldSpawningTask = object : BukkitRunnable() {
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
                            val newMineral: ItemStack = plugin.system.goldMineral
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

    private fun startGameCountdown() {
        val gameLength = plugin.getCfgInt("game.gameLength")

        bar.isVisible = true
        bar.color = BarColor.RED
        bar.setTitle(plugin.getMsgString("game.time-left").replace("%time%", gameLength.toMinutesString()))
        bar.progress = 1.0

        gameCountdownTask = object : BukkitRunnable() {
            var countdown = gameLength
            override fun run() {
                if (countdown <= 0) {
                    onEndCountdownStart()
                    cancel()
                    return
                }
                bar.setTitle(plugin.getMsgString("game.time-left").replace("%time%", countdown.toMinutesString()))
                bar.progress = countdown.toDouble() / gameLength.toDouble()
                countdown--
            }
        }.runTaskTimerAsynchronously(plugin, 0, 20)
    }

    fun giveSwordToMurderer() {
        murderer?.let {
            it.player.inventory.setItem(0, ItemStack(Material.IRON_SWORD))
            bow.giveBow()
            players.forEach {player ->
                player.player.sendTitle("", plugin.getMsgString("game.sword.received-title"), 0, 25, 10)
                player.player.playSound(it.player.location, Sound.ENTITY_ENDER_DRAGON_GROWL, 10f, 1f)
            }
        }
    }

    fun giveLocatorToMurderer() {
        val locator = ItemStack(Material.COMPASS)
        val meta = locator.itemMeta
        meta!!.setDisplayName(plugin.getMsgString("game.items.player-locator"))
        locator.itemMeta = meta

        murderer.let {
            it.player.inventory.setItem(4, locator)
            object : BukkitRunnable() {
                var closest = Double.MAX_VALUE
                override fun run() {
                    if (!it.player.isOnline || it.player.world != plugin.gamePortalManager.portal.location.world) {
                        cancel()
                        return
                    }
                    var closestPlayer: Player? = null
                    for (innocent in getInnocents()) {
                        val dist: Double = innocent.player.location.distance(it.player.location)
                        if (closest == Double.MAX_VALUE || dist < closest) {
                            closest = dist
                            closestPlayer = innocent.player
                        }
                    }
                    if (closestPlayer != null) it.player.compassTarget = closestPlayer.location
                }
            }.runTaskTimer(plugin, 0, 20)
        }

        players.forEach {
            it.player.sendTitle("", plugin.getMsgString("game.locator.received-title"), 0, 25, 10)
            it.player.playSound(it.player.location, Sound.ENTITY_ZOMBIE_DEATH, 10f, 1f)
        }
    }

    fun getInnocents(): List<GamePlayer> {
        return players.filter { (it as MurderMysteryPlayer).role in listOf(MurderMysteryPlayerRole.INNOCENT, MurderMysteryPlayerRole.DETECTIVE) && it.isAlive() }
    }

    private fun getGoldLocations(): MutableList<Location> {
        return locations["golds"]!!
    }
}
