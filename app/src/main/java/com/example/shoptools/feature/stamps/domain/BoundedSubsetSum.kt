package com.example.shoptools.feature.stamps.domain

/**
 * 切手在庫の組み合わせを計算する
 * 二進分解 + 0/1 DP + 構成復元
 */
data class StampItem(val denomination: Int, val count: Int)
data class StampCombination(
    val total: Int,
    val diff: Int,  // abs(total - target), 0 if exact
    val pieces: Int,
    val composition: Map<Int, Int>,  // denomination -> count used
)

object BoundedSubsetSum {
    /**
     * @param inventory 在庫リスト
     * @param target 目標金額
     * @return Triple(exact, underList, overList) - under/overはdiff昇順・枚数少ない順 top3
     */
    fun solve(
        inventory: List<StampItem>,
        target: Int,
    ): Triple<StampCombination?, List<StampCombination>, List<StampCombination>> {
        if (target <= 0) return Triple(null, emptyList(), emptyList())

        // 二進分解: 各額面×枚数を1/2/4...に分解して0/1ナップサック問題に変換
        data class Item(val denomination: Int, val originalDenomination: Int, val multiplier: Int)
        val items = mutableListOf<Item>()
        for (stamp in inventory) {
            if (stamp.denomination <= 0 || stamp.count <= 0) continue
            var remaining = stamp.count
            var k = 1
            while (remaining > 0) {
                val take = minOf(k, remaining)
                items.add(Item(stamp.denomination * take, stamp.denomination, take))
                remaining -= take
                k *= 2
            }
        }

        if (items.isEmpty()) return Triple(null, emptyList(), emptyList())

        // DPの上限: target + 最大単価 (over候補のため少し超えてもOK)
        val maxDenom = inventory.maxOfOrNull { it.denomination } ?: 0
        val dpMax = target + maxDenom

        // dp[amount] = reachability + traceback配列
        val reachable = BooleanArray(dpMax + 1) { false }
        reachable[0] = true
        val from = Array(dpMax + 1) { -1 }  // -1 = no item used
        val itemUsed = Array(dpMax + 1) { -1 } // which item index was last used

        for ((idx, item) in items.withIndex()) {
            // 0/1 DP: 降順で処理
            for (j in dpMax downTo item.denomination) {
                if (reachable[j - item.denomination] && !reachable[j]) {
                    reachable[j] = true
                    from[j] = j - item.denomination
                    itemUsed[j] = idx
                }
            }
        }

        // 構成を復元する関数
        fun reconstruct(amount: Int): Map<Int, Int> {
            val comp = mutableMapOf<Int, Int>()
            var cur = amount
            while (cur > 0 && itemUsed[cur] != -1) {
                val idx = itemUsed[cur]
                val item = items[idx]
                comp[item.originalDenomination] = (comp[item.originalDenomination] ?: 0) + item.multiplier
                cur = from[cur]
            }
            return comp
        }

        fun makeCombo(amount: Int): StampCombination {
            val comp = reconstruct(amount)
            val pieces = comp.values.sum()
            return StampCombination(amount, Math.abs(amount - target), pieces, comp)
        }

        val comparator = compareBy<StampCombination> { it.diff }.thenBy { it.pieces }

        var exact: StampCombination? = null
        val underList = mutableListOf<StampCombination>()
        val overList = mutableListOf<StampCombination>()

        for (amount in 1..dpMax) {
            if (!reachable[amount]) continue
            when {
                amount == target -> exact = makeCombo(amount)
                amount < target -> underList.add(makeCombo(amount))
                amount > target -> overList.add(makeCombo(amount))
            }
        }

        underList.sortWith(comparator)
        overList.sortWith(comparator)

        return Triple(exact, underList.take(3), overList.take(3))
    }

    /** 構成を "84x1, 63x2" のような文字列に変換 */
    fun compositionToString(comp: Map<Int, Int>): String {
        return comp.entries
            .sortedByDescending { it.key }
            .joinToString(", ") { (denom, cnt) -> "${denom}x${cnt}" }
    }
}
