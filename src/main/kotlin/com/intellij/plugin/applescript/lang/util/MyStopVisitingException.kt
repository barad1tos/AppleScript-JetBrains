package com.intellij.plugin.applescript.lang.util

import com.intellij.openapi.vfs.VirtualFile

class MyStopVisitingException : RuntimeException {

    val result: VirtualFile

    constructor(message: String, result: VirtualFile) : super(Throwable(message)) {
        this.result = result
    }

    constructor(result: VirtualFile) : super(Throwable("File found$result")) {
        this.result = result
    }
}
