package com.intellij.plugin.applescript.lang.ide.sdef

class NotScriptableApplicationException : Throwable {

    private val applicationName: String

    constructor(applicationName: String) : super() {
        this.applicationName = applicationName
    }

    constructor(applicationName: String, message: String) : super(message) {
        this.applicationName = applicationName
    }

    fun getApplicationName(): String = applicationName
}
