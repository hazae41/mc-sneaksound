package hazae41.minecraft.sneaksound

import hazae41.minecraft.kotlin.bukkit.PluginConfigFile
import org.bukkit.entity.Player

object Locale : PluginConfigFile("locale") {
    val noSounds by string("no-sounds")
    val noPerm by string("no-perm")
    val notPlayer by string("not-player")

    val received by string("received")

    val unknownPlayer by string("unknown-player")
    fun unknownPlayer(name: String) = unknownPlayer.replace("%name%", name)

    val remadePack by string("remade-pack")

    val unsetSelf by string("unset-self")
    val unsetOther by string("unset-other")
    fun unsetOther(player: Player) = unsetOther.replace("%name%", player.name)

    val unknownSound by string("unknown-sound")

    val setSelf by string("set-self")
    fun setSelf(sound: String) = setSelf.replace("%sound%", sound)

    val setOther by string("set-other")
    fun setOther(player: Player, sound: String) = setOther
        .replace("%name%", player.name)
        .replace("%sound%", sound)

    val enabledSelf by string("enabled-self")
    val disabledSelf by string("disabled-self")

    val enabledOther by string("enabled-other")
    fun enabledOther(player: Player) = enabledOther.replace("%name%", player.name)
    val disabledOther by string("disabled-other")
    fun disabledOther(player: Player) = disabledOther.replace("%name%", player.name)

    val notEnabledSelf by string("not-enabled-self")

    val sentAll by string("sent-all")
    val sentOther by string("sent-other")
    fun sentOther(player: Player) = sentOther.replace("%name%", player.name)
}