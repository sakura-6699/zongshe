package com.example.loan.data.local.db

import com.example.loan.data.local.dao.UserDao
import com.example.loan.data.local.entity.UserEntity
import com.example.loan.domain.model.KycStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * 首次启动写入演示用测试用户（仅用户，不播种贷款，申请流程从空开始）。
 * 幂等：已有用户则跳过。
 */
@Singleton
class DemoUserSeeder @Inject constructor(
    private val applicationScope: CoroutineScope,
    private val dbProvider: Provider<LoanDatabase>,
) {
    fun seedIfNeeded() {
        applicationScope.launch {
            val db = dbProvider.get()
            if (db.userDao().getFirstUserOnce() != null) return@launch
            db.userDao().insert(testUser())
        }
    }

    companion object {
        fun testUser() = UserEntity(
            name = "张三",
            phone = "13800000000",
            kycStatus = KycStatus.VERIFIED,
            creditLimit = 50_000.0,
        )
    }
}
