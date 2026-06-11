package com.finora.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Wraps Supabase Auth for sign-up / sign-in / sign-out.
 */
class AuthRepository(private val client: SupabaseClient) {

    /** Current auth state as a simple sealed class. */
    sealed interface AuthState {
        data object Loading : AuthState
        data object NotAuthenticated : AuthState
        data class Authenticated(val userId: String, val email: String?) : AuthState
    }

    /** Observe auth state reactively. */
    val authState: Flow<AuthState> = client.auth.sessionStatus.map { status ->
        when (status) {
            is SessionStatus.Authenticated -> {
                val user = status.session.user
                AuthState.Authenticated(
                    userId = user?.id ?: "",
                    email = user?.email
                )
            }
            is SessionStatus.NotAuthenticated -> AuthState.NotAuthenticated
            else -> AuthState.Loading
        }
    }

    /** Sign up with email + password. Throws on failure. */
    suspend fun signUp(email: String, password: String) {
        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    /** Sign in with email + password. Throws on failure. */
    suspend fun signIn(email: String, password: String) {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    /** Sign out. */
    suspend fun signOut() {
        client.auth.signOut()
    }

    /**
     * Permanently delete the current auth user (the row in auth.users).
     *
     * The anon key can't touch auth.users directly, so this calls a Postgres
     * function `delete_user()` that runs with elevated privileges (SECURITY
     * DEFINER) and deletes auth.uid(). The function must exist in Supabase —
     * see the SQL in the project docs. Throws if it isn't installed.
     */
    suspend fun deleteUser() {
        client.postgrest.rpc("delete_user")
    }

    /** Current user ID or null. */
    fun currentUserId(): String? =
        client.auth.currentUserOrNull()?.id
}
