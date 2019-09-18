package hazae41.minecraft.sneaksound

import hazae41.minecraft.kotlin.bukkit.BukkitPlugin
import hazae41.minecraft.kotlin.bukkit.ConfigSection
import hazae41.minecraft.kotlin.bukkit.PluginConfigFile
import hazae41.minecraft.kotlin.bukkit.command
import hazae41.minecraft.kotlin.bukkit.init
import hazae41.minecraft.kotlin.bukkit.listen
import hazae41.minecraft.kotlin.bukkit.msg
import hazae41.minecraft.kotlin.bukkit.severe
import hazae41.minecraft.kotlin.bukkit.update
import hazae41.minecraft.kotlin.catch
import hazae41.minecraft.kotlin.ex
import hazae41.minecraft.kotlin.get
import network.aeternum.bananapuncher714.localresourcepackhoster.LocalResourcePackHoster
import network.aeternum.bananapuncher714.localresourcepackhoster.resoucepack.SoundPackWrapper
import network.aeternum.bananapuncher714.localresourcepackhoster.resoucepack.SoundPackWrapper.SoundResource
import org.bukkit.Location
import org.bukkit.SoundCategory.PLAYERS
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerToggleSneakEvent
import java.io.File

object Config : PluginConfigFile("config") {
    val pitch by double("pitch")
    val volume by double("volume")
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

    override fun onEnable() {
        catch<Exception>(::severe) {
            update(16004)
            init(Config, Players)

            lrph = getPlugin(LocalResourcePackHoster::class.java)
            sounds = dataFolder.listFiles()!!.filter { "ogg" in it.extension }

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
    val pack = dataFolder["tmp"]["pack.zip"]
    if (pack.exists()) pack.delete()
    lrph.resourcePacks["sneak"] = pack
    SoundPackWrapper(pack).apply {
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

fun Plugin.makeListeners() = listen<PlayerToggleSneakEvent> {
    if (!it.isSneaking) return@listen
    if (!it.player.hasPermission("sneaksound.use")) return@listen
    val names = sounds.map { it.soundName }
    val config = it.player.config
    val sound = config.sound.takeIf { it in names } ?: names.random()
    it.player.location.playSound(sound)
}

fun Plugin.makeCommands() = command("sneaksound") { args ->

    fun CommandSender.require(perm: String) {
        if (!hasPermission("sneaksound.$perm"))
            throw ex("&cYou don't have permission")
    }

    fun String.getPlayer() =
        server.matchPlayer(this).getOrNull(0)
            ?: throw ex("&cUnknown player: $name")

    val notPlayer = ex("&cYou're not a player")

    catch<Exception>(::msg) {
        when (args.getOrNull(0)) {
            "unset" -> {
                when (val target = args.getOrNull(2)) {
                    null -> {
                        require("set")
                        val player = this as? Player ?: throw notPlayer
                        player.config.sound = ""
                        msg("&bUnset sneak sound")
                    }
                    else -> {
                        require("set.other")
                        val player = target.getPlayer()
                        player.config.sound = ""
                        msg("&bUnset sneak sound of ${player.name}")
                    }
                }
            }
            "set" -> {
                val sound = args.getOrNull(1)?.toLowerCase()
                    ?: throw ex("/sneaksound set <sound> [player]")

                when (val target = args.getOrNull(2)) {
                    null -> {
                        require("set")
                        val player = this as? Player ?: throw notPlayer
                        if (sound !in sounds.map { it.soundName })
                            throw ex("&cThis sound doesn't exists")
                        player.config.sound = sound
                        msg("&bSet sneak sound to $sound")
                    }
                    else -> {
                        require("set.other")
                        val player = target.getPlayer()
                        if (sound !in sounds.map { it.soundName })
                            throw ex("&cThis sound doesn't exists")
                        player.config.sound = sound
                        msg("&bSet sneak sound of ${player.name} to $sound")
                    }
                }
            }
            "toggle" -> {
                when (val target = args.getOrNull(1)) {
                    null -> {
                        require("toggle")
                        val player = this as? Player ?: throw notPlayer
                        val config = player.config
                        config.enabled = !config.enabled
                        when (config.enabled) {
                            true -> msg("&bEnabled sneak sounds")
                            false -> msg("&bDisabled sneak sounds")
                        }
                    }
                    else -> {
                        require("toggle.other")
                        val player = target.getPlayer()
                        val config = player.config
                        config.enabled = !config.enabled
                        when (config.enabled) {
                            true -> msg("&bEnabled sneak sounds for ${player.name}")
                            false -> msg("&bDisabled sneak sounds for ${player.name}")
                        }
                    }
                }
            }
            "send" -> {
                when (val target = args.getOrNull(1)) {
                    null -> {
                        require("send")
                        val player = this as? Player ?: throw notPlayer
                        val config = player.config
                        if (!config.enabled) throw ex("&cSneak sounds are not enabled")
                        lrph.sendResourcePack(player, "sneak")
                        msg("&bReceived resource pack")
                    }
                    else -> {
                        require("send.other")
                        val player = target.getPlayer()
                        val config = player.config
                        if (!config.enabled) throw ex("&cSneak sounds are not enabled for ${player.name}")
                        lrph.sendResourcePack(player, "sneak")
                        msg("&bSent resource pack to ${player.name}")
                    }
                }
            }
            else -> {
                require("help")
                msg("&b~ hazae41's SneakSound ${description.version} ~")
                msg("&bCommands:")
                msg("&b- Toggle sounds: /sneaksound toggle [player]")
                msg("&b- Send pack: /sneaksound send [player]")
                msg("&b- Set sound: /sneaksound set <sound> [player]")
                msg("&b- Unset sound: /sneaksound unset [player]")
                msg("&b~ the end ~")
            }
        }
    }
}