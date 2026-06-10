package com.finora.data.local

import com.finora.data.local.entity.CategoryEntity
import com.finora.domain.model.TransactionType

/** Default categories seeded on first launch. */
object DefaultData {

    /** Income category that interest payouts are booked to. */
    const val CAPITALIZATION_CATEGORY = "Капитализация"

    fun categories(): List<CategoryEntity> = listOf(
        // Expenses
        cat("Продукты", "cart", 0xFF3FB18C),
        cat("Кафе и рестораны", "restaurant", 0xFFE17055),
        cat("Транспорт", "car", 0xFF0984E3),
        cat("Жильё", "home", 0xFF6C5CE7),
        cat("Здоровье", "health", 0xFFE84393),
        cat("Развлечения", "movie", 0xFFFDCB6E),
        cat("Покупки", "shopping", 0xFFE84393),
        cat("Связь", "phone", 0xFF00CEC9),
        cat("Образование", "school", 0xFF6C5CE7),
        cat("Путешествия", "flight", 0xFF0984E3),
        cat("Подписки", "subscription", 0xFFA29BFE),
        cat("Игры", "games", 0xFF6C5CE7),
        cat("Питомцы", "pets", 0xFFFAB1A0),
        cat("Счета и платежи", "bills", 0xFF74B9FF),
        cat("Подарки", "gift", 0xFFE84393),
        cat("Прочее", "category", 0xFFB2BEC3),
        // Income
        cat("Зарплата", "salary", 0xFF3FB18C, TransactionType.INCOME),
        cat("Подработка", "work", 0xFF00CEC9, TransactionType.INCOME),
        cat("Подарок", "gift", 0xFFE84393, TransactionType.INCOME),
        cat("Инвестиции", "invest", 0xFFFDCB6E, TransactionType.INCOME),
        cat("Кэшбэк", "savings", 0xFF00CEC9, TransactionType.INCOME),
        cat(CAPITALIZATION_CATEGORY, "percent", 0xFF55EFC4, TransactionType.INCOME),
        cat("Прочее", "category", 0xFFB2BEC3, TransactionType.INCOME)
    )

    private fun cat(
        name: String,
        icon: String,
        color: Long,
        type: TransactionType = TransactionType.EXPENSE
    ) = CategoryEntity(
        name = name,
        type = type.name,
        iconKey = icon,
        color = color,
        isDefault = true
    )
}
