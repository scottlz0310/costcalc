package com.example.shoptools.core

import java.text.NumberFormat
import java.util.Locale

/** 小数第2位で四捨五入して文字列に変換する */
fun formatUnitPrice(value: Double, useDigitSeparator: Boolean = false): String {
    return if (useDigitSeparator) {
        val nf = NumberFormat.getNumberInstance(Locale.JAPAN).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        nf.format(value)
    } else {
        String.format(Locale.US, "%.2f", value)
    }
}

/** 整数の金額を桁区切りで表示する */
fun formatAmount(value: Int, useDigitSeparator: Boolean = false): String {
    return if (useDigitSeparator) {
        NumberFormat.getIntegerInstance(Locale.JAPAN).format(value)
    } else {
        value.toString()
    }
}
