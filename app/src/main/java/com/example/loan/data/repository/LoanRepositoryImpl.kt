package com.example.loan.data.repository

import androidx.room.withTransaction
import com.example.loan.data.local.dao.LoanDao
import com.example.loan.data.local.dao.RepaymentScheduleDao
import com.example.loan.data.local.db.LoanDatabase
import com.example.loan.data.local.entity.LoanEntity
import com.example.loan.data.local.entity.RepaymentScheduleEntity
import com.example.loan.domain.model.Loan
import com.example.loan.domain.model.RepaymentSchedule
import com.example.loan.domain.repository.LoanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoanRepositoryImpl @Inject constructor(
    private val db: LoanDatabase,
    private val loanDao: LoanDao,
    private val repaymentScheduleDao: RepaymentScheduleDao,
) : LoanRepository {
    override fun getLoans(): Flow<List<Loan>> =
        loanDao.getLoans().map { entities -> entities.map { it.toDomain() } }

    override fun getSchedulesForLoan(loanId: Long): Flow<List<RepaymentSchedule>> =
        repaymentScheduleDao.getSchedulesForLoan(loanId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun insertLoanWithSchedules(
        loan: Loan,
        schedules: List<RepaymentSchedule>,
    ): Long = db.withTransaction {
        val loanId = loanDao.insert(loan.toEntity())
        repaymentScheduleDao.insertAll(
            schedules.map { it.copy(loanId = loanId).toEntity() },
        )
        loanId
    }
}

private fun LoanEntity.toDomain(): Loan = Loan(
    id = id,
    userId = userId,
    principal = principal,
    annualRateBp = annualRateBp,
    termMonths = termMonths,
    startDate = startDate,
    status = status,
)

private fun Loan.toEntity(): LoanEntity = LoanEntity(
    id = id,
    userId = userId,
    principal = principal,
    annualRateBp = annualRateBp,
    termMonths = termMonths,
    startDate = startDate,
    status = status,
)

private fun RepaymentScheduleEntity.toDomain(): RepaymentSchedule = RepaymentSchedule(
    id = id,
    loanId = loanId,
    period = period,
    dueDate = dueDate,
    amountDue = amountDue,
    isPaid = isPaid,
)

private fun RepaymentSchedule.toEntity(): RepaymentScheduleEntity = RepaymentScheduleEntity(
    id = id,
    loanId = loanId,
    period = period,
    dueDate = dueDate,
    amountDue = amountDue,
    isPaid = isPaid,
)
