package com.example.loan.domain.repository

import com.example.loan.domain.model.Loan
import com.example.loan.domain.model.LoanStatus
import com.example.loan.domain.model.RepaymentSchedule
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface LoanRepository {
    fun getLoans(): Flow<List<Loan>>
    fun getSchedulesForLoan(loanId: Long): Flow<List<RepaymentSchedule>>
    fun getAllSchedules(): Flow<List<RepaymentSchedule>>

    /** 插入贷款及其还款计划，保证原子性，返回贷款 id */
    suspend fun insertLoanWithSchedules(loan: Loan, schedules: List<RepaymentSchedule>): Long

    /** 插入不带还款计划的贷款（如申请中的 PENDING 贷款），返回贷款 id */
    suspend fun insertLoan(loan: Loan): Long

    suspend fun updateLoanStatus(loanId: Long, status: LoanStatus)

    /** 标记某期还款为已还 */
    suspend fun markSchedulePaid(scheduleId: Long)

    /**
     * 确认放款：事务内校验状态为 APPROVED、生成还款计划、状态置 ACTIVE。
     * @return 放款是否成功（贷款不存在或状态不是 APPROVED 时为 false）
     */
    suspend fun activateLoan(loanId: Long, today: LocalDate): Boolean
}
