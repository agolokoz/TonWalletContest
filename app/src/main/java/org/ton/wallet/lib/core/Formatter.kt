package org.ton.wallet.lib.core

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import org.ton.lib.tonapi.TonCoin
import org.ton.wallet.R
import org.ton.wallet.lib.core.ext.hiddenMiddle
import org.ton.wallet.lib.core.ext.threadLocal
import org.ton.wallet.uikit.span.FontSpan
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.pow

object Formatter {

    val StringBuilder = threadLocal { StringBuilder() }

    private val decimalFormat by lazy { DecimalFormat.getInstance(locale) as DecimalFormat }
    private val locale by lazy { Locale.US }

    private val date = threadLocal { Date() }
    private val dayMonthFormat by lazy { SimpleDateFormat("MMMM d", locale) }
    private val fullDateFormat by lazy { SimpleDateFormat("MMM d, yyyy", locale) }
    private val timeFormat by lazy { SimpleDateFormat("HH:mm", locale) }

    val decimalSeparator: Char
        get() = decimalFormat.decimalFormatSymbols.decimalSeparator

    fun getBeautifiedAmount(amount: CharSequence?, proportion: Float = 0.73f): SpannableStringBuilder? {
        if (amount == null) {
            return null
        }
        val separatorPosition = amount.indexOf(decimalSeparator)
        if (separatorPosition == -1) {
            return SpannableStringBuilder(amount)
        }
        val spannableBuilder = SpannableStringBuilder(amount)
        spannableBuilder.setSpan(RelativeSizeSpan(proportion), separatorPosition, amount.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        return spannableBuilder
    }

    fun getFormattedAmount(amount: Long): String {
        val divider = 10.0.pow(TonCoin.Decimals.toDouble()).toLong()
        val integerPart = amount / divider
        val decimalPart = amount % divider
        if (decimalPart == 0L) {
            return String.format(locale, "%d", integerPart)
        }
        var pow = 10L
        var decimals = 9
        for (i in 9 downTo 1) {
            if (decimalPart % pow != 0L) {
                break
            }
            pow *= 10
            decimals--
        }

        val formatBuilder = StringBuilder.get()!!.clear()
            .append("%d.%0")
            .append(decimals)
            .append('d')
        return String.format(locale, formatBuilder.toString(), abs(integerPart), abs(decimalPart / (pow / 10)))
    }

    fun getFormattedAmount(balance: BigDecimal, currencySymbol: String): String {
        var value = balance.stripTrailingZeros()
        if (value.scale() > 0) {
            value = value.setScale(2, RoundingMode.HALF_UP)
        }

        val decimals = value.scale()
        val format = StringBuilder.get()!!.clear()
            .append("%s%.").append(decimals).append('f')
            .toString()
        return String.format(Res.getCurrentLocale(), format, currencySymbol, balance)
    }

    fun getShortAddress(address: String): String {
        return getShortAddressSafe(address)!!
    }

    fun getShortAddressSafe(address: String?): String? {
        return address?.hiddenMiddle(4, 4)
    }

    fun getMiddleAddress(address: String?): String? {
        return address?.hiddenMiddle(6, 7)
    }

    fun getShortHash(hash: String?): String? {
        return hash?.hiddenMiddle(6, 6)
    }

    fun getBeautifiedShortString(shortString: String): SpannableStringBuilder {
        return getBeautifiedShortStringSafe(shortString)!!
    }

    fun getBeautifiedShortStringSafe(shortString: String?): SpannableStringBuilder? {
        if (shortString.isNullOrEmpty()) {
            return null
        }
        val stringBuilder = SpannableStringBuilder(shortString)
        stringBuilder.setSpan(
            FontSpan(Res.font(R.font.roboto_regular)),
            shortString.indexOfFirst { it == '.' },
            shortString.indexOfLast { it == '.' } + 1,
            Spannable.SPAN_INCLUSIVE_INCLUSIVE
        )
        return stringBuilder
    }

    fun getTimeString(timestampMs: Long): String {
        val date = date.get()!!
        date.time = timestampMs
        return timeFormat.format(date)
    }

    fun getDayMonthString(timestampMs: Long): String {
        val date = date.get()!!
        date.time = timestampMs
        return dayMonthFormat.format(date)
    }

    fun getFullDateString(timestampMs: Long): String {
        val date = date.get()!!
        date.time = timestampMs
        return fullDateFormat.format(date)
    }
}