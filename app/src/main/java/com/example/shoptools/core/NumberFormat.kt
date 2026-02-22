package com.example.shoptools.core

import java.util.Locale

/** 小数第2位で四捨五入して文字列に変換する */
fun formatUnitPrice(value: Double, useDigitSeparator: Boolean = false): String {
    // String.format delegates to the JVM's printf, which uses HALF_UP for %f format.
    // Both branches use String.format so rounding is always consistent.
    return if (useDigitSeparator) {
        String.format(Locale.JAPAN, "%,.2f", value)
    } else {
        String.format(Locale.US, "%.2f", value)
    }
}

/** 整数の金額を桁区切りで表示する */
fun formatAmount(value: Int, useDigitSeparator: Boolean = false): String {
    return if (useDigitSeparator) {
        String.format(Locale.JAPAN, "%,d", value)
    } else {
        value.toString()
    }
}
