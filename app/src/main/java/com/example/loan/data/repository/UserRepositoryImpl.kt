package com.example.loan.data.repository

import com.example.loan.data.local.dao.UserDao
import com.example.loan.data.local.entity.UserEntity
import com.example.loan.domain.model.User
import com.example.loan.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
) : UserRepository {
    override fun getFirstUser(): Flow<User?> =
        userDao.getFirstUser().map { it?.toDomain() }

    override suspend fun insertUser(user: User): Long =
        userDao.insert(user.toEntity())
}

private fun UserEntity.toDomain(): User = User(
    id = id,
    name = name,
    phone = phone,
    kycStatus = kycStatus,
    creditLimit = creditLimit,
)

private fun User.toEntity(): UserEntity = UserEntity(
    id = id,
    name = name,
    phone = phone,
    kycStatus = kycStatus,
    creditLimit = creditLimit,
)
