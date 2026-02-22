package com.example.shoptools.feature.unitprice.domain

/**
 * 単価計算ドメインロジック
 * 単位は計算に使わず、内容量は小数OK
 * 単価 = 価格 ÷ (内容量 × 入数)
 */
object UnitPriceCalculator {
    /**
     * @param price 価格（円）> 0
     * @param quantity 内容量（小数OK）> 0
     * @param count 入数（整数）>= 1
     * @return 単価（Double）。丸めは呼び出し元で行う
     */
    fun calculate(price: Double, quantity: Double, count: Int): Double {
        require(price > 0) { "price must be > 0" }
        require(quantity > 0) { "quantity must be > 0" }
        require(count >= 1) { "count must be >= 1" }
        return price / (quantity * count)
    }
}
