package icu.bluedream.gamesetup.ui.viewmodel

import androidx.compose.runtime.mutableStateOf
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import icu.bluedream.gamesetup.core.constant.Constants
import icu.bluedream.gamesetup.core.constant.GameType
import icu.bluedream.gamesetup.core.constant.InstallationAction
import icu.bluedream.gamesetup.core.constant.InstallationAction.*
import icu.bluedream.gamesetup.core.data.GameManifest
import icu.bluedream.gamesetup.core.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI

class HomeScreenViewModel : ScreenModel {
    var showGameType = mutableStateOf(false)
    var selectedGame = mutableStateOf(GameType.GenshinCN)
    var selectedDir = mutableStateOf("")
    var installLog = mutableStateOf("")
    private var isPathInstalledGame = mutableStateOf(false)

    private var installDir = ""

    fun startChoosingPath() {
        screenModelScope.launch {
            val selected = chooseDir()
            if (selected.startsWith("e")) {
                isPathInstalledGame.value = false
            } else {
                val pathList = File(selected).list()
                if (pathList != null) {
                    isPathInstalledGame.value = pathList.contains("YuanShen.exe")
                    installDir = if (isPathInstalledGame.value) {
                        selected
                    } else {
                        File("${selected}/HoYoGamePacks/").mkdir()
                        "${selected}/HoYoGamePacks/"
                    }
                } else {
                    isPathInstalledGame.value = false
                    File("${selected}/HoYoGamePacks/").mkdir()
                    installDir = "${selected}/HoYoGamePacks/"
                }
            }
            withContext(Dispatchers.Main) {
                selectedDir.value = selected
            }
        }
    }

    fun startTask() {
        screenModelScope.launch(Dispatchers.Main) {
            var canDoNext = 0
            var packageInfo: GameManifest? = null
            var pkgCount = 0
            val jobFetchInfo = launch(Dispatchers.IO) {
                try {
                    writeLog("We will install the game ${selectedGame.value.name} to your Mac.")
                    val url = URI.create(Constants.getGameURL(selectedGame.value)).toURL()
                    packageInfo = url.fetch<GameManifest>()
                    canDoNext += 1
                    writeLog(packageInfo?.message.toString())
                } catch (e: Exception) {
                    writeLog(e.message ?: "Unknown message gotten from process: jobFetchInfo().")
                    canDoNext = 0
                }
            }
            jobFetchInfo.join()
            val jobInstallApp = launch(Dispatchers.IO) {
                val gameName = getGameFinderName(selectedGame.value)
                val gameInnerName = getGameFileNameInner(selectedGame.value)
                writeLog("We are now detecting your environment, please wait...")
                writeLog("This may take a while.")
                val type = exposeGame2Application(gameInnerName, gameName)
                when(type) {
                    InstallBrandNew -> writeLog("We will do a brand-new installation.")
                    UpdateExisting -> writeLog("We have detected an existing app, then we will update it.")
                }
            }
            jobInstallApp.join()
            val jobDownloadPackages = launch(Dispatchers.IO) {
                val pkgUrls = mutableListOf<String>()
                val splitPkgZipFileName = mutableListOf<String>()
                val pkgMd5s = mutableListOf<String>()
                var pkgZipFileName: String? = null
                fun getGameTotalSize(): Long {
                    var size = 0L
                    for (pkg in packageInfo!!.data.gamePackages[0].main.major!!.gamePkgs) {
                        size += pkg.size.toLong()
                        pkgUrls.add(pkg.url)
                        pkgMd5s.add(pkg.md5)
                    }
                    return size
                }
                fun checkFile(pkgName: String, pkgCount: Int): Boolean {
                    val targetFile = File(installDir + pkgName)
                    if (!targetFile.exists()) {
                        return true
                    }
                    writeLog("\n正在校验资源文件：$pkgName\n")
                    val targetMd5 = calculateMD5(targetFile)
                    if (targetMd5 != pkgMd5s[pkgCount - 1]) {
                        targetFile.delete()
                        return true
                    }
                    return false
                }
                writeLog(
                    "We will download the game data packets, which size is ${getGameTotalSize() / 1024 / 1024 / 1024} GB," +
                            "Please reserve space at least twice the size of the data packets to install the game normally."
                )
                mkdir(installDir)
                writeLog("We will download packets at: $installDir")
                for (url in pkgUrls) {
                    pkgCount++
                    writeLog(" Download $pkgCount of ${pkgUrls.size}...")
                    while (!downloadPkg(
                            url, installDir, pkgCount,
                            failLog = { er -> writeLog(er) },
                            addSplitZip = { spName ->
                                if (!splitPkgZipFileName.contains(spName)) {
                                    splitPkgZipFileName.add(spName)
                                }
                            },
                            initPkgZipFileName = { name ->
                                val lastDotIndex = name.lastIndexOf(".")
                                if (pkgZipFileName.isNullOrEmpty()) {
                                    pkgZipFileName = name.substring(0, lastDotIndex)
                                    writeLog("Downloading: $pkgZipFileName")
                                }
                            },
                            checkExist = {
                                File(installDir + pkgZipFileName).exists()
                                //已存在待解压的合并包，不下载数据
                            },
                            checkShouldDownload = { s: String, i: Int -> checkFile(s, i) },
                            printDownloadProgress = { downloadedSize, fileSize, fileName, current ->
                                writeDownloadProgress(downloadedSize, fileSize, fileName, current)
                            }
                        )
                    ) {
                        writeLog("Failed to download, retry in 2s")
                        Thread.sleep(2000)
                    }
                }
                writeLog("All files have been downloaded and are preparing to decompress.")
                unZipGamePkgs(installDir, isPathInstalledGame.value, { writeLog(it) }, splitPkgZipFileName, installDir, pkgZipFileName)
                modifyLaunchBat(selectedGame.value, installDir)
            }
            jobDownloadPackages.join()
            withContext(Dispatchers.Main) {
                writeLog("Launching args have been set, now you can start your journey.")
            }
        }
    }

    private fun writeLog(msg: String) {
        installLog.value = "${installLog.value}\n\n$msg"
    }

    private fun writeDownloadProgress(downloaded: Long, total: Long, fileName: String, speed: Float) {
        val lines = installLog.value.split(Regex("\\r?\\n|\\r"))
        val progress = (downloaded.toFloat() / total * 100).toInt()
        if (lines.isNotEmpty()) {
            val moddedLine = lines.toMutableList()
            var lastLine = moddedLine[lines.lastIndex]
            if (lastLine.startsWith("File:")) {
                lastLine = "File: $fileName, Progress: $progress%, Speed: $speed MB/s"
                moddedLine[lines.lastIndex] = lastLine
                installLog.value = moddedLine.joinToString(System.lineSeparator())
            } else {
                writeLog("File: $fileName, Progress: $progress%, Speed: $speed MB/s")
            }
        } else {
            writeLog("File: $fileName, Progress: $progress%, Speed: $speed MB/s")
        }
    }
}