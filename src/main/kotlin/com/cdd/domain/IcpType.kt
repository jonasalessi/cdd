package com.cdd.domain

import kotlinx.serialization.Serializable

/**
 * ICP Types as defined in the CDD methodology.
 */
@Serializable
enum class IcpType(val defaultWeight: Double) {
    CODE_BRANCH(1.0),
    CONDITION(1.0),
    EXCEPTION_HANDLING(1.0),
    INTERNAL_COUPLING(1.0),
    EXTERNAL_COUPLING(0.5)
}
