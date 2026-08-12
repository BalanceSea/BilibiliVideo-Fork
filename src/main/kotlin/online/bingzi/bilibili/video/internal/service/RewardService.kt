package online.bingzi.bilibili.video.internal.service

import online.bingzi.bilibili.video.internal.config.DedupStrategy
import online.bingzi.bilibili.video.internal.config.RewardConfigManager
import online.bingzi.bilibili.video.internal.config.RewardTemplate
import online.bingzi.bilibili.video.internal.repository.BoundAccountRepository
import online.bingzi.bilibili.video.internal.repository.RewardRecordRepository
import online.bingzi.bilibili.video.internal.util.MessageUtil
import org.bukkit.entity.Player

/**
 * 奖励发放服务层。
 *
 * 当前实现：
 * - 基于「玩家 + bvid」检测是否完成三连
 * - 若完成且未领取过，则记录一条奖励记录
 * - 根据 rewardKey 从配置中载入 Kether 奖励模板，供命令层执行
 */
object RewardService {

    data class RewardResult(
        val success: Boolean,
        val message: String,
        val template: RewardTemplate? = null,
        val bvid: String? = null,
        val targetKey: String? = null
    )

    /**
     * 使用玩家绑定的 B 站账号，基于指定 bvid 发放奖励。
     *
     * @param player     Minecraft 玩家
     * @param bvid       视频 BVID
     * @param targetKey  奖励目标 key（默认使用 bvid）
     * @param rewardKey  奖励模板 key（默认 "default"）
     */
    fun rewardByPlayerAndBvid(
        player: Player,
        bvid: String,
        targetKey: String = bvid,
        rewardKey: String = "default"
    ): RewardResult {
        if (!RewardConfigManager.isBvidConfigured(bvid)) {
            return RewardResult(
                success = false,
                message = "video-not-find"
            )
        }

        // 获取玩家绑定的 B 站账号信息
        val playerUuid = player.uniqueId.toString()
        val boundAccount = BoundAccountRepository.findByPlayerUuid(playerUuid)
        val bilibiliMid = boundAccount?.bilibiliMid

        // 检查三连状态
        val tripleResult = CredentialService.checkTripleByPlayer(player, bvid)
        if (!tripleResult.success || tripleResult.tripleStatus == null) {
            return RewardResult(
                success = false,
                message = tripleResult.message
            )
        }

        val tripleStatus = tripleResult.tripleStatus
        if (!tripleStatus.isTriple) {
            return RewardResult(
                success = false,
                message = MessageUtil.parseText("video-not-claim")
            )
        }

        // 解析奖励模板
        val resolve = RewardConfigManager.resolveForBvid(bvid, defaultKey = rewardKey)
        val resolvedRewardKey = resolve.rewardKey

        // 根据配置的判重策略检查是否已领取
        val dedupStrategy = RewardConfigManager.getDedupStrategy()
        val alreadyClaimed = checkAlreadyClaimed(
            strategy = dedupStrategy,
            playerUuid = playerUuid,
            bilibiliMid = bilibiliMid,
            targetKey = targetKey,
            rewardKey = resolvedRewardKey
        )

        if (alreadyClaimed) {
            return RewardResult(
                success = false,
                message = MessageUtil.parseText("video-claimed")
            )
        }

        val inserted = RewardRecordRepository.insert(
            playerUuid = playerUuid,
            playerName = player.name,
            bilibiliMid = bilibiliMid,
            targetKey = targetKey,
            rewardKey = resolvedRewardKey,
            status = 1,
            context = "bvid=$bvid; triple=true; bilibiliMid=$bilibiliMid",
            failReason = null
        )

        if (inserted <= 0) {
            return RewardResult(
                success = false,
                message = MessageUtil.parseText("video-claim-error")
            )
        }

        val template = resolve.template
        val msg = if (template != null) {
            MessageUtil.parseText("video-claim")
        } else {
            MessageUtil.parseText("video-claim-warn")
        }

        return RewardResult(
            success = true,
            message = msg,
            template = template,
            bvid = bvid,
            targetKey = targetKey
        )
    }

    /**
     * 根据判重策略检查是否已领取奖励。
     */
     fun checkAlreadyClaimed(
        strategy: DedupStrategy,
        playerUuid: String,
        bilibiliMid: Long?,
        targetKey: String,
        rewardKey: String
    ): Boolean {
        return when (strategy) {
            DedupStrategy.PLAYER_ONLY -> {
                val existing = RewardRecordRepository.findAllByPlayerAndTarget(playerUuid, targetKey)
                existing.any { it.rewardKey == rewardKey && it.status == 1 }
            }

            DedupStrategy.BILIBILI_ONLY -> {
                if (bilibiliMid == null) {
                    // 没有绑定 B 站账号，理论上不应该走到这里，但保险起见返回 false
                    false
                } else {
                    val existing = RewardRecordRepository.findAllByBilibiliMidAndTarget(bilibiliMid, targetKey)
                    existing.any { it.rewardKey == rewardKey && it.status == 1 }
                }
            }

            DedupStrategy.PLAYER_AND_BILIBILI -> {
                if (bilibiliMid == null) {
                    // 没有绑定 B 站账号，回退到仅检查玩家
                    val existing = RewardRecordRepository.findAllByPlayerAndTarget(playerUuid, targetKey)
                    existing.any { it.rewardKey == rewardKey && it.status == 1 }
                } else {
                    val existing = RewardRecordRepository.findAllByPlayerAndBilibiliMidAndTarget(
                        playerUuid,
                        bilibiliMid,
                        targetKey
                    )
                    existing.any { it.rewardKey == rewardKey && it.status == 1 }
                }
            }
        }
    }
}
