package icu.bluedream.gamesetup.core.util

import kotlinx.serialization.json.Json
import java.io.*
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLConnection

@Throws(Exception::class)
actual inline fun <reified T> URL.fetch(): T {
    val connection = this.openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    val responseCode = connection.responseCode
    if (responseCode == HttpURLConnection.HTTP_OK) {
        val `is` = connection.inputStream
        val reader = BufferedReader(InputStreamReader(`is`))
        val data = reader.use { it.readLine() }
        println(data)
        return Json.decodeFromString<T>(data)
    } else {
        throw Exception("Something went wrong when while we are fetching package info, ${connection.responseMessage}")
    }
}

actual fun downloadPkg(
    pkgUrl: String, destPath: String, pkgCount: Int,
    failLog: (String) -> Unit, addSplitZip: (String) -> Unit,
    initPkgZipFileName: (String) -> Unit, checkExist: () -> Boolean,
    checkShouldDownload: (String, Int) -> Boolean,
    printDownloadProgress: (Long, Long, String, Float) -> Unit
): Boolean {
    val url = URI.create(pkgUrl).toURL()
    val connection = url.openConnection()
    connection.connect()
    var startTime: Long
    var currentTime: Long
    var lastDownloadSize = 0L
    var downloadedSize: Long = 0
    // 获取文件名
    val fileName = getFileName(pkgUrl, connection)
    if (fileName.isEmpty()) {
        failLog("无法获取下载文件名")
        return false
    }
    addSplitZip(fileName)
    initPkgZipFileName(fileName)
    if (checkExist()) { return true } //已存在待解压的合并包，不下载数据
    val destFilePath = destPath + fileName
    if (!checkShouldDownload(fileName, pkgCount)) {
        return true
    }
    val fileSize = connection.contentLengthLong
    failLog("Size: ${fileSize / 1024 / 1024 / 1024} GB")
    startTime = System.currentTimeMillis()
    val file = File(destFilePath)
    if (!file.exists()) { file.createNewFile() }
    BufferedInputStream(connection.getInputStream()).use { input ->
        FileOutputStream(destFilePath).use { output ->
            val buffer = ByteArray(1024 * 32)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                downloadedSize += bytesRead
                currentTime = System.currentTimeMillis()
                if (currentTime - startTime >= 1000L) {
                    printDownloadProgress(downloadedSize, fileSize, fileName, (downloadedSize - lastDownloadSize) / 1024f / 1024f)
                    startTime = System.currentTimeMillis()
                    lastDownloadSize = downloadedSize
                }
            }
        }
    }
    val verifyDownload = checkShouldDownload(fileName, pkgCount)
    if (verifyDownload) {
        File(destFilePath).delete()
    } else {
        failLog("Download finished, saved to path: $destFilePath")
    }
    return !verifyDownload
}

// 从 URL 和连接中获取文件名
private fun getFileName(url: String, connection: URLConnection): String {
    var fileName = ""
    // 从 URL 中获取文件名
    val urlPath = URI.create(url).toURL().path
    val slashIndex = urlPath.lastIndexOf('/')
    if (slashIndex >= 0 && slashIndex < urlPath.length - 1) {
        fileName = urlPath.substring(slashIndex + 1)
    }
    // 如果获取不到，从连接中获取 Content-Disposition 中的文件名
    if (fileName.isEmpty()) {
        val disposition = connection.getHeaderField("Content-Disposition")
        if (disposition != null && disposition.contains("filename=")) {
            fileName = disposition.substring(disposition.indexOf("filename=") + 9)
            fileName = fileName.replace("\"", "")
        }
    }
    return fileName
}