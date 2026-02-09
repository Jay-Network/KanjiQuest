package com.jworks.kanjiquest.core.domain.model

/**
 * A purchasable item in the J Coin shop.
 */
data class ShopItem(
    val id: String,
    val name: String,
    val description: String,
    val cost: Int,
    val category: ShopCategory,
    val contentId: String? = null, // For unlockable content (theme ID, game mode ID, etc.)
    val iconUrl: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Categories of items in the J Coin shop.
 */
enum class ShopCategory(val displayName: String) {
    THEME("Themes"),
    BOOSTER("Boosters"),
    UTILITY("Utilities"),
    CONTENT("Content"),
    CROSS_BUSINESS("Special Offers");

    companion object {
        fun fromString(value: String): ShopCategory {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: UTILITY
        }
    }
}

/**
 * Result of a purchase attempt.
 */
sealed class PurchaseResult {
    data class Success(
        val item: ShopItem,
        val newBalance: Long,
        val unlockId: String? = null
    ) : PurchaseResult()

    data class InsufficientFunds(
        val required: Int,
        val available: Long
    ) : PurchaseResult()

    data class AlreadyOwned(
        val item: ShopItem
    ) : PurchaseResult()

    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : PurchaseResult()
}

/**
 * An active booster (XP multiplier, etc.).
 */
data class ActiveBooster(
    val id: Long,
    val userId: String,
    val boosterType: BoosterType,
    val multiplier: Double,
    val activatedAt: Long,
    val expiresAt: Long
) {
    val isExpired: Boolean
        get() = kotlinx.datetime.Clock.System.now().epochSeconds > expiresAt

    val remainingSeconds: Long
        get() = maxOf(0, expiresAt - kotlinx.datetime.Clock.System.now().epochSeconds)
}

enum class BoosterType(val displayName: String, val defaultMultiplier: Double) {
    DOUBLE_XP("Double XP", 2.0),
    TRIPLE_XP("Triple XP", 3.0);

    companion object {
        fun fromString(value: String): BoosterType {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: DOUBLE_XP
        }
    }
}
