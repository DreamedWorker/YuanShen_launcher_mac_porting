package icu.bluedream.gamesetup.core.util

import cn.hutool.core.io.resource.ClassPathResource
import icu.bluedream.gamesetup.core.constant.Constants
import icu.bluedream.gamesetup.core.constant.Constants.GS_CLOUD_GAME_PARAM
import icu.bluedream.gamesetup.core.constant.Constants.GS_EXE
import icu.bluedream.gamesetup.core.constant.Constants.LAUNCH_BAT
import icu.bluedream.gamesetup.core.constant.Constants.MAC_APPLICATION_PATH
import icu.bluedream.gamesetup.core.constant.Constants.WINE_DRIVE_C_PATH
import icu.bluedream.gamesetup.core.constant.Constants.WINE_MAC_ROOT_PATH
import icu.bluedream.gamesetup.core.constant.Constants.ZZZ_EXE
import icu.bluedream.gamesetup.core.constant.GameType
import icu.bluedream.gamesetup.core.constant.GameType.*
import icu.bluedream.gamesetup.core.constant.InstallationAction
import java.io.File

/**
 * 设定游戏文件在文件系统中的名字
 * @param type 游戏类型
 * @return 游戏文件名
 */
actual fun getGameFinderName(type: GameType): String = when(type) {
    GenshinCN -> "原神.app"
    GenshinOS -> "Genshin Impact.app"
    ZzzCN -> "绝区零.app"
    ZzzOS -> "Zenless Zone Zero.app"
}

/**
 * 获取预设的软件包
 * @param type 游戏类型
 * @return 存在于 /resource 的软件包（压缩）名
 */
actual fun getGameFileNameInner(type: GameType): String = when(type) {
    GenshinCN, GenshinOS -> "GS.app"
    ZzzCN, ZzzOS -> "ZZZ.app"
}

/**
 * 将内置的空包暴露到Application目录下，并判断安装方式
 * @param innerGameName 内部包名字
 * @param outerGameName 国际化的外部包名字
 * @return 安装动作类型
 */
actual fun exposeGame2Application(innerGameName: String, outerGameName: String): InstallationAction {
    val resource = ClassPathResource(innerGameName)
    val appInSystem = File(MAC_APPLICATION_PATH, outerGameName)
    fun unzip() {
        appInSystem.mkdir()
        println(resource.url)
        unzipInnerFile(resource.stream, appInSystem)
    }
    if (appInSystem.exists()) {
        appInSystem.delete()
        return InstallationAction.UpdateExisting
    }
    unzip()
    return InstallationAction.InstallBrandNew
}

actual fun modifyLaunchBat(type: GameType, gamePkgsBaseDir: String) {
    val launchBatPath = "$MAC_APPLICATION_PATH${getGameFinderName(type)}/$WINE_DRIVE_C_PATH$LAUNCH_BAT"
    val launchString = "\"$WINE_MAC_ROOT_PATH$gamePkgsBaseDir${getExeFileName(type)}\"" + if (isGS(type)) " $GS_CLOUD_GAME_PARAM" else ""
    val launchBatFile = File(launchBatPath)
    launchBatFile.writeText(launchString)
}

private fun getExeFileName(type: GameType) = when(type) {
    GenshinCN, GenshinOS -> GS_EXE
    ZzzCN, ZzzOS -> ZZZ_EXE
}

private fun isGS(type: GameType) = type == GenshinCN || type == GenshinOS