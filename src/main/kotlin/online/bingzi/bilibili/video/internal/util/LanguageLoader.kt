package online.bingzi.bilibili.video.internal.util

import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.module.lang.Language
import taboolib.module.lang.registerLanguage

@Awake(LifeCycle.ENABLE)
object LanguageLoader {

    fun init() {
        Language.default = "zh_CN"

        registerLanguage("zh_CN")
    }

}