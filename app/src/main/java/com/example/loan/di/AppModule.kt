package com.example.loan.di

import android.content.Context
import androidx.room.Room
import com.example.loan.data.local.dao.LoanDao
import com.example.loan.data.local.dao.RepaymentScheduleDao
import com.example.loan.data.local.dao.UserDao
import com.example.loan.data.local.db.DatabaseSeeder
import com.example.loan.data.local.db.LoanDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()

    @Provides
    @Named("reviewDelayMillis")
    fun provideReviewDelayMillis(): Long = 1_000

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): LoanDatabase = Room.databaseBuilder(
        context,
        LoanDatabase::class.java,
        LoanDatabase.DATABASE_NAME,
    ).build()

    @Provides
    fun provideUserDao(db: LoanDatabase): UserDao = db.userDao()

    @Provides
    fun provideLoanDao(db: LoanDatabase): LoanDao = db.loanDao()

    @Provides
    fun provideRepaymentScheduleDao(db: LoanDatabase): RepaymentScheduleDao =
        db.repaymentScheduleDao()
}
