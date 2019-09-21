package hazae41.minecraft.sneaksound

import hazae41.minecraft.kotlin.bukkit.command
import hazae41.minecraft.kotlin.bukkit.msg
import hazae41.minecraft.kotlin.catch
import hazae41.minecraft.kotlin.ex
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

fun Plugin.makeCommands() = command("sneaksound") { args ->

    fun CommandSender.has(perm: String) = hasPermission("sneaksound.$perm")

    fun CommandSender.require(perm: String) {
        if (!has(perm)) throw ex(Locale.noPerm)
    }

    fun getPlayer() =
        this as? Player ?: throw ex(Locale.notPlayer)

    fun String.getPlayer() =
        server.matchPlayer(this).getOrNull(0)
            ?: throw ex(Locale.unknownPlayer(this))

    catch<Exception>(::msg) {
        when (args.getOrNull(0)) {
            "list" -> {
                require("list")
                msg(Locale.cmdListTitle)
                sounds.forEach { msg("&b${it.soundName}") }
                msg(Locale.cmdEnd)
            }
            "remake" -> {
                require("remake")
                makePack()
                msg(Locale.remadePack)
            }
            "unset" -> {
                require("set")
                when (val target = args.getOrNull(2)) {
                    null -> {
                        getPlayer().config.sound = ""
                        msg(Locale.unsetSelf)
                    }
                    else -> {
                        require("set.other")
                        val player = target.getPlayer()
                        player.config.sound = ""
                        msg(Locale.unsetOther(player))
                    }
                }
            }
            "set" -> {
                require("set")

                val sound = args.getOrNull(1)?.toLowerCase()
                    ?: throw ex("/sneaksound set <sound> [player]")

                when (val target = args.getOrNull(2)) {
                    null -> {
                        if (sound !in sounds.map { it.soundName })
                            throw ex(Locale.unknownSound)
                        getPlayer().config.sound = sound
                        msg(Locale.setSelf(sound))
                    }
                    else -> {
                        require("set.other")
                        val player = target.getPlayer()
                        if (sound !in sounds.map { it.soundName })
                            throw ex(Locale.unknownSound)
                        player.config.sound = sound
                        msg(Locale.setOther(player, sound))
                    }
                }
            }
            "toggle" -> {
                require("togggle")
                when (val target = args.getOrNull(1)) {
                    null -> {
                        val config = getPlayer().config
                        config.enabled = !config.enabled
                        when (config.enabled) {
                            true -> msg(Locale.enabledSelf)
                            false -> msg(Locale.disabledSelf)
                        }
                    }
                    else -> {
                        require("toggle.other")
                        val player = target.getPlayer()
                        val config = player.config
                        config.enabled = !config.enabled
                        when (config.enabled) {
                            true -> msg(Locale.enabledOther(player))
                            false -> msg(Locale.disabledOther(player))
                        }
                    }
                }
            }
            "send" -> {
                require("send")
                when (val target = args.getOrNull(1)) {
                    null -> {
                        val player = getPlayer()
                        if (!player.config.enabled)
                            throw ex(Locale.notEnabledSelf)
                        player.sendPack()
                    }
                    "all" -> {
                        require("send.all")
                        server.onlinePlayers.forEach {
                            if (it.config.enabled) it.sendPack()
                        }
                        msg(Locale.sentAll)
                    }
                    else -> {
                        require("send.other")
                        val player = target.getPlayer()
                        player.sendPack()
                        msg(Locale.sentOther(player))
                    }
                }
            }
            else -> {
                msg(Locale.cmdTitle)
                if (this is Player) {
                    if (config.sound.isNotEmpty())
                        msg(Locale.statusSound(config.sound))
                    when (config.enabled) {
                        true -> msg(Locale.statusEnabled)
                        false -> msg(Locale.statusDisabled)
                    }
                }
                if (has("about")) {
                    msg("&bAuthors: ${description.authors}")
                    msg("&bVersion: ${description.version}")
                }
                msg(Locale.cmdCommands)
                if (has("list")) msg(Locale.cmdList)
                if (has("toggle")) msg(Locale.cmdToggle)
                if (has("send")) msg(Locale.cmdSend)
                if (has("set")) msg(Locale.cmdSet)
                if (has("unset")) msg(Locale.cmdUnset)
                if (has("remake")) msg(Locale.cmdRemake)
                msg(Locale.cmdEnd)
            }
        }
    }
}