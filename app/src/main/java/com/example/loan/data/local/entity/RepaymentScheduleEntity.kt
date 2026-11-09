package com.example.loan.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "repayment_schedules",
    foreignKeys = [
        ForeignKey(
            entity = LoanEntity::class,
            parentColumns = ["id"],
            childColumns = ["loanId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("loanId")],
)
data class RepaymentScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,
    /** 第几期（从 1 开始） */
    val period: Int,
    val dueDate: LocalDate,
    /** 应还总额（元，含本金与利息） */
    val amountDue: Double,
    val isPaid: Boolean = false,
)
