package online.bingzi.bilibili.video.internal.util

import org.bukkit.entity.Player
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.ProxyPlayer
import taboolib.module.chat.colored
import taboolib.module.lang.asLangText
import taboolib.module.lang.asLangTextList
import taboolib.platform.util.asLangText
import taboolib.platform.util.asLangTextList
import taboolib.platform.util.sendMessage

data class MessageArg(
    val key: String,
    val value: String
)

object MessageUtil {

    /**
     * 发送自定义消息
     */
    fun Player.sendPrefixedMessage(text: String) {
        val prefix = asLangText("prefix")
        sendMessage("$prefix ${text.colored()}")
    }
    fun ProxyCommandSender.sendPrefixedMessage(text: String) {
        val prefix = asLangText("prefix")
        sendMessage("$prefix ${text.colored()}")
    }

    /**
     * 发送自定义解析的消息
     */
    fun ProxyCommandSender.sendParseLang(node: String, vararg args: MessageArg) {
        val prefix = asLangText("prefix")
        var message = asLangText(node)
        message = parseText(message, *args)
        sendMessage("$prefix$message")
    }

    fun ProxyCommandSender.sendParseLang(node: String, isList: Boolean, vararg args: MessageArg) {
        val prefix = asLangText("prefix")
        var texts = asLangTextList(node)
        texts = parseText(texts, *args)
        if (isList) {
            for (text in texts) {
                sendMessage("$prefix$text")
            }
        }
    }

    fun Player.sendParseLang(node: String, vararg args: MessageArg) {
        val prefix = asLangText("prefix")
        var message = asLangText(node)
        message = parseText(message, *args)
        sendMessage("$prefix$message")
    }
    fun Player.sendParseLang(node: String, isList: Boolean, vararg args: MessageArg) {
        val prefix = asLangText("prefix")
        var texts = asLangTextList(node)
        texts = parseText(texts, *args)
        if (isList) {
            for (text in texts) {
                sendMessage("$prefix$text")
            }
        }
    }
    /**
     * 检查是否为玩家
     */
    fun ProxyCommandSender.requirePlayer(action: (ProxyPlayer) -> Unit) {
        if (this is ProxyPlayer) {
            action(this)
        } else {
            sendParseLang("player-only")
        }
    }

    /**
     * 解析自定义占位符
     */
    fun parseText(text: String, vararg args: MessageArg): String {
        var result = text

        for (arg in args) {
            result = result.replace(
                oldValue = "{${arg.key}}",
                newValue = arg.value
            )
        }

        return result.colored()
    }

    fun parseText(texts: List<String>, vararg args: MessageArg): List<String> {
        return texts.map { text ->
            args.fold(text) { result, arg ->
                result.replace("{${arg.key}}", arg.value)
            }
        }
    }

    fun Player.parseNodeText(node: String, vararg args: MessageArg): String {
        var message = asLangText(node)
        message = parseText(message, *args)
        return message
    }
}