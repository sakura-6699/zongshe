package com.example.loan.di

import com.example.loan.data.repository.LoanRepositoryImpl
import com.example.loan.data.repository.UserRepositoryImpl
import com.example.loan.domain.repository.LoanRepository
import com.example.loan.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindLoanRepository(impl: LoanRepositoryImpl): LoanRepository
}
