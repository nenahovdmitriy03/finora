package com.finora.presentation.home

import org.junit.Assert.*
import org.junit.Test

class HomeUiStateTest {

    @Test
    fun `freeBalance is total minus goals`() {
        val state = HomeUiState(totalBalance = 200_000.0, inGoals = 50_000.0)
        assertEquals(150_000.0, state.freeBalance, 0.001)
    }

    @Test
    fun `freeBalance does not go negative`() {
        val state = HomeUiState(totalBalance = 10_000.0, inGoals = 50_000.0)
        assertEquals(0.0, state.freeBalance, 0.001)
    }

    @Test
    fun `freeBalance equals total when no goals`() {
        val state = HomeUiState(totalBalance = 100_000.0, inGoals = 0.0)
        assertEquals(100_000.0, state.freeBalance, 0.001)
    }

    @Test
    fun `default state is loading with zeroes`() {
        val state = HomeUiState()
        assertEquals(0.0, state.totalBalance, 0.001)
        assertEquals(0.0, state.inGoals, 0.001)
        assertEquals(0.0, state.freeBalance, 0.001)
        assertEquals(0.0, state.monthIncome, 0.001)
        assertEquals(0.0, state.monthExpense, 0.001)
        assertTrue(state.loading)
    }
}
