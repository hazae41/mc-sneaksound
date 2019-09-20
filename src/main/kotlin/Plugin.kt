package hazae41.minecraft.sneaksound

import hazae41.minecraft.kotlin.bukkit.BukkitPlugin
import hazae41.minecraft.kotlin.bukkit.ConfigSection
import hazae41.minecraft.kotlin.bukkit.PluginConfigFile
import hazae41.minecraft.kotlin.bukkit.init
import hazae41.minecraft.kotlin.bukkit.listen
import hazae41.minecraft.kotlin.bukkit.msg
import hazae41.minecraft.kotlin.bukkit.schedule
import hazae41.minecraft.kotlin.bukkit.severe
import hazae41.minecraft.kotlin.bukkit.update
import hazae41.minecraft.kotlin.bukkit.warning
import hazae41.minecraft.kotlin.catch
import hazae41.minecraft.kotlin.get
import network.aeternum.bananapuncher714.localresourcepackhoster.LocalResourcePackHoster
import network.aeternum.bananapuncher714.localresourcepackhoster.resoucepack.SoundPackWrapper
import network.aeternum.bananapuncher714.localresourcepackhoster.resoucepack.SoundPackWrapper.SoundResource
import org.bukkit.Location
import org.bukkit.SoundCategory.PLAYERS
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object Config : PluginConfigFile("config") {
    val sendOnJoin by boolean("send-on-join")
    val sendOnSneak by boolean("send-on-sneak")
    val pitch by double("pitch")
    val volume by double("volume")
    val efficiency by int("efficiency")
}

val Player.config: Players.Data
    get() {
        val uuid = uniqueId.toString()
        if (uuid !in Players.config)
            Players.config.createSection(uuid)
        return Players.Data(uuid)
    }

object Players : PluginConfigFile("players") {
    class Data(uuid: String) : ConfigSection(this, uuid) {
        var enabled by boolean("enabled", true)
        var sound by string("sound")
    }
}

class Plugin : BukkitPlugin() {

    lateinit var lrph: LocalResourcePackHoster
    lateinit var sounds: List<File>

    fun Player.sendPack() {
        if (!config.enabled) return
        lrph.sendResourcePack(this, "sneak")
        msg(Locale.received)
    }

    override fun onEnable() {
        catch<Exception>(::severe) {
            update(16004)
            init(Config, Players, Locale)

            lrph = getPlugin(LocalResourcePackHoster::class.java)

            sounds = dataFolder["sounds"].listFiles()
                ?.filter { "ogg" in it.extension }.orEmpty()

            sounds.takeIf { it.any() }
                ?: warning(Locale.noSounds)

            makePack()
            makeCommands()
            makeListeners()
        }
    }
}

val File.soundName
    get() = nameWithoutExtension
        .toLowerCase()
        .trim()
        .replace(" ", "-")

fun Plugin.makePack() {
    val default = dataFolder["packs"]["default.zip"]
    val tmp = dataFolder["packs"]["tmp.zip"]
    if (tmp.exists()) tmp.delete()
    if (default.exists()) default.copyTo(tmp)
    lrph.resourcePacks["sneak"] = tmp
    SoundPackWrapper(tmp).apply {
        sounds.forEach {
            val sound = SoundResource("custom.sneak.${it.soundName}", it, true)
            addSound(sound, "custom", false)
            save()
        }
    }.close()
}

fun Location.playSound(name: String) {
    world.playSound(
        this,
        "custom.sneak.$name",
        PLAYERS,
        Config.volume.toFloat(),
        Config.pitch.toFloat()
    )
}

fun Plugin.makeListeners() {
    listen<PlayerJoinEvent> {
        if (Config.sendOnJoin)
            schedule(delay = 2, unit = TimeUnit.SECONDS) {
                it.player.sendPack()
            }
    }

    val receiveds = mutableListOf<UUID>()
    fun Player.sendPackOnce() {
        if (uniqueId in receiveds) return
        receiveds.add(uniqueId)
        sendPack()
    }

    listen<PlayerQuitEvent> {
        val uuid = it.player.uniqueId
        if (uuid in receiveds) receiveds.remove(uuid)
    }

    listen<PlayerToggleSneakEvent> {
        if (!it.isSneaking) return@listen
        if (Config.sendOnSneak) it.player.sendPackOnce()
        if (!it.player.hasPermission("sneaksound.use")) return@listen
        if (Random.nextInt(100) > Config.efficiency) return@listen
        val names = sounds.map { it.soundName }
        val config = it.player.config
        val sound = config.sound.takeIf { it in names } ?: names.random()
        it.player.location.playSound(sound)
    }
}
