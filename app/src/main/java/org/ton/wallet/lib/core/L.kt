package org.ton.wallet.lib.core

import android.text.TextUtils
import android.util.Log
import org.ton.wallet.lib.core.ext.threadLocal
import java.io.PrintWriter
import java.io.StringWriter

object L {

    private val stringBuilder = threadLocal { StringBuilder() }
    private val targets = arrayListOf<LogTarget>()

    private lateinit var logcatTarget: LogcatTarget

    fun init() {
        logcatTarget = LogcatTarget()
        logcatTarget.isEnabled = true
        targets.add(logcatTarget)
    }

    fun v(vararg args: Any) {
        v(null, *args)
    }

    fun v(throwable: Throwable?, vararg args: Any) {
        log(Log.VERBOSE, null, throwable, *args)
    }

    fun d(vararg args: Any) {
        d(null, *args)
    }

    fun d(throwable: Throwable?, vararg args: Any) {
        log(Log.DEBUG, null, throwable, *args)
    }

    fun i(vararg args: Any) {
        i(null, *args)
    }

    fun i(throwable: Throwable?, vararg args: Any) {
        log(Log.INFO, null, throwable, *args)
    }

    fun w(vararg args: Any?) {
        w(null, *args)
    }

    fun w(throwable: Throwable?, vararg args: Any?) {
        log(Log.WARN, null, throwable, *args)
    }

    fun e(message: String) {
        e(null, message)
    }

    fun e(throwable: Throwable?) {
        e(throwable, null)
    }

    fun e(tag: String?, throwable: Throwable?) {
        e(tag, throwable, null)
    }

    fun e(throwable: Throwable?, vararg args: Any?) {
        e(null as? String?, throwable, *args)
    }

    fun e(tag: String?, throwable: Throwable?, vararg args: Any?) {
        log(Log.ERROR, tag, throwable, *args)
    }


    // --- private ---
    private fun log(priority: Int, tag: String?, throwable: Throwable?, vararg args: Any?) {
        var isNeedLogs = false
        for (i in 0 until targets.size) {
            isNeedLogs = targets[i].isEnabled
            if (isNeedLogs) {
                break
            }
        }
        if (!isNeedLogs) {
            return
        }

        val sb = stringBuilder.get()!!

        // prepare tag
        val resultTag = if (tag == null) {
            val loggerClassName = L::class.java.name
            val thread = Thread.currentThread()
            val element = traceThread(thread, loggerClassName)
            val methodName = element?.methodName ?: "unknown"
            val lineNumber = element?.lineNumber ?: 0
            sb.clear()
                .append(element?.className?.substringAfterLast('.') ?: loggerClassName)
                .append('.')
                .append(methodName)
                .append(':')
                .append(lineNumber)
                .toString()
        } else {
            tag
        }

        // prepare message
        sb.clear()
        for (arg in args) {
            sb.append(arg ?: "null").append(' ')
        }
        if (throwable != null) {
            val stackTraceString = getStackTraceString(throwable)
            if (!TextUtils.isEmpty(stackTraceString)) {
                sb.append('\n').append(stackTraceString)
            }
        }
        val msg = sb.toString()
        for (i in 0 until targets.size) {
            targets[i].log(priority, resultTag, msg)
        }
    }

    private fun getStackTraceString(throwable: Throwable?): String {
        if (throwable == null) {
            return ""
        }
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        pw.flush()
        return sw.toString()
    }

    private fun traceThread(thread: Thread, className: String): StackTraceElement? {
        val elements = thread.stackTrace
        var isFound = false
        for (i in elements.indices) {
            if (elements[i].className == className) {
                isFound = true
            }
            if (isFound && elements[i].className != className) {
                return elements[i]
            }
        }
        return null
    }


    internal interface LogTarget {

        var isEnabled: Boolean

        fun log(priority: Int, tag: String, msg: String)
    }

    internal class LogcatTarget : LogTarget {

        override var isEnabled: Boolean = false

        override fun log(priority: Int, tag: String, msg: String) {
            if (isEnabled) {
                Log.println(priority, tag, msg)
            }
        }
    }
}