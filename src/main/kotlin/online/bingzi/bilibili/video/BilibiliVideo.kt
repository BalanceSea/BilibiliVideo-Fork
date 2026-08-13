package online.bingzi.bilibili.video

import online.bingzi.bilibili.video.internal.credential.QrLoginService
import online.bingzi.bilibili.video.internal.database.DatabaseFactory
import online.bingzi.bilibili.video.internal.service.TripleStatusCache
import taboolib.common.platform.Platform
import taboolib.common.platform.Plugin
import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning
import taboolib.module.metrics.Metrics
import taboolib.platform.util.bukkitPlugin

object BilibiliVideo : Plugin() {
    override fun onEnable() {
        info("正在启动 BilibiliVideo-Fork 插件")
        DatabaseFactory.initFromConfig()
    }

    override fun onActive() {
        info("BilibiliVideo-Fork 插件启动完成！")
        info("当前插件版本: ${bukkitPlugin.description.version}")
        info("原作者: BingZi-233")
        info("Fork作者: BalanceSea | 山海")
        info("插件交流群: 3643203568")
        // 初始化Metrics以收集插件的使用统计信息
        Metrics(33320, bukkitPlugin.description.version, Platform.BUKKIT)
    }

    override fun onDisable() {
        info("正在禁用 BilibiliVideo 插件...")
        try {
            QrLoginService.shutdown()
        } catch (e: Throwable) {
            warning("[BilibiliVideo-Fork] QrLoginService 关闭异常: ${e.message}")
        }
        try {
            DatabaseFactory.shutdown()
        } catch (e: Throwable) {
            warning("[BilibiliVideo-Fork] DatabaseFactory 关闭异常: ${e.message}")
        }
        try {
            TripleStatusCache.close()
        } catch (e: Throwable) {
            warning("[BilibiliVideo-Fork] TripleStatusCache 关闭异常: ${e.message}")
        }
        info("BilibiliVideo-Fork 插件禁用完成！")
    }
}
