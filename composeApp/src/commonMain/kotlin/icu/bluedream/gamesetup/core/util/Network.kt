package icu.bluedream.gamesetup.core.util

import java.net.URL

expect inline fun <reified T> URL.fetch(): T
expect fun downloadPkg(
    pkgUrl: String,
    destPath: String,
    pkgCount: Int,
    failLog: (String) -> Unit,
    addSplitZip: (String) -> Unit,
    initPkgZipFileName: (String) -> Unit,
    checkExist: () -> Boolean,
    checkShouldDownload: (String, Int) -> Boolean,
    printDownloadProgress: (Long, Long, String, Float) -> Unit
): Boolean