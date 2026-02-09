package com.jworks.kanjiquest.core.domain.model

enum class UserLevel(val displayName: String) {
    FREE("Free"),
    PREMIUM("Premium"),
    ADMIN("Admin");

    companion object {
        val ADMIN_EMAILS = listOf(
            "jayismocking@gmail.com",
            "jay@jworks.com",
            "jay@tutoringjay.com"
        )
    }
}
