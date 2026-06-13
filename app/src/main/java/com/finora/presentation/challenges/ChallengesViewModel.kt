package com.finora.presentation.challenges

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.data.repository.FinanceRepository
import com.finora.domain.model.Category
import com.finora.domain.model.Challenge
import com.finora.domain.model.TransactionType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChallengeWithSpend(
    val challenge: Challenge,
    val category: Category?,
    /** Actual spend during the challenge period (null for non-spending challenges). */
    val spent: Double? = null
) {
    /** Whether the spending cap is still respected. */
    val isOnTrack: Boolean
        get() {
            val target = challenge.targetAmount ?: return true
            return (spent ?: 0.0) <= target
        }
}

data class ChallengesUiState(
    val active: List<ChallengeWithSpend> = emptyList(),
    val completed: List<ChallengeWithSpend> = emptyList(),
    val expenseCategories: List<Category> = emptyList()
)

class ChallengesViewModel(private val repository: FinanceRepository) : ViewModel() {

    val uiState: StateFlow<ChallengesUiState> = combine(
        repository.observeChallenges(),
        repository.observeCategories(TransactionType.EXPENSE),
        repository.observeTransactionDetails() // trigger recompose when transactions change
    ) { challenges, cats, _ ->
        val catMap = cats.associateBy { it.id }
        val now = System.currentTimeMillis()
        val mapped = challenges.map { ch ->
            val cat = ch.categoryId?.let { catMap[it] }
            val spent = if (ch.targetAmount != null) {
                repository.expenseInCategoryBetween(ch.categoryId, ch.startDate, minOf(now, ch.endDate))
            } else null
            ChallengeWithSpend(ch, cat, spent)
        }
        ChallengesUiState(
            active = mapped.filter { it.challenge.isActive },
            completed = mapped.filter { it.challenge.completed || it.challenge.endDate < now },
            expenseCategories = cats
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChallengesUiState())

    fun addChallenge(
        title: String,
        description: String,
        emoji: String,
        targetDays: Int,
        targetAmount: Double?,
        categoryId: Long?
    ) {
        if (title.isBlank() || targetDays <= 0) return
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            repository.addChallenge(
                Challenge(
                    title = title.trim(),
                    description = description.trim(),
                    emoji = emoji,
                    targetDays = targetDays,
                    targetAmount = targetAmount,
                    categoryId = categoryId,
                    startDate = now,
                    endDate = now + targetDays.toLong() * 86_400_000,
                    createdAt = now
                )
            )
        }
    }

    fun completeChallenge(id: Long) {
        viewModelScope.launch { repository.completeChallenge(id) }
    }

    fun deleteChallenge(challenge: Challenge) {
        viewModelScope.launch { repository.deleteChallenge(challenge) }
    }
}
