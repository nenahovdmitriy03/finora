package com.finora.presentation.accounts

import com.finora.domain.model.Account
import com.finora.domain.model.AccountBalance
import org.junit.Assert.*
import org.junit.Test

class AccountsUiStateTest {

    @Test
    fun `free balance is total minus goals`() {
        val state = AccountsUiState(total = 100_000.0, inGoals = 30_000.0)
        assertEquals(70_000.0, state.free, 0.001)
    }

    @Test
    fun `free balance does not go negative`() {
        val state = AccountsUiState(total = 10_000.0, inGoals = 50_000.0)
        assertEquals(0.0, state.free, 0.001)
    }

    @Test
    fun `free equals total when no goals`() {
        val state = AccountsUiState(total = 50_000.0, inGoals = 0.0)
        assertEquals(50_000.0, state.free, 0.001)
    }

    @Test
    fun `default state is empty`() {
        val state = AccountsUiState()
        assertEquals(0.0, state.total, 0.001)
        assertEquals(0.0, state.inGoals, 0.001)
        assertEquals(0.0, state.free, 0.001)
        assertTrue(state.accounts.isEmpty())
    }

    @Test
    fun `state with accounts computes total`() {
        val acc1 = AccountBalance(Account(name = "A"), 10_000.0)
        val acc2 = AccountBalance(Account(name = "B"), 20_000.0)
        val state = AccountsUiState(
            total = 30_000.0,
            inGoals = 5_000.0,
            accounts = listOf(acc1, acc2)
        )
        assertEquals(2, state.accounts.size)
        assertEquals(25_000.0, state.free, 0.001)
    }
}
