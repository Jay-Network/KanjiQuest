package com.jworks.kanjiquest.core.domain.model

data class Subscription(
    val userId: String,
    val appId: String = "kanjiquest",
    val plan: SubscriptionPlan = SubscriptionPlan.FREE,
    val status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
    val cancelAtPeriodEnd: Boolean = false
)

enum class SubscriptionPlan(val value: String) {
    FREE("free"),
    PREMIUM("premium");

    companion object {
        fun fromString(value: String): SubscriptionPlan =
            entries.find { it.value == value } ?: FREE
    }
}

enum class SubscriptionStatus(val value: String) {
    ACTIVE("active"),
    PAST_DUE("past_due"),
    CANCELED("canceled"),
    TRIALING("trialing");

    companion object {
        fun fromString(value: String): SubscriptionStatus =
            entries.find { it.value == value } ?: ACTIVE
    }
}
