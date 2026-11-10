package com.example.loan.data.local.db

import com.example.loan.data.local.dao.LoanDao
import com.example.loan.data.local.dao.RepaymentScheduleDao
import com.example.loan.data.local.dao.UserDao
import com.example.loan.data.local.entity.LoanEntity
import com.example.loan.data.local.entity.UserEntity
import com.example.loan.data.repository.RepaymentPlanCalculator
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
        val startDate = LocalDate.now()
        val loanId = loanDao.insert(
            LoanEntity(
                userId = userId,
                principal = 10_000.0,
                annualRateBp = 780,
                termMonths = 3,
                startDate = startDate,
                status = LoanStatus.ACTIVE,
            ),
        )
        scheduleDao.insertAll(
            RepaymentPlanCalculator.buildSchedules(
                loanId = loanId,
                principal = 10_000.0,
                annualRateBp = 780,
                termMonths = 3,
                startDate = startDate,
            ),
        )
    }
}
