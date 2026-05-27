package com.dhyey.fanfic.auth

import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.StateFlow

interface AuthService {
    val currentUser: StateFlow<FirebaseUser?>
    suspend fun signUp(email: String, password: String): Result<FirebaseUser>
    suspend fun signIn(email: String, password: String): Result<FirebaseUser>
    fun signOut()
    fun isUserSignedIn(): Boolean
}
