package io.github.com6235.btwabeautyplugin

import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.server.ServerListPingEvent
import org.bukkit.plugin.java.JavaPlugin

data class Messages(
    val firstJoinToasts: List<String>,
    val joinToasts: List<String>,
    val motd: List<Motd>,
    val joinMessages: List<String>,
    val firstJoinMessages: List<String>,
    val quitMessages: List<String>,
)

data class Motd(val firstLine: String, val secondLine: String)

class BtwABeautyPlugin : JavaPlugin(), Listener {
    private var messages: Messages? = null

    override fun onEnable() {
        val (_, err) = pcall(::loadConfig)
        if (err != null) throw SerializerException(err)
        logger.info(messages.toString())
        logger.info("BtwABeautyPlugin enabled!")
        Bukkit.getPluginManager().registerEvents(this, this)
    }

    private fun loadConfig() {
        saveDefaultConfig()
        val toastsSection = config.getConfigurationSection("Toasts")!!
        val messagesSection = config.getConfigurationSection("Messages")!!
        messages = Messages(
            toastsSection.getStringList("First-Join"),
            toastsSection.getStringList("Join"),
            (config.getList("Motd") as List<LinkedHashMap<Int, String>>).map { a -> Motd(a[1]!!, a[2]!!) },
            messagesSection.getStringList("Join"),
            messagesSection.getStringList("First-Join"),
            messagesSection.getStringList("Quit"),
        )
    }

    override fun onDisable() {
    }

    private fun PlayerJoinEvent.sendJoinMsg(messages: List<String>, toasts: List<String>) {
        val (message, toast) = messages.rand() to toasts.rand()
        if (toast != null) {
            player.sendMessage(toast.toComponent("player" to player.displayName()))
        }
        if (message != null) {
            joinMessage(message.toComponent("player" to player.displayName()))
        }
    }

    @EventHandler
    fun onPlayerJoin(e: PlayerJoinEvent) {
        val isFirstJoin = !e.player.hasPlayedBefore()
        if (isFirstJoin) {
            e.sendJoinMsg(messages!!.firstJoinMessages, messages!!.firstJoinToasts)
        } else {
            e.sendJoinMsg(messages!!.joinMessages, messages!!.joinToasts)
        }
    }

    @EventHandler
    fun onPlayerQuit(e: PlayerQuitEvent) {
        val text = messages!!.quitMessages.rand()
        if (text != null) e.quitMessage(text.toComponent("player" to e.player.displayName()))
    }

    @EventHandler
    fun onServerListed(e: ServerListPingEvent) {
        val motd = messages!!.motd.rand()
        if (motd != null) {
            val text = motd.firstLine + "<newline>" + motd.secondLine
            e.motd(text.toComponent())
        }
    }
}

fun String.toComponent(vararg args: Pair<String, ComponentLike>) =
    MiniMessage.miniMessage().deserialize(this, *args.map { Placeholder.component(it.first, it.second) }.toTypedArray())

@Suppress("all")
fun <T> List<T>.rand(): T? = if (isNotEmpty()) random() else null

@Suppress("ktlint:standard:try-catch-finally-spacing", "ktlint:standard:statement-wrapping")
inline fun <T> pcall(crossinline lambda: () -> T) = try { lambda() to null } catch (e: Throwable) { null to e }

class SerializerException(err: Throwable) : RuntimeException("Please check the config for errors", err)
