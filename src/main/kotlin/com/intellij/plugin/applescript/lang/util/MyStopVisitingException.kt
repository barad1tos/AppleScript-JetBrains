package com.intellij.plugin.applescript.lang.util

import com.intellij.openapi.vfs.VirtualFile

class MyStopVisitingException(
    val result: VirtualFile,
) : RuntimeException(Throwable("File found$result"))
