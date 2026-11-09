package com.example.loan.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.loan.domain.model.LoanStatus
import java.time.LocalDate

@Entity(
    tableName = "loans",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("userId")],
)
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    /** 借款本金（元） */
    val principal: Double,
    /** 年利率，单位万分比（如 780 = 7.8%） */
    val annualRateBp: Int,
    /** 期限（月） */
    val termMonths: Int,
    val startDate: LocalDate,
    val status: LoanStatus,
)
