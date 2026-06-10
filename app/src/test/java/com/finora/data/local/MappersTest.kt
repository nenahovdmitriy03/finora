package com.finora.data.local

import com.finora.data.local.entity.*
import com.finora.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class MappersTest {

    // ─── Account roundtrip ───────────────────────────────────────────────

    @Test
    fun `Account to Entity and back preserves all fields`() {
        val original = Account(
            id = 42,
            name = "Тинькофф",
            type = AccountType.SAVINGS,
            initialBalance = 150_000.0,
            color = 0xFF6C5CE7,
            iconKey = "card",
            createdAt = 1000L,
            interestRate = 16.0,
            interestPeriod = InterestPeriod.MONTHLY,
            lastInterestAt = 2000L,
            interestPayoutMinute = 600,
            interestPayoutDay = 15
        )
        val roundtrip = original.toEntity().toDomain()

        assertEquals(original.id, roundtrip.id)
        assertEquals(original.name, roundtrip.name)
        assertEquals(original.type, roundtrip.type)
        assertEquals(original.initialBalance, roundtrip.initialBalance, 0.001)
        assertEquals(original.color, roundtrip.color)
        assertEquals(original.iconKey, roundtrip.iconKey)
        assertEquals(original.createdAt, roundtrip.createdAt)
        assertEquals(original.interestRate, roundtrip.interestRate, 0.001)
        assertEquals(original.interestPeriod, roundtrip.interestPeriod)
        assertEquals(original.lastInterestAt, roundtrip.lastInterestAt)
        assertEquals(original.interestPayoutMinute, roundtrip.interestPayoutMinute)
        assertEquals(original.interestPayoutDay, roundtrip.interestPayoutDay)
    }

    @Test
    fun `Account with null interest fields roundtrips correctly`() {
        val original = Account(name = "Наличные", type = AccountType.CASH, initialBalance = 0.0)
        val roundtrip = original.toEntity().toDomain()
        assertNull(roundtrip.interestPeriod)
        assertNull(roundtrip.lastInterestAt)
    }

    @Test
    fun `AccountEntity type string maps to enum`() {
        val entity = AccountEntity(
            name = "Test", type = "SAVINGS", initialBalance = 0.0,
            color = 0, iconKey = "", createdAt = 0
        )
        assertEquals(AccountType.SAVINGS, entity.toDomain().type)
    }

    @Test
    fun `AccountEntity unknown type falls back to OTHER`() {
        val entity = AccountEntity(
            name = "Test", type = "CRYPTO", initialBalance = 0.0,
            color = 0, iconKey = "", createdAt = 0
        )
        assertEquals(AccountType.OTHER, entity.toDomain().type)
    }

    // ─── Category roundtrip ─────────────────────────────────────────────

    @Test
    fun `Category to Entity and back preserves all fields`() {
        val original = Category(
            id = 10, name = "Еда", type = TransactionType.EXPENSE,
            iconKey = "food", color = 0xFFFF5722, isDefault = true
        )
        val roundtrip = original.toEntity().toDomain()
        assertEquals(original, roundtrip)
    }

    @Test
    fun `CategoryEntity unknown type falls back to EXPENSE`() {
        val entity = CategoryEntity(
            name = "X", type = "UNKNOWN", iconKey = "", color = 0, isDefault = false
        )
        assertEquals(TransactionType.EXPENSE, entity.toDomain().type)
    }

    // ─── Transaction roundtrip ───────────────────────────────────────────

    @Test
    fun `Transaction to Entity and back preserves all fields`() {
        val original = Transaction(
            id = 7, amount = 3500.0, type = TransactionType.INCOME,
            accountId = 1, categoryId = 5, note = "Зарплата",
            date = 1000L, createdAt = 900L
        )
        val roundtrip = original.toEntity().toDomain()
        assertEquals(original, roundtrip)
    }

    @Test
    fun `Transaction with null categoryId roundtrips`() {
        val original = Transaction(
            amount = 100.0, type = TransactionType.EXPENSE,
            accountId = 1, categoryId = null
        )
        val roundtrip = original.toEntity().toDomain()
        assertNull(roundtrip.categoryId)
    }

    // ─── Goal roundtrip ──────────────────────────────────────────────────

    @Test
    fun `Goal to Entity and back preserves all fields`() {
        val original = Goal(
            id = 3, name = "Отпуск", targetAmount = 200_000.0,
            savedAmount = 80_000.0, iconKey = "flight", color = 0xFF3FB18C,
            deadline = 5000L, createdAt = 100L, linkedAccountId = 42
        )
        val roundtrip = original.toEntity().toDomain()
        assertEquals(original, roundtrip)
    }

    @Test
    fun `Goal with null optional fields roundtrips`() {
        val original = Goal(name = "Test", targetAmount = 100.0)
        val roundtrip = original.toEntity().toDomain()
        assertNull(roundtrip.deadline)
        assertNull(roundtrip.linkedAccountId)
    }

    // ─── GoalContribution roundtrip ──────────────────────────────────────

    @Test
    fun `GoalContribution to Entity and back preserves all fields`() {
        val original = GoalContribution(
            id = 1, goalId = 3, accountId = 42, amount = 10_000.0, date = 2000L
        )
        val roundtrip = original.toEntity().toDomain()
        assertEquals(original, roundtrip)
    }

    @Test
    fun `Negative contribution amount preserved through mapping`() {
        val original = GoalContribution(goalId = 1, accountId = 2, amount = -5000.0)
        val roundtrip = original.toEntity().toDomain()
        assertEquals(-5000.0, roundtrip.amount, 0.001)
    }

    // ─── Transfer roundtrip ──────────────────────────────────────────────

    @Test
    fun `Transfer to Entity and back preserves all fields`() {
        val original = Transfer(
            id = 5, fromAccountId = 1, toAccountId = 2,
            amount = 50_000.0, note = "На сбер", date = 3000L, createdAt = 2500L
        )
        val roundtrip = original.toEntity().toDomain()
        assertEquals(original, roundtrip)
    }

    @Test
    fun `Transfer with empty note roundtrips`() {
        val original = Transfer(fromAccountId = 1, toAccountId = 2, amount = 100.0)
        val roundtrip = original.toEntity().toDomain()
        assertEquals("", roundtrip.note)
    }

    // ─── InterestPeriod mapping ──────────────────────────────────────────

    @Test
    fun `All InterestPeriod values roundtrip through string`() {
        InterestPeriod.entries.forEach { period ->
            val entity = Account(
                name = "test", interestRate = 10.0, interestPeriod = period
            ).toEntity()
            val back = entity.toDomain()
            assertEquals(period, back.interestPeriod)
        }
    }
}
