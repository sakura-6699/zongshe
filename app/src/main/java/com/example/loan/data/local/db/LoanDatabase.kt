package com.example.loan.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.loan.data.local.dao.LoanDao
import com.example.loan.data.local.dao.RepaymentScheduleDao
import com.example.loan.data.local.dao.UserDao
import com.example.loan.data.local.entity.LoanEntity
import com.example.loan.data.local.entity.RepaymentScheduleEntity
import com.example.loan.data.local.entity.UserEntity

@Database(
    entities = [UserEntity::class, LoanEntity::class, RepaymentScheduleEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(LoanConverters::class)
abstract class LoanDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun loanDao(): LoanDao
    abstract fun repaymentScheduleDao(): RepaymentScheduleDao

    companion object {
        const val DATABASE_NAME = "loan.db"
    }
}
