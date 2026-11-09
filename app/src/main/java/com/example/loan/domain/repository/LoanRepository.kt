package com.example.loan.domain.repository

import com.example.loan.domain.model.Loan
import com.example.loan.domain.model.RepaymentSchedule
import kotlinx.coroutines.flow.Flow

interface LoanRepository {
    fun getLoans(): Flow<List<Loan>>
    fun getSchedulesForLoan(loanId: Long): Flow<List<RepaymentSchedule>>
    /** 插入贷款及其还款计划，保证原子性，返回贷款 id */
    suspend fun insertLoanWithSchedules(loan: Loan, schedules: List<RepaymentSchedule>): Long
}
