package icu.bluedream.gamesetup.core.util

import java.io.File
import java.io.InputStream

expect suspend fun chooseDir(): String
expect fun unzipInnerFile(inputStream: InputStream, outputDir: File)
expect fun mkdir(path: String)
expect fun calculateMD5(file: File): String
expect fun unZipGamePkgs(
    destDir: String,
    isPathInstalledGame: Boolean,
    write: (String) -> Unit,
    splitPkgZipFileName: List<String>,
    gamePkgsBaseDir: String,
    pkgZipFileName: String?
)