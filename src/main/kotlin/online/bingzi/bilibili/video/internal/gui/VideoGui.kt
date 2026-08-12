package online.bingzi.bilibili.video.internal.gui

import online.bingzi.bilibili.video.internal.config.GuiConfig
import online.bingzi.bilibili.video.internal.config.GuiConfigManager
import online.bingzi.bilibili.video.internal.config.Icon
import online.bingzi.bilibili.video.internal.config.IconType
import online.bingzi.bilibili.video.internal.config.RewardConfigManager
import online.bingzi.bilibili.video.internal.repository.BoundAccountRepository
import online.bingzi.bilibili.video.internal.service.CredentialService
import online.bingzi.bilibili.video.internal.service.RewardKetherExecutor
import online.bingzi.bilibili.video.internal.service.RewardService
import online.bingzi.bilibili.video.internal.service.TripleStatusCache
import online.bingzi.bilibili.video.internal.util.MessageArg
import online.bingzi.bilibili.video.internal.util.MessageUtil.parseNodeText
import online.bingzi.bilibili.video.internal.util.MessageUtil.sendParseLang
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.library.xseries.XMaterial
import taboolib.module.chat.colored
import taboolib.module.ui.ClickType.*
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.PageableChest
import taboolib.platform.util.buildItem
import taboolib.platform.util.submit

/**
 * 三连奖励状态枚举
 */
enum class VideoStatus{
    CLAIM,
    CLAIMED,
    NOTCLAIM
}

/**
 * 视频物品集合
 */
data class VideoItemStack(
    val item: ItemStack,
    val claim: VideoStatus,
    val bvid: String
)

object VideoGui {
    private var guiConfig: GuiConfig = GuiConfigManager.getGuiConfig()

    fun reload() {
        guiConfig = GuiConfigManager.getGuiConfig()
    }

    fun Player.openVideoMenu() {
        val bvids = RewardConfigManager
            .getConfiguredBvids()
            .distinct()
            .toList()

        openMenu<PageableChest<String>>(guiConfig.title.colored()) {
            map(*guiConfig.layout.toTypedArray())

            for (icon in guiConfig.icons) {

                if (icon.type == IconType.VIDEO) {
                    slotsBy(icon.key.single())

                    elements {
                        bvids
                    }
                    val map = HashMap<ItemStack, VideoItemStack>()

                    onClose(true){
                        map.clear()
                    }
                    onGenerate(async = true) { player, bvid, _, _ ->
                        val result = TripleStatusCache
                            .getOrLoad(player.uniqueId, bvid)
                            .join()

                        val videoItemNew = buildVideoItem(
                            player = player,
                            icon = icon,
                            bvid = bvid,
                            result = result
                        )
                        map[videoItemNew.item] = videoItemNew
                        videoItemNew.item
                    }

                    onClick { event, _ ->
                        if (event.clickType == CLICK) {
                            val player = event.clicker
                            val event = event.clickEvent()
                            val videoItem = map.getValue(event.currentItem!!)
                            if (event.click == org.bukkit.event.inventory.ClickType.LEFT) {
                                when (videoItem.claim) {
                                    VideoStatus.CLAIM -> {
                                        player.submit(async = true) {
                                            val result = RewardService.rewardByPlayerAndBvid(player, videoItem.bvid)
                                            player.submit(async = false) {
                                                if (!result.success) {
                                                    val bv = MessageArg(
                                                        "bv",
                                                        videoItem.bvid
                                                    )
                                                    player.sendParseLang(result.message, bv)
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

                                                closeInventory()
                                            }
                                        }
                                        return@onClick
                                    }

                                    VideoStatus.CLAIMED -> {
                                        sendParseLang("video-claimed")
                                        return@onClick
                                    }

                                    VideoStatus.NOTCLAIM -> {
                                        sendParseLang("video-not-claim")
                                        return@onClick
                                    }
                                }
                            }else if (event.click == org.bukkit.event.inventory.ClickType.RIGHT) {
                                val url = MessageArg(
                                    "bv",
                                    videoItem.bvid
                                )
                                sendParseLang("video-url",url)
                                closeInventory()
                            }
                        }
                    }
                    continue
                }

                if (icon.type == IconType.PREVIOUS) {
                    setPreviousPage(getFirstSlot(icon.key[0])){ page, hasPreviousPage ->
                        buildItem(if (hasPreviousPage) icon.mats else XMaterial.GRAY_STAINED_GLASS_PANE) {
                            name = if (hasPreviousPage) icon.name else "§7没有上一页了"
                            lore += icon.lore.map { line ->
                                line
                                    .replace("%page%", (page + 1).toString())
                            }
                            customModelData = icon.cmd
                            colored()
                        }
                    }
                    continue
                }else if (icon.type == IconType.NEXT) {
                    setNextPage(getFirstSlot(icon.key[0])){ page, hasNext ->
                        buildItem(if (hasNext) icon.mats else XMaterial.GRAY_STAINED_GLASS_PANE) {
                            name = if (hasNext) icon.name else "§7没有下一页了"
                            lore += icon.lore.map { line ->
                                line
                                    .replace("%page%", (page + 1).toString())
                            }
                            customModelData = icon.cmd
                            colored()
                        }
                    }
                    continue
                }


                val item = buildItem(icon.mats) {
                    name = icon.name
                    lore += icon.lore
                    if (icon.mats == XMaterial.PLAYER_HEAD){
                        skullOwner = this@openVideoMenu.name
                    }
                    icon.cmd
                    colored()
                }

                set(icon.key.single(), item)
            }
        }
    }

    /**
     * 构建BV视频物品
     */
    private fun buildVideoItem(
        player: Player,
        icon: Icon,
        bvid: String,
        result: CredentialService.TripleCheckResult
    ): VideoItemStack {
        val status = result.tripleStatus

        val template = RewardConfigManager.resolveForBvid(bvid).template

        val desc: String = when(template != null) {
            true -> template.description
            false -> "§c查询失败"
        }.toString()

        val likeText = when (status?.liked) {
            true -> "§a已点赞"
            false -> "§c未点赞"
            null -> "§c查询失败"
        }

        val coinText = when {
            status == null -> "§c查询失败"
            status.coinCount > 0 -> "§a已投币(${status.coinCount})"
            else -> "§c未投币"
        }

        val favoriteText = when (status?.favoured) {
            true -> "§a已收藏"
            false -> "§c未收藏"
            null -> "§c查询失败"
        }

        val tripleText = when (status?.isTriple) {
            true -> "§a已完成三连"
            false -> "§e尚未完成三连"
            null -> "§c查询失败"
        }
        var claimStatusMessage: String
        var claimStatus: VideoStatus

        val resolve = RewardConfigManager.resolveForBvid(bvid)
        val account = BoundAccountRepository.findByPlayerUuid(player.uniqueId.toString())

        val claimed = RewardService.checkAlreadyClaimed(
                strategy = RewardConfigManager.getDedupStrategy(),
                playerUuid = player.uniqueId.toString(),
                bilibiliMid = account?.bilibiliMid,
                targetKey = bvid,
                rewardKey = resolve.rewardKey
        )

        if (status?.isTriple == true) {
            if (claimed){
                claimStatusMessage = player.parseNodeText("gui-video-already-claim")
                claimStatus = VideoStatus.CLAIMED
            }else {
                claimStatusMessage = player.parseNodeText("gui-video-claim")
                claimStatus = VideoStatus.CLAIM
            }
        }else{
            claimStatusMessage = player.parseNodeText("gui-video-cant-claim")
            claimStatus = VideoStatus.NOTCLAIM
        }

        val item = buildItem(icon.mats) {
            name = icon.name.replace("%bv%", bvid)

            lore += icon.lore.map { line ->
                line
                    .replace("%bv%", bvid)
                    .replace("%status_like%", likeText)
                    .replace("%status_coin%", coinText)
                    .replace("%status_favorite%", favoriteText)
                    .replace("%status%", tripleText)
                    .replace("%description%",desc)
                    .replace("%claim_status%",claimStatusMessage)
            }
            customModelData = icon.cmd
            colored()
        }
        return VideoItemStack(
            item,
            claimStatus,
            bvid
        )
    }
}