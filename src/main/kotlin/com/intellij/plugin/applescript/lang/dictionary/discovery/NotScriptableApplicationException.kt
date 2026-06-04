package com.intellij.plugin.applescript.lang.dictionary.discovery

class NotScriptableApplicationException(
    val applicationName: String,
    message: String,
) : Throwable(message)
