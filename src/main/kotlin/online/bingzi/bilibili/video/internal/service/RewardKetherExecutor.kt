package online.bingzi.bilibili.video.internal.service

import online.bingzi.bilibili.video.internal.config.RewardTemplate
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.console
import taboolib.expansion.fakeOp
import taboolib.module.chat.colored
import taboolib.module.kether.KetherShell
import taboolib.module.kether.ScriptOptions
import taboolib.platform.type.BukkitCommandSender

/**
 * 使用 Kether 执行奖励模板。
 */
object RewardKetherExecutor {

    /**
     * 执行给定奖励模板。
     * 默认以控制台执行
     * @param player    领取奖励的玩家
     * @param template  奖励模板
     * @param bvid      视频 BVID
     * @param targetKey 奖励目标 key
     */
    fun execute(player: Player, template: RewardTemplate, bvid: String, targetKey: String) {
        val script = template.lines.joinToString("\n").colored()
        KetherShell.eval(
            source = script,
            options = ScriptOptions.new {
                sender(Bukkit.getConsoleSender())
                // 允许使用通用与 Bukkit 相关的 action，后续可根据需要调整 namespace
                namespace(listOf("kether", "bukkit"))
                vars(
                    mapOf(
                        "player" to player.name,
                        "bvid" to bvid,
                        "target" to targetKey,
                        "rewardKey" to template.key
                    )
                )
                sandbox()
            }
        )
    }
}

