package icu.bluedream.gamesetup.core.util

import icu.bluedream.gamesetup.core.constant.GameType
import icu.bluedream.gamesetup.core.constant.InstallationAction


/**
 * 设定游戏文件在文件系统中的名字
 * @param type 游戏类型
 * @return 游戏文件名
 */
expect fun getGameFinderName(type: GameType): String

/**
 * 获取预设的软件包
 * @param type 游戏类型
 * @return 存在于 /resource 的软件包（压缩）名
 */
expect fun getGameFileNameInner(type: GameType): String

/**
 * 将内置的空包暴露到Application目录下，并判断安装方式
 * @param innerGameName 内部包名字
 * @param outerGameName 国际化的外部包名字
 * @return 安装动作类型
 */
expect fun exposeGame2Application(innerGameName: String, outerGameName: String): InstallationAction
expect fun modifyLaunchBat(type: GameType, gamePkgsBaseDir: String)