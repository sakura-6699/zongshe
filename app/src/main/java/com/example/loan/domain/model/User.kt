package com.example.loan.domain.model

data class User(
    val id: Long = 0,
    val name: String,
    val phone: String,
    val kycStatus: KycStatus,
    val creditLimit: Double,
)
