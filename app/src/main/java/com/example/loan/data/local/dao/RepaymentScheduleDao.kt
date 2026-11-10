package com.example.loan.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.loan.data.local.entity.RepaymentScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RepaymentScheduleDao {
    @Query("SELECT * FROM repayment_schedules WHERE loanId = :loanId ORDER BY period ASC")
    fun getSchedulesForLoan(loanId: Long): Flow<List<RepaymentScheduleEntity>>

    @Query("SELECT * FROM repayment_schedules ORDER BY dueDate ASC")
    fun getAllSchedules(): Flow<List<RepaymentScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(schedules: List<RepaymentScheduleEntity>): List<Long>

    @Query("UPDATE repayment_schedules SET isPaid = 1 WHERE id = :scheduleId")
    suspend fun markPaid(scheduleId: Long)
}
