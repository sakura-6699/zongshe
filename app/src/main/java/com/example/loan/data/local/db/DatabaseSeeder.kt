package com.example.loan.data.local.db

import com.example.loan.data.local.dao.LoanDao
import com.example.loan.data.local.dao.RepaymentScheduleDao
import com.example.loan.data.local.dao.UserDao
import com.example.loan.data.local.entity.LoanEntity
import com.example.loan.data.local.entity.RepaymentScheduleEntity
import com.example.loan.data.local.entity.UserEntity
import com.example.loan.domain.model.KycStatus
import com.example.loan.domain.model.LoanStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * 首次启动时写入演示数据：1 个测试用户、1 笔 ACTIVE 贷款、3 期还款计划。
 * 幂等：已有用户则跳过。
 */
@Singleton
class DatabaseSeeder @Inject constructor(
    private val applicationScope: CoroutineScope,
    private val dbProvider: Provider<LoanDatabase>,
) {
    fun seedIfNeeded() {
        applicationScope.launch {
            val db = dbProvider.get()
            if (db.userDao().getFirstUserOnce() != null) return@launch
            seed(db.userDao(), db.loanDao(), db.repaymentScheduleDao())
        }
    }

    private suspend fun seed(
        userDao: UserDao,
        loanDao: LoanDao,
        scheduleDao: RepaymentScheduleDao,
    ) {
        val userId = userDao.insert(
            UserEntity(
                name = "张三",
                phone = "13800000000",
                kycStatus = KycStatus.VERIFIED,
                creditLimit = 50_000.0,
            ),
        )
        val principal = 10_000.0
        val annualRateBp = 780
        val termMonths = 3
        val startDate = LocalDate.now()
        val loanId = loanDao.insert(
            LoanEntity(
                userId = userId,
                principal = principal,
                annualRateBp = annualRateBp,
                termMonths = termMonths,
                startDate = startDate,
                status = LoanStatus.ACTIVE,
            ),
        )
        scheduleDao.insertAll(buildSchedules(loanId, principal, annualRateBp, termMonths, startDate))
    }
}

/** 等额本金：每期固定本金 + 剩余本金的当期利息（简化计算，非精确金融公式） */
private fun buildSchedules(
    loanId: Long,
    principal: Double,
    annualRateBp: Int,
    termMonths: Int,
    startDate: LocalDate,
): List<RepaymentScheduleEntity> {
    val monthlyRate = annualRateBp / 10_000.0 / 12
    val monthlyPrincipal = principal / termMonths
    return (1..termMonths).map { period ->
        val remaining = principal - (period - 1) * monthlyPrincipal
        RepaymentScheduleEntity(
            loanId = loanId,
            period = period,
            dueDate = startDate.plusMonths(period.toLong()),
            amountDue = monthlyPrincipal + remaining * monthlyRate,
        )
    }
}
