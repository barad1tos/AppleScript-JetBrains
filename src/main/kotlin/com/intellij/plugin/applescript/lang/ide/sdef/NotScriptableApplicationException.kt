package com.intellij.plugin.applescript.lang.ide.sdef

class NotScriptableApplicationException(
    val applicationName: String,
    message: String,
) : Throwable(message)
