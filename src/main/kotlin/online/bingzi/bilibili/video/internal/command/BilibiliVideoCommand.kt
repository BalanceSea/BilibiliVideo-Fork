package online.bingzi.bilibili.video.internal.command

import online.bingzi.bilibili.video.internal.config.GuiConfigManager
import online.bingzi.bilibili.video.internal.config.RewardConfigManager
import online.bingzi.bilibili.video.internal.credential.QrLoginService
import online.bingzi.bilibili.video.internal.gui.VideoGui
import online.bingzi.bilibili.video.internal.gui.VideoGui.openVideoMenu
import online.bingzi.bilibili.video.internal.repository.BoundAccountRepository
import online.bingzi.bilibili.video.internal.service.BindingService
import online.bingzi.bilibili.video.internal.service.CredentialService
import online.bingzi.bilibili.video.internal.service.RewardKetherExecutor
import online.bingzi.bilibili.video.internal.service.RewardService
import online.bingzi.bilibili.video.internal.ui.VirtualItemSession
import online.bingzi.bilibili.video.internal.util.MessageArg
import online.bingzi.bilibili.video.internal.util.MessageUtil.sendParseLang
import online.bingzi.bilibili.video.internal.util.MessageUtil.sendPrefixedMessage
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.PermissionDefault
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import taboolib.expansion.createHelper
import taboolib.module.lang.Language
import taboolib.platform.util.asLangText
import taboolib.platform.util.submit
import java.util.UUID

/**
 * /bv 主命令。
 *
 * 当前实现的子命令：
 * - /bv help        查看帮助
 * - /bv qrcode      生成绑定用二维码地图
 * - /bv triple <bvid>  使用当前玩家绑定的 B 站账号检测指定稿件的三连状态
 * - /bv status      查看当前绑定状态与凭证信息
 * - /bv reward <bvid>  基于三连记录登记奖励
 * - /bv admin credential ... 管理/查看凭证
 * - /bv menu 查看视频列表
 *
 * 权限节点：
 * - bilibili.video.use          玩家基础命令（qrcode/status/triple/reward）
 * - bilibili.video.admin        管理员命令
 */
@CommandHeader(name = "bv", aliases = ["bilibili"], permission = "bilibili.video.use", permissionDefault = PermissionDefault.TRUE)
object BilibiliVideoCommand {

    @CommandBody
    val main = mainCommand {
        createHelper()
    }

    /**
     * 生成二维码地图，供玩家扫码绑定 B 站账号。
     */
    @CommandBody(permission = "bilibili.video.use", permissionDefault = PermissionDefault.TRUE)
    val qrcode = subCommand {
        execute<Player> { player, _, _ ->
            player.submit(async = true) {
                val result = QrLoginService.startLogin(player)
                if (!result.success || result.qrUrl == null) {
                    val message = result.message
                    player.submit(async = false) {
                        player.sendPrefixedMessage("&e$message")
                    }
                    return@submit
                }
                val qrUrl = result.qrUrl
                player.submit(async = false) {
                    // 使用虚拟物品发送二维码地图到主手
                    VirtualItemSession.sendVirtualItem(player, qrUrl)
                    player.sendParseLang("account-bind-tip")
                }
            }
        }
    }

    /**
     * 打开视频菜单，供玩家查看
     */
    @CommandBody(permission = "bilibili.video.use", permissionDefault = PermissionDefault.TRUE)
    val menu = subCommand {
        execute<Player> { player, _, _ ->
            val token = BoundAccountRepository.findByPlayerUuid(player?.uniqueId.toString())
            if (token == null || token.status != 1){
                player.sendParseLang("account-not-bind")
                return@execute
            }
            player.openVideoMenu()
        }
    }
    /**
     * 查看当前玩家的绑定状态与凭证信息。
     */
    @CommandBody(permission = "bilibili.video.use", permissionDefault = PermissionDefault.TRUE)
    val status = subCommand {
        execute<Player> { player, _, _ ->
            player.submit(async = true) {
                val binding = BindingService.getBoundAccount(player.uniqueId.toString())
                val credential = CredentialService.getCredentialInfo(player)

                player.submit(async = false) {
                    if (binding == null) {
                        player.sendParseLang("account-not-bind")
                    } else {
                        val name = MessageArg(
                            "player",
                            binding.playerName
                        )
                        val mid = MessageArg(
                            "user_mid",
                            "${binding.bilibiliMid}"
                        )
                        val userName = MessageArg(
                            "user_name",
                            binding.bilibiliName
                        )
                        player.sendParseLang("account-bind-info",true, name,mid,userName,)
//                        player.sendMessage("§b[BV] 当前绑定信息：")
//                        player.sendMessage(" §7- 玩家：§f${binding.playerName}")
//                        player.sendMessage(" §7- B 站 UID：§f${binding.bilibiliMid}")
//                        player.sendMessage(" §7- B 站昵称：§f${binding.bilibiliName}")
                    }

                    if (credential == null) {
                        player.sendParseLang("token-not-save")
                    } else {
                        val statusText = when (credential.status) {
                            0 -> "§c禁用"
                            1 -> "§a正常"
                            2 -> "§c过期"
                            else -> "§e未知(${credential.status})"
                        }
                        val tag = MessageArg(
                            "token_tag",
                            credential.label
                        )

                        val status = MessageArg(
                            "token_status",
                            statusText
                        )

                        val mid = MessageArg(
                            "user_mid",
                            "${credential.bilibiliMid ?: binding?.bilibiliMid}"
                        )

                        player.sendParseLang("token-info", true,tag,status,mid)
//                        player.sendMessage("§b[BV] 凭证信息：")
//                        player.sendMessage(" §7- 标签：§f${credential.label}")
//                        player.sendMessage(" §7- 状态：$statusText")
//                        player.sendMessage(" §7- 绑定 UID：§f${credential.bilibiliMid ?: binding?.bilibiliMid}")
                    }
                }
            }
        }
    }

    /**
     * 使用当前玩家绑定的 B 站账号检测指定稿件的三连状态。
     *
     * 参数：
     * - bvid：视频的 BVID，例如 BVxxxxxxxxx
     */
    @CommandBody(permission = "bilibili.video.use", permissionDefault = PermissionDefault.TRUE)
    val triple = subCommand {
        dynamic("bvid") {
            suggestion<Player> { _, _ ->
                RewardConfigManager.getConfiguredBvids()
            }
            execute<Player> { player, context, _ ->
                val bvid = context["bvid"]
                if (!ensureConfiguredBvid(player, bvid)) {
                    return@execute
                }
                player.submit(async = true) {
                    val result = CredentialService.checkTripleByPlayer(player, bvid)
                    player.submit(async = false) {
                        if (!result.success || result.tripleStatus == null) {
                            player.sendPrefixedMessage("&e${result.message}")
                            return@submit
                        }
                        val status = result.tripleStatus
                        val likeText = if (status.liked) "video-like-success" else "video-like-fail"
                        val coinText = if (status.coinCount > 0) "video-coin-success" else "video-coin-fail"
                        val favText = if (status.favoured) "video-favorite-success" else "video-favorite-fail"
                        val tripleText = if (status.isTriple) "video-triple-success" else "video-triple-fail"


                        val bv = MessageArg(
                            "bv",
                            bvid
                        )

                        val like = MessageArg(
                            "video_like",
                            player.asLangText(likeText)
                        )

                        val coin = MessageArg(
                            "video_coin",
                            player.asLangText(coinText)
                        )

                        val fav = MessageArg(
                            "video_favorite",
                            player.asLangText(favText)
                        )

                        val triple = MessageArg(
                            "video_triple",
                            player.asLangText(tripleText)
                        )

                        player.sendParseLang("video-status",bv,like,coin,fav,triple)
//                        player.sendMessage("§b[BV] 对稿件 $bvid 的三连状态：")
//                        player.sendMessage(" §7- $likeText")
//                        player.sendMessage(" §7- $coinText")
//                        player.sendMessage(" §7- $favText")
//                        player.sendMessage(" §7- $tripleText")
                    }
                }
            }
        }
    }

    /**
     * 基于三连记录登记奖励。
     */
    @CommandBody(permission = "bilibili.video.use", permissionDefault = PermissionDefault.TRUE)
    val reward = subCommand {
        dynamic("bvid") {
            suggestion<Player> { _, _ ->
                RewardConfigManager.getConfiguredBvids()
            }
            execute<Player> { player, context, _ ->
                val bvid = context["bvid"]
                if (!ensureConfiguredBvid(player, bvid)) {
                    return@execute
                }
                player.submit(async = true) {
                    val result = RewardService.rewardByPlayerAndBvid(player, bvid)
                    player.submit(async = false) {
                        if (!result.success) {
                            var bv = MessageArg(
                                "bv",
                                bvid
                            )
                            player.sendParseLang(result.message,bv)
                            return@submit
                        }

                        // 成功时，若存在奖励模板则执行 Kether
                        val template = result.template
                        if (template != null && result.bvid != null) {
                            val targetKey = result.targetKey ?: result.bvid
                            RewardKetherExecutor.execute(
                                player = player,
                                template = template,
                                bvid = result.bvid,
                                targetKey = targetKey
                            )
                        }

                        player.sendParseLang(result.message)
                    }
                }
            }
        }
    }

    /**
     * 管理员命令：管理与查看凭证。
     *
     * - /bv admin credential list
     * - /bv admin credential info <label>
     * - /bv admin credential refresh <label>   （占位，刷新逻辑后续实现）
     * - /bv admin unbind <target>
     * - /bv admin reload
     */
    @CommandBody(permission = "bilibili.video.admin", permissionDefault = PermissionDefault.OP)
    val admin = subCommand {
        literal("unbind") {
            dynamic("target") {
                suggestion<ProxyCommandSender> { _, _ ->
                    Bukkit.getOnlinePlayers().map { it.name }
                }
                execute<ProxyCommandSender> { sender, context, _ ->
                    val targetArg = context["target"]
                    val target = resolveUnbindTarget(targetArg)
                    if (target == null) {
                        sender.sendMessage("§c[BV] 未找到与 $targetArg 匹配的玩家、UUID 或 B 站 UID。")
                        return@execute
                    }
                    val result = BindingService.unbind(target.playerUuid)
                    if (!result.success) {
                        sender.sendMessage("§c[BV] ${result.message}")
                        return@execute
                    }
                    val nameText = target.displayName ?: target.playerUuid
                    val midText = target.bilibiliMid?.toString() ?: "未知 UID"
                    sender.sendMessage("§a[BV] 已解除玩家 $nameText 与 B 站账号 $midText 的绑定。")
                }
            }
        }
        literal("credential") {
            literal("list") {
                execute<ProxyCommandSender> { sender, _, _ ->
                    val all = online.bingzi.bilibili.video.internal.repository.CredentialRepository.findAll()
                    if (all.isEmpty()) {
                        sender.sendMessage("§e[BV] 当前没有任何凭证记录。")
                        return@execute
                    }
                    sender.sendMessage("§b[BV] 凭证列表：")
                    all.forEach {
                        val statusText = when (it.status) {
                            0 -> "禁用"
                            1 -> "正常"
                            2 -> "过期"
                            else -> "未知(${it.status})"
                        }
                        sender.sendMessage(" §7- §f${it.label} §7| UID=${it.bilibiliMid ?: "?"} | 状态=$statusText")
                    }
                }
            }
            literal("info") {
                dynamic("label") {
                    execute<ProxyCommandSender> { sender, context, _ ->
                        val label = context["label"]
                        val info = CredentialService.getCredentialInfo(label)
                        if (info == null) {
                            sender.sendMessage("§c[BV] 未找到名为 $label 的凭证。")
                            return@execute
                        }
                        sender.sendMessage("§b[BV] 凭证详情：$label")
                        sender.sendMessage(" §7- UID：§f${info.bilibiliMid ?: "未知"}")
                        sender.sendMessage(" §7- 状态：§f${info.status}")
                        sender.sendMessage(" §7- lastUsedAt：§f${info.lastUsedAt ?: 0}")
                        sender.sendMessage(" §7- expiredAt：§f${info.expiredAt ?: 0}")
                    }
                }
            }
            literal("refresh") {
                dynamic("label") {
                    execute<ProxyCommandSender> { sender, context, _ ->
                        val label = context["label"]
                        // 刷新逻辑尚未实现，这里仅做占位与提示。
                        sender.sendMessage("§e[BV] 凭证刷新流程尚未实现：$label")
                    }
                }
            }
        }
        literal("reload") {
            execute<ProxyCommandSender> { sender, _, _ ->
                val ok = RewardConfigManager.reload()
                val ok1 = GuiConfigManager.reload()
                Language.reload()
                sender.sendPrefixedMessage("&a已重载语言文件")
                if (ok) {
                    val size = RewardConfigManager.getConfiguredBvids().size
                    sender.sendPrefixedMessage("&a已重载配置文件，当前共登记 $size 个 reward.videos 项。")
                } else {
                    sender.sendPrefixedMessage("&c重载配置文件失败，请检查后台日志。")
                }
                if (ok1) {
                    VideoGui.reload()
                    sender.sendPrefixedMessage("&a已重载GUI配置")
                }else {
                    sender.sendPrefixedMessage("&a重载GUI配置失败，请检查后台日志。")
                }
            }
        }
        createHelper()
    }

    private fun ensureConfiguredBvid(player: Player, bvid: String): Boolean {
        if (!RewardConfigManager.isBvidConfigured(bvid)) {
            val bv = MessageArg(
                "bv",
                bvid
            )
            player.sendParseLang("video-not-find",bv)
            return false
        }
        return true
    }

    private data class UnbindTarget(
        val playerUuid: String,
        val displayName: String?,
        val bilibiliMid: Long?
    )

    private fun resolveUnbindTarget(argument: String): UnbindTarget? {
        runCatching { UUID.fromString(argument) }.getOrNull()?.let { uuid ->
            val uuidStr = uuid.toString()
            val binding = BoundAccountRepository.findByPlayerUuid(uuidStr, includeInactive = true)
            val name = binding?.playerName ?: Bukkit.getOfflinePlayer(uuid).name
            return UnbindTarget(uuidStr, name, binding?.bilibiliMid)
        }

        Bukkit.getPlayerExact(argument)?.let { player ->
            val uuidStr = player.uniqueId.toString()
            val binding = BoundAccountRepository.findByPlayerUuid(uuidStr, includeInactive = true)
            return UnbindTarget(uuidStr, player.name, binding?.bilibiliMid)
        }

        BoundAccountRepository.findByPlayerName(argument, includeInactive = true)?.let { binding ->
            return UnbindTarget(binding.playerUuid, binding.playerName, binding.bilibiliMid)
        }

        argument.toLongOrNull()?.let { mid ->
            BoundAccountRepository.findByBilibiliMid(mid, includeInactive = true)?.let { binding ->
                return UnbindTarget(binding.playerUuid, binding.playerName, binding.bilibiliMid)
            }
        }

        return null
    }
}
