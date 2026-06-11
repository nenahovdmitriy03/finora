package com.finora.data.backup

import com.finora.data.local.entity.AccountEntity
import com.finora.data.local.entity.CategoryEntity
import com.finora.data.local.entity.GoalContributionEntity
import com.finora.data.local.entity.GoalEntity
import com.finora.data.local.entity.TransactionEntity
import com.finora.data.local.entity.TransferEntity
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-logic tests for the JSON backup format (no Android / DB needed).
 * Verifies that a full snapshot survives a serialize → deserialize round-trip
 * with all fields and IDs intact — this is what protects the user's data.
 */
class BackupSerializationTest {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    private fun sample() = BackupData(
        version = 1,
        dbVersion = 5,
        exportedAt = 1_700_000_000_000L,
        accounts = listOf(
            AccountEntity(
                id = 1, name = "Тинькофф", type = "CARD", initialBalance = 1234.56,
                color = 0xFF6C5CE7, iconKey = "card", createdAt = 100L,
                interestRate = 16.0, interestPeriod = "MONTHLY", lastInterestAt = 200L,
                interestPayoutMinute = 600, interestPayoutDay = 15
            )
        ),
        categories = listOf(
            CategoryEntity(id = 2, name = "Еда", type = "EXPENSE", iconKey = "food", color = 0xFF00B894, isDefault = true)
        ),
        transactions = listOf(
            TransactionEntity(id = 3, amount = 99.99, type = "EXPENSE", accountId = 1, categoryId = 2, note = "обед", date = 300L, createdAt = 301L),
            TransactionEntity(id = 4, amount = 5000.0, type = "INCOME", accountId = 1, categoryId = null, note = "", date = 400L, createdAt = 401L)
        ),
        goals = listOf(
            GoalEntity(id = 5, name = "Бали", targetAmount = 100000.0, savedAmount = 25000.0, iconKey = "beach", color = 0xFFE17055, deadline = 999L, createdAt = 500L, linkedAccountId = 1)
        ),
        goalContributions = listOf(
            GoalContributionEntity(id = 6, goalId = 5, accountId = 1, amount = 25000.0, date = 600L)
        ),
        transfers = listOf(
            TransferEntity(id = 7, fromAccountId = 1, toAccountId = 1, amount = 100.0, note = "перевод", date = 700L, createdAt = 701L)
        )
    )

    @Test
    fun `backup survives serialize-deserialize round-trip`() {
        val original = sample()
        val text = json.encodeToString(BackupData.serializer(), original)
        val restored = json.decodeFromString(BackupData.serializer(), text)
        assertEquals(original, restored)
    }

    @Test
    fun `totalRecords counts every row`() {
        assertEquals(1 + 1 + 2 + 1 + 1 + 1, sample().totalRecords)
    }

    @Test
    fun `nullable transaction category is preserved`() {
        val text = json.encodeToString(BackupData.serializer(), sample())
        val restored = json.decodeFromString(BackupData.serializer(), text)
        assertEquals(null, restored.transactions.first { it.id == 4L }.categoryId)
        assertEquals(2L, restored.transactions.first { it.id == 3L }.categoryId)
    }

    @Test
    fun `unknown future fields are ignored`() {
        // Simulates restoring a file created by a newer app version.
        val withExtra = """{"version":1,"somethingNew":true,"accounts":[],"categories":[]}"""
        val restored = json.decodeFromString(BackupData.serializer(), withExtra)
        assertTrue(restored.accounts.isEmpty())
    }

    @Test
    fun `corrupt json throws (so the DB is never wiped on a bad file)`() {
        assertThrows(Exception::class.java) {
            json.decodeFromString(BackupData.serializer(), "this is not json{{{")
        }
    }
}
