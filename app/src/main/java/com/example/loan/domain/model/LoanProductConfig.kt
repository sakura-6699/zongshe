package com.example.loan.domain.model

/** 贷款产品配置（演示用单一产品，利率与期限集中管理） */
object LoanProductConfig {
    /** 年利率，万分比（780 = 7.8%） */
    const val DEFAULT_ANNUAL_RATE_BP = 780

    /** 可选期限（月） */
    val ALLOWED_TERMS = listOf(3, 6, 12)
}
