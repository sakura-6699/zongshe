package com.example.loan.domain.model

import java.time.LocalDate

data class Loan(
    val id: Long = 0,
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
