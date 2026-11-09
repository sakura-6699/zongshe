package com.example.loan.domain.repository

import com.example.loan.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getFirstUser(): Flow<User?>
    suspend fun insertUser(user: User): Long
}
