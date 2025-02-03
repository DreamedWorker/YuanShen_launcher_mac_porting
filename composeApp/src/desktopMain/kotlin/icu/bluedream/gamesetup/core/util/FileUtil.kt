package icu.bluedream.gamesetup.core.util

import hoyogamesetup.composeapp.generated.resources.Res
import hoyogamesetup.composeapp.generated.resources.home_select_dir
import io.github.vinceglb.filekit.core.FileKit
import org.jetbrains.compose.resources.getString
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest


actual suspend fun chooseDir(): String {
    val dir = FileKit.pickDirectory(getString(Res.string.home_select_dir)) ?: return "eNull"
    return if (dir.file.canWrite()) {
        dir.file.absolutePath
    } else {
        "eCannotWrite"
    }
}

actual fun unzipInnerFile(inputStream: InputStream, outputDir: File) {
    FileOutputStream(outputDir).use { outputStream ->
        inputStream.copyTo(outputStream)
    }
//    fun newFile(destinationDir: File, zipEntry: ZipEntry): File {
//        val destFile = File(destinationDir, zipEntry.name)
//        val canonicalDestinationDirPath = destinationDir.canonicalPath
//        val canonicalDestFilePath = destFile.canonicalPath
//        if (!canonicalDestFilePath.startsWith(canonicalDestinationDirPath + File.separator)) {
//            throw IOException("Try to decompress to a non-subdirectory path !!!")
//        }
//        return destFile
//    }
//
//    ZipInputStream(inputStream).use { zis ->
//        var zipEntry: ZipEntry? = zis.nextEntry
//        while (zipEntry != null) {
//            val newFile = newFile(outputDir, zipEntry)
//            if (zipEntry.isDirectory) {
//                newFile.mkdirs()
//            } else {
//                newFile.parentFile?.mkdirs()
//                FileOutputStream(newFile).use { fos ->
//                    zis.copyTo(fos)
//                }
//            }
//            zipEntry = zis.nextEntry
//        }
//    }
}

actual fun mkdir(path: String) {
    val file = File(path)
    if (file.exists()) {
        file.mkdir()
    }
}

actual fun calculateMD5(file: File): String {
    val digest = MessageDigest.getInstance("MD5")
    val inputStream = FileInputStream(file)
    val buffer = ByteArray(8192)
    var read = 0
    while (inputStream.read(buffer).also { read = it } > 0) {
        digest.update(buffer, 0, read)
    }
    val md5sum = digest.digest()
    val hexString = StringBuilder()
    for (i in md5sum.indices) {
        hexString.append(String.format("%02x", md5sum[i]))
    }
    inputStream.close()
    return hexString.toString().uppercase()
}

actual fun unZipGamePkgs(
    destDir: String,
    isPathInstalledGame: Boolean,
    write: (String) -> Unit,
    splitPkgZipFileName: List<String>,
    gamePkgsBaseDir: String,
    pkgZipFileName: String?
) {
    if (isPathInstalledGame) {
        write("Successful installation！")
        return
    }
    val outputDirFile = File(destDir)
    if (!outputDirFile.exists()) {
        outputDirFile.mkdirs()
    }
    catAllSplitZipFiles(splitPkgZipFileName, gamePkgsBaseDir, write, pkgZipFileName)
    pkgZipFileName?.let {
        val unzipArgs = mutableListOf<String>()
        unzipArgs.add(it)
        unzipArgs.add("-d")
        unzipArgs.add(outputDirFile.absolutePath)
        val processBuilder = ProcessBuilder("unzip", *unzipArgs.toTypedArray())
            .directory(File(gamePkgsBaseDir))
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
        write("Decompressing the game file...")
        write("Please wait as it will take a while.")
        val process = processBuilder.start()
        val exitCode = process.waitFor()
        if (exitCode == 0) {
            write("Decompression completed")
            //解压完成后删除压缩包
            File(gamePkgsBaseDir + it).delete()
            write("Successful installation！")
        } else {
            write("An error occurred during the decompression process. Exit code: $exitCode")
        }
    }
}

private fun catAllSplitZipFiles(
    splitPkgZipFileName: List<String>,
    gamePkgsBaseDir: String,
    write: (String) -> Unit,
    pkgZipFileName: String?
) {
    val splitZipPkgs = mutableListOf<File>()
    for (name in splitPkgZipFileName) {
        val pkg = File(gamePkgsBaseDir + name)
        if (pkg.exists()) {
            splitZipPkgs.add(pkg)
        }
    }
    pkgZipFileName?.let {
        try {
            val destFile = File(gamePkgsBaseDir + it)
            if (splitZipPkgs.size == splitPkgZipFileName.size) {
                FileOutputStream(destFile).channel.use { outputChannel ->
                    for (splitPkg in splitZipPkgs) {
                        write("Merging resource file(s): ${splitPkg.name} ... ")
                        val inputChannel = FileInputStream(splitPkg).channel
                        outputChannel.transferFrom(inputChannel, outputChannel.size(), inputChannel.size())
                        inputChannel.close()
                    }
                    outputChannel.close()
                }
                for (splitPkg in splitZipPkgs) {
                    splitPkg.delete()
                }
            } else {
                if (splitZipPkgs.size != 0) {
                    write("Some of resource packages is missing, please download it again!")
                }
            }
        } catch (e: Exception) {
            write("Error merging files: ${e.message}")
        }
    }
}