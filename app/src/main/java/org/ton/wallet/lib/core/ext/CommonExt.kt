package org.ton.wallet.lib.core.ext

import java.lang.ref.WeakReference

fun <T> threadLocal(initializer: () -> T): ThreadLocal<T> {
    return object : ThreadLocal<T>() {
        override fun initialValue(): T {
            return initializer.invoke()
        }
    }
}

fun <T> weak(value: T) = WeakReference(value)