package com.example.loan.domain.model

import java.time.LocalDate

data class RepaymentSchedule(
    val id: Long = 0,
    val loanId: Long,
    /** 第几期（从 1 开始） */
    val period: Int,
    val dueDate: LocalDate,
    /** 应还总额（元，含本金与利息） */
    val amountDue: Double,
    val isPaid: Boolean = false,
)
