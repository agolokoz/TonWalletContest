package org.ton.wallet.util

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

abstract class PrefDelegate<T>(
    protected val prefs: SharedPreferences,
    private val name: String,
    private val default: T,
    private val flow: MutableStateFlow<T>? = null,
    isInitDefault: Boolean = true
) : ReadWriteProperty<Any?, T> {

    private var actualValue: T = default
    private var isValueInitialized: Boolean = false

    init {
        if (isInitDefault) {
            flow?.tryEmit(get())
        }
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return get()
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        actualValue = value
        isValueInitialized = true
        setToPreferences(name, value)
        flow?.tryEmit(value)
    }

    abstract fun getFromPreferences(name: String, default: T): T

    abstract fun setToPreferences(name: String, value: T)

    private fun get(): T {
        if (!isValueInitialized) {
            actualValue = getFromPreferences(name, default)
            isValueInitialized = true
        }
        return actualValue
    }
}

abstract class NullablePrefDelegate<T>(
    prefs: SharedPreferences,
    name: String,
    default: T? = null,
    flow: MutableStateFlow<T?>? = null
) : PrefDelegate<T?>(prefs, name, default, flow) {

    override fun getFromPreferences(name: String, default: T?): T? {
        return if (prefs.contains(name)) {
            getFromPreferencesNonNull(name)
        } else {
            default
        }
    }

    override fun setToPreferences(name: String, value: T?) {
        if (value == null) {
            prefs.edit().remove(name).apply()
        } else {
            setToPreferencesNonNull(name, value)
        }
    }

    abstract fun getFromPreferencesNonNull(name: String): T

    abstract fun setToPreferencesNonNull(name: String, value: T)
}

class BooleanPrefDelegate(
    prefs: SharedPreferences,
    name: String,
    default: Boolean,
    flow: MutableStateFlow<Boolean>? = null
) : PrefDelegate<Boolean>(prefs, name, default, flow) {

    override fun getFromPreferences(name: String, default: Boolean): Boolean {
        return prefs.getBoolean(name, default)
    }

    override fun setToPreferences(name: String, value: Boolean) {
        prefs.edit().putBoolean(name, value).apply()
    }
}

class EnumPrefDelegate<T : Enum<T>>(
    prefs: SharedPreferences,
    name: String,
    private val values: Array<T>,
    defValue: T = values[0],
    flow: MutableStateFlow<T>? = null
) : PrefDelegate<T>(prefs, name, defValue, flow, false) {

    override fun getFromPreferences(name: String, default: T): T {
        val value = prefs.getString(name, null)
        return values?.firstOrNull { it.name == value } ?: default
    }

    override fun setToPreferences(name: String, value: T) {
        prefs.edit().putString(name, value.name).apply()
    }
}

class LongPrefDelegate(
    prefs: SharedPreferences,
    name: String,
    default: Long,
    flow: MutableStateFlow<Long>? = null
) : PrefDelegate<Long>(prefs, name, default, flow) {

    override fun getFromPreferences(name: String, default: Long): Long {
        return prefs.getLong(name, default)
    }

    override fun setToPreferences(name: String, value: Long) {
        prefs.edit().putLong(name, value).apply()
    }
}

class LongNullablePrefDelegate(
    prefs: SharedPreferences,
    name: String,
    default: Long? = null,
    flow: MutableStateFlow<Long?>? = null
) : NullablePrefDelegate<Long>(prefs, name, default, flow) {

    override fun getFromPreferencesNonNull(name: String): Long {
        return prefs.getLong(name, 0)
    }

    override fun setToPreferencesNonNull(name: String, value: Long) {
        prefs.edit().putLong(name, value).apply()
    }
}

class StringPrefDelegate(
    prefs: SharedPreferences,
    name: String,
    default: String?,
    flow: MutableStateFlow<String?>? = null
) : PrefDelegate<String?>(prefs, name, default, flow) {

    override fun getFromPreferences(name: String, default: String?): String? {
        return prefs.getString(name, default)
    }

    override fun setToPreferences(name: String, value: String?) {
        prefs.edit().putString(name, value).apply()
    }
}