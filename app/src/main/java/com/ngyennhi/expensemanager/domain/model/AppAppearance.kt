package com.ngyennhi.expensemanager.domain.model

enum class AppLanguage(val tag: String) {
    SYSTEM(""), VIETNAMESE("vi"), ENGLISH("en");

    companion object {
        fun fromTag(tag: String): AppLanguage = entries.firstOrNull { it.tag == tag } ?: SYSTEM
    }
}

enum class AppTheme {
    SYSTEM, LIGHT, DARK;

    fun isDark(systemIsDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemIsDark
        LIGHT -> false
        DARK -> true
    }

    companion object {
        fun fromStored(value: String?): AppTheme = entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}
