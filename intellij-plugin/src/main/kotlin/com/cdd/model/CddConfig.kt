package com.cdd.model

import com.cdd.CddConstants

data class CddConfig(
    var icpLimits: MutableMap<String, MutableMap<String, Int>> = mutableMapOf(
        CddConstants.LANGUAGE_JAVA to mutableMapOf(CddConstants.WILDCARD_ALL to 12),
        CddConstants.LANGUAGE_KOTLIN to mutableMapOf(CddConstants.WILDCARD_ALL to 12)
    ),
    var metrics: MutableMap<String, MutableMap<String, MutableMap<String, Double>>> = mutableMapOf(
        CddConstants.LANGUAGE_JAVA to mutableMapOf(
            CddConstants.WILDCARD_ALL to mutableMapOf(
                CddConstants.METRIC_CODE_BRANCH to 1.0,
                CddConstants.METRIC_CONDITION to 1.0,
                CddConstants.METRIC_INTERNAL_COUPLING to 1.0,
                CddConstants.METRIC_EXCEPTION_HANDLING to 1.0
            )
        ),
        CddConstants.LANGUAGE_KOTLIN to mutableMapOf(
            CddConstants.WILDCARD_ALL to mutableMapOf(
                CddConstants.METRIC_CODE_BRANCH to 1.0,
                CddConstants.METRIC_CONDITION to 1.0,
                CddConstants.METRIC_INTERNAL_COUPLING to 1.0,
                CddConstants.METRIC_EXCEPTION_HANDLING to 1.0
            )
        )
    ),
    var internalCoupling: InternalCouplingConfig = InternalCouplingConfig(),
    var include: MutableList<String> = mutableListOf(),
    var exclude: MutableList<String> = mutableListOf(),
    var sloc: SlocConfig = SlocConfig()
)

data class InternalCouplingConfig(
    var autoDetect: Boolean = true,
    var packages: MutableList<String> = mutableListOf()
)

data class SlocConfig(
    var methodLimit: Int = 24
)
