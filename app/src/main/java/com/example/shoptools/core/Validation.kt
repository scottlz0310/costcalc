package com.example.shoptools.core

data class ValidationResult(val isValid: Boolean, val errorMessage: String = "")

fun validatePrice(input: String): ValidationResult {
    val value = input.toDoubleOrNull()
    return when {
        input.isBlank() -> ValidationResult(false, "価格を入力してください")
        value == null -> ValidationResult(false, "数値を入力してください")
        value <= 0 -> ValidationResult(false, "0より大きい値を入力してください")
        else -> ValidationResult(true)
    }
}

fun validateQuantity(input: String): ValidationResult {
    val value = input.toDoubleOrNull()
    return when {
        input.isBlank() -> ValidationResult(false, "内容量を入力してください")
        value == null -> ValidationResult(false, "数値を入力してください")
        value <= 0 -> ValidationResult(false, "0より大きい値を入力してください")
        else -> ValidationResult(true)
    }
}

fun validateCount(input: String): ValidationResult {
    val value = input.toIntOrNull()
    return when {
        input.isBlank() -> ValidationResult(false, "入数を入力してください")
        value == null -> ValidationResult(false, "整数を入力してください")
        value < 1 -> ValidationResult(false, "1以上の値を入力してください")
        else -> ValidationResult(true)
    }
}

fun validateDenomination(input: String): ValidationResult {
    val value = input.toIntOrNull()
    return when {
        input.isBlank() -> ValidationResult(false, "額面を入力してください")
        value == null -> ValidationResult(false, "整数を入力してください")
        value <= 0 -> ValidationResult(false, "0より大きい値を入力してください")
        else -> ValidationResult(true)
    }
}

fun validateStock(input: String): ValidationResult {
    val value = input.toIntOrNull()
    return when {
        input.isBlank() -> ValidationResult(false, "枚数を入力してください")
        value == null -> ValidationResult(false, "整数を入力してください")
        value < 0 -> ValidationResult(false, "0以上の値を入力してください")
        else -> ValidationResult(true)
    }
}

fun validateTarget(input: String): ValidationResult {
    val value = input.toIntOrNull()
    return when {
        input.isBlank() -> ValidationResult(false, "目標金額を入力してください")
        value == null -> ValidationResult(false, "整数を入力してください")
        value <= 0 -> ValidationResult(false, "0より大きい値を入力してください")
        else -> ValidationResult(true)
    }
}
