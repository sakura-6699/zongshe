package com.example.loan.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.loan.domain.model.KycStatus

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val kycStatus: KycStatus,
    val creditLimit: Double,
)
