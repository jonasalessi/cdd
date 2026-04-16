package com.cdd.settings

data class CddSettingsModel(
    val javaIcpLimits: MutableMap<String, Int> = mutableMapOf(),
    val kotlinIcpLimits: MutableMap<String, Int> = mutableMapOf(),
    val javaMetrics: MutableMap<String, MutableMap<String, Double>> = mutableMapOf(),
    val kotlinMetrics: MutableMap<String, MutableMap<String, Double>> = mutableMapOf(),
    var autoDetect: Boolean = true,
    val packages: MutableList<String> = mutableListOf(),
    val include: MutableList<String> = mutableListOf(),
    val exclude: MutableList<String> = mutableListOf(),
    var methodLimit: Int = 24
)
