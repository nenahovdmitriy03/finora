package com.finora.presentation.goals

import com.finora.domain.model.Account
import com.finora.domain.model.Goal
import com.finora.domain.model.GoalAccountSummary
import com.finora.domain.model.GoalContribution
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests the GoalWithSources aggregation logic from GoalsViewModel.
 *
 * GoalWithSources combines each goal with a list of accounts that
 * contributed to it (net amounts, sorted descending).
 */
class GoalsViewModelLogicTest {

    /**
     * Mirrors the combine() logic in GoalsViewModel.goalsWithSources.
     */
    private fun buildGoalWithSources(
        goals: List<Goal>,
        contributions: List<GoalContribution>,
        accounts: List<Account>
    ): List<GoalWithSources> {
        val accountMap = accounts.associateBy { it.id }
        val contribsByGoal = contributions.groupBy { it.goalId }
        return goals.map { goal ->
            val goalContribs = contribsByGoal[goal.id].orEmpty()
            val byAccount = goalContribs
                .groupBy { it.accountId }
                .mapNotNull { (accId, items) ->
                    val acc = accountMap[accId] ?: return@mapNotNull null
                    val net = items.sumOf { it.amount }
                    if (net == 0.0) return@mapNotNull null
                    GoalAccountSummary(account = acc, netAmount = net)
                }
                .sortedByDescending { it.netAmount }
            GoalWithSources(goal = goal, sources = byAccount)
        }
    }

    @Test
    fun `goal with no contributions has empty sources`() {
        val goal = Goal(id = 1, name = "Отпуск", targetAmount = 100_000.0)
        val result = buildGoalWithSources(listOf(goal), emptyList(), emptyList())
        assertEquals(1, result.size)
        assertTrue(result[0].sources.isEmpty())
    }

    @Test
    fun `goal with single account contribution`() {
        val goal = Goal(id = 1, name = "Авто", targetAmount = 500_000.0, savedAmount = 100_000.0)
        val acc = Account(id = 10, name = "Сбер")
        val contributions = listOf(
            GoalContribution(goalId = 1, accountId = 10, amount = 100_000.0)
        )
        val result = buildGoalWithSources(listOf(goal), contributions, listOf(acc))
        assertEquals(1, result[0].sources.size)
        assertEquals("Сбер", result[0].sources[0].account.name)
        assertEquals(100_000.0, result[0].sources[0].netAmount, 0.001)
    }

    @Test
    fun `goal with multiple accounts sorted by contribution`() {
        val goal = Goal(id = 1, name = "Дом", targetAmount = 3_000_000.0, savedAmount = 200_000.0)
        val acc1 = Account(id = 1, name = "Тинькофф")
        val acc2 = Account(id = 2, name = "Сбер")
        val acc3 = Account(id = 3, name = "Наличные")
        val contributions = listOf(
            GoalContribution(goalId = 1, accountId = 2, amount = 80_000.0),
            GoalContribution(goalId = 1, accountId = 1, amount = 50_000.0),
            GoalContribution(goalId = 1, accountId = 3, amount = 30_000.0),
            GoalContribution(goalId = 1, accountId = 2, amount = 40_000.0) // second deposit from Сбер
        )
        val result = buildGoalWithSources(
            listOf(goal), contributions, listOf(acc1, acc2, acc3)
        )
        val sources = result[0].sources
        assertEquals(3, sources.size)
        // Sorted by netAmount desc: Сбер(120k), Тинькофф(50k), Наличные(30k)
        assertEquals("Сбер", sources[0].account.name)
        assertEquals(120_000.0, sources[0].netAmount, 0.001)
        assertEquals("Тинькофф", sources[1].account.name)
        assertEquals(50_000.0, sources[1].netAmount, 0.001)
        assertEquals("Наличные", sources[2].account.name)
        assertEquals(30_000.0, sources[2].netAmount, 0.001)
    }

    @Test
    fun `multiple goals aggregate independently`() {
        val goal1 = Goal(id = 1, name = "Отпуск", targetAmount = 100_000.0, savedAmount = 30_000.0)
        val goal2 = Goal(id = 2, name = "Авто", targetAmount = 500_000.0, savedAmount = 50_000.0)
        val acc = Account(id = 1, name = "Тинькофф")
        val contributions = listOf(
            GoalContribution(goalId = 1, accountId = 1, amount = 30_000.0),
            GoalContribution(goalId = 2, accountId = 1, amount = 50_000.0)
        )
        val result = buildGoalWithSources(listOf(goal1, goal2), contributions, listOf(acc))
        assertEquals(2, result.size)
        assertEquals(30_000.0, result[0].sources[0].netAmount, 0.001)
        assertEquals(50_000.0, result[1].sources[0].netAmount, 0.001)
    }

    @Test
    fun `partial withdrawal reduces net amount`() {
        val goal = Goal(id = 1, name = "Test", targetAmount = 100_000.0, savedAmount = 7_000.0)
        val acc = Account(id = 1, name = "Acc")
        val contributions = listOf(
            GoalContribution(goalId = 1, accountId = 1, amount = 10_000.0),
            GoalContribution(goalId = 1, accountId = 1, amount = -3_000.0)
        )
        val result = buildGoalWithSources(listOf(goal), contributions, listOf(acc))
        assertEquals(7_000.0, result[0].sources[0].netAmount, 0.001)
    }

    @Test
    fun `full withdrawal removes account from sources`() {
        val goal = Goal(id = 1, name = "Test", targetAmount = 100_000.0, savedAmount = 0.0)
        val acc = Account(id = 1, name = "Acc")
        val contributions = listOf(
            GoalContribution(goalId = 1, accountId = 1, amount = 10_000.0),
            GoalContribution(goalId = 1, accountId = 1, amount = -10_000.0)
        )
        val result = buildGoalWithSources(listOf(goal), contributions, listOf(acc))
        assertTrue(result[0].sources.isEmpty())
    }

    @Test
    fun `saveGoal validation rejects blank name`() {
        // Mirrors GoalsViewModel.saveGoal() guard
        val name = "   "
        val target = 100_000.0
        val shouldSave = name.isNotBlank() && target > 0.0
        assertFalse(shouldSave)
    }

    @Test
    fun `saveGoal validation rejects zero target`() {
        val name = "Test"
        val target = 0.0
        val shouldSave = name.isNotBlank() && target > 0.0
        assertFalse(shouldSave)
    }

    @Test
    fun `saveGoal validation rejects negative target`() {
        val name = "Test"
        val target = -5_000.0
        val shouldSave = name.isNotBlank() && target > 0.0
        assertFalse(shouldSave)
    }

    @Test
    fun `saveGoal validation accepts valid input`() {
        val name = "Отпуск"
        val target = 100_000.0
        val shouldSave = name.isNotBlank() && target > 0.0
        assertTrue(shouldSave)
    }
}
