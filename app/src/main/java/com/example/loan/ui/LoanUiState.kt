package com.example.loan.ui

import com.example.loan.domain.model.Loan
import com.example.loan.domain.model.LoanStatus
import com.example.loan.domain.model.RepaymentSchedule
import com.example.loan.domain.model.User

/** 贷款页面统一 UI 状态（列表、提交中、错误可共存，故用单一 data class） */
data class LoanUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val loans: List<LoanUiModel> = emptyList(),
    /** 机审进行中，阻止重复提交 */
    val isSubmitting: Boolean = false,
    /** 一次性错误，UI 展示后调用 dismissError 清除 */
    val error: UiError? = null,
)

data class LoanUiModel(
    val loan: Loan,
    val schedules: List<RepaymentSchedule> = emptyList(),
    val hasOverdue: Boolean = false,
    /** 未还应还合计（元） */
    val totalDue: Double = 0.0,
    /** 已还合计（元） */
    val totalPaid: Double = 0.0,
)

sealed interface UiError {
    data class InvalidAmount(val amount: Double) : UiError
    data object UserMissing : UiError
    data class InvalidTransition(
        val from: LoanStatus,
        val action: String,
    ) : UiError

    data class Unknown(val detail: String) : UiError
}
