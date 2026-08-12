package online.bingzi.bilibili.video.internal.config

import taboolib.common.platform.function.warning
import taboolib.library.xseries.XMaterial
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigFile
import java.util.Locale

enum class IconType {
    NORMAL,
    VIDEO,
    PREVIOUS,
    NEXT
}

/**
 * 图标配置
 */
data class Icon(
    val key : String,
    val name : String,
    val mats : XMaterial,
    val lore : List<String>,
    val type : IconType ?= IconType.NORMAL,
    val cmd : Int = -1
)

data class GuiConfig(
    val title : String,
    val layout: List<String>,
    val icons : List<Icon>
)

object GuiConfigManager {
    @Config("gui.yml")
    lateinit var file: ConfigFile
        private set

    fun getGuiConfig(): GuiConfig {
        val title = requireNotNull(file.getString("Title")) {
            "gui.yml 缺少字符串配置项: Title"
        }

        val layout = file.getStringList("Layout")
        requireNotNull(layout.isNotEmpty()) {
            "gui.yml 缺少菜单布局: Layout"
        }

        val icons = file.getConfigurationSection("Icons")
            ?.getKeys(false)
            ?.map { key ->
                val path = "Icons.$key"
                val typeValue = file.getString("$path.type", IconType.NORMAL.name)!!

                val type = try {
                    IconType.valueOf(typeValue.trim().uppercase(Locale.ROOT))
                } catch (ex: IllegalArgumentException) {
                    error(
                        "gui.yml 配置项 $path.type 的值 '$typeValue' 无效，" +
                                "允许值: ${IconType.entries.joinToString()}"
                    )
                }

                Icon(
                    key = key,
                    name = file.getString("$path.display.name", " ")!!,
                    mats = XMaterial.matchXMaterial(file.getString("$path.display.mats").toString()).orElse(XMaterial.STONE)!!,
                    lore = file.getStringList("$path.display.lore"),
                    cmd = file.getInt("$path.display.cmd"),
                    type = type,
                )
            }
            .orEmpty()

        return GuiConfig(title, layout, icons)
    }

    fun reload(): Boolean {
        return try {
            file.reload()
            true
        } catch (ex: Throwable) {
            warning("重载 gui.yml 失败: ${ex.message}",ex)
            false
        }
    }
}