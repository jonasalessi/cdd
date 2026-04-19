package com.cdd.ui.settings.tools.cdd

import com.intellij.util.ui.ColumnInfo

data class CddRuleItem(var pattern: String, var limit: Int)
data class CddWeightItem(var pattern: String, var weights: MutableMap<String, Double>)

class PatternColumn : ColumnInfo<CddRuleItem, String>("File Pattern (Regex)") {
    override fun valueOf(item: CddRuleItem): String = item.pattern
    override fun setValue(item: CddRuleItem, value: String) {
        item.pattern = value
    }

    override fun isCellEditable(item: CddRuleItem): Boolean = true
}

class LimitColumn : ColumnInfo<CddRuleItem, Int>("ICP Limit") {
    override fun valueOf(item: CddRuleItem): Int = item.limit
    override fun setValue(item: CddRuleItem, value: Int) {
        item.limit = value
    }

    override fun isCellEditable(item: CddRuleItem): Boolean = true
    override fun getColumnClass(): Class<*> = Int::class.javaObjectType
}

class WeightPatternColumn : ColumnInfo<CddWeightItem, String>("File Pattern (Regex)") {
    override fun valueOf(item: CddWeightItem): String = item.pattern
    override fun setValue(item: CddWeightItem, value: String) {
        item.pattern = value
    }

    override fun isCellEditable(item: CddWeightItem): Boolean = true
}

class WeightColumn(name: String, private val metricKey: String) : ColumnInfo<CddWeightItem, Double>(name) {
    override fun valueOf(item: CddWeightItem): Double = item.weights[metricKey] ?: 1.0
    override fun setValue(item: CddWeightItem, value: Double) {
        item.weights[metricKey] = value
    }

    override fun isCellEditable(item: CddWeightItem): Boolean = true
    override fun getColumnClass(): Class<*> = Double::class.javaObjectType
}
