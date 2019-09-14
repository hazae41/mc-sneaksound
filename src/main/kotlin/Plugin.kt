package hazae41.minecraft.sneaksound

import hazae41.minecraft.kotlin.bukkit.BukkitPlugin
import hazae41.minecraft.kotlin.bukkit.PluginConfigFile
import hazae41.minecraft.kotlin.bukkit.command
import hazae41.minecraft.kotlin.bukkit.init
import hazae41.minecraft.kotlin.bukkit.listen
import hazae41.minecraft.kotlin.bukkit.severe
import hazae41.minecraft.kotlin.catch
import network.aeternum.bananapuncher714.localresourcepackhoster.LocalResourcePackHoster
import network.aeternum.bananapuncher714.localresourcepackhoster.resoucepack.SoundPackWrapper
import network.aeternum.bananapuncher714.localresourcepackhoster.resoucepack.SoundPackWrapper.SoundResource
import org.bukkit.SoundCategory.PLAYERS
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerToggleSneakEvent
import java.io.File
import kotlin.random.Random

object Config : PluginConfigFile("config"){
    val sounds by stringList("sounds")
}

class Plugin : BukkitPlugin(){
    override fun onEnable() {
        catch<Exception>(::severe){
            init(Config)

            val sounds = Config.sounds
                .map{ File(dataFolder, it) }
                .filter{ it.exists() }

            val pack = File(dataFolder, "pack.zip")
            if(pack.exists()) pack.delete()

            pack.makePack(sounds)
            val lrph = getPlugin(LocalResourcePackHoster::class.java)
            lrph.resourcePacks["sneak"] = pack

            command("sneaksound"){ args ->
                val player = args.getOrNull(0)
                    ?.takeIf { hasPermission("sneaksound.send") }
                    ?.let { server.matchPlayer(it).getOrNull(0) }
                    ?: this as? Player ?: return@command
                lrph.sendResourcePack(player, "sneak")
            }

            listen<PlayerToggleSneakEvent>{
                if(!it.isSneaking) return@listen
                if(!it.player.hasPermission("sneaksound.use")) return@listen
                val i = if(sounds.size > 1) Random.nextInt(sounds.size) else 0
                fun Player.playSound() = playSound(it.player.location, "custom.sneak.$i", PLAYERS, 1f, 1f)
                it.player.world.players.forEach(Player::playSound)
            }
        }

    }

    fun File.makePack(sounds: List<File>){
        SoundPackWrapper(this).apply {
            sounds.forEachIndexed { i, sound ->
                addSound(SoundResource("custom.sneak.$i", sound, true), "custom", false)
            }
            save()
            close()
        }
    }
}