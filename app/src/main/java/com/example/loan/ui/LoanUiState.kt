package com.example.loan.ui

import com.example.loan.domain.model.Loan
import com.example.loan.domain.model.LoanStatus
import com.example.loan.domain.model.RepaymentSchedule
import com.example.loan.domain.model.User
import java.time.LocalDate

/** 贷款页面统一 UI 状态（列表、提交中、错误可共存，故用单一 data class） */
data class LoanUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val loans: List<LoanUiModel> = emptyList(),
    /** 机审进行中，阻止重复提交 */
    val isSubmitting: Boolean = false,
    /** 一次性错误，UI 展示后调用 dismissError 清除 */
    val error: UiError? = null,
) {
    /** 当前应展示的屏幕，按优先级从贷款列表推导 */
    val screen: LoanScreen
        get() = if (isLoading) {
            LoanScreen.Loading
        } else {
            deriveScreen(loans)
        }

    private fun deriveScreen(loans: List<LoanUiModel>): LoanScreen {
        fun latestOf(status: LoanStatus) = loans.lastOrNull { it.loan.status == status }
        fun latestOfAny(statuses: Set<LoanStatus>) =
            loans.lastOrNull { it.loan.status in statuses }

        return when {
            loans.any { it.loan.status == LoanStatus.PENDING } -> LoanScreen.Reviewing
            loans.any { it.loan.status == LoanStatus.APPROVED } ->
                LoanScreen.ApprovePending(latestOf(LoanStatus.APPROVED)!!)
            else -> latestOfAny(setOf(LoanStatus.ACTIVE, LoanStatus.OVERDUE))
                ?.let { LoanScreen.Repayment(it) }
                ?: latestOf(LoanStatus.SETTLED)?.let { LoanScreen.Settled(it) }
                ?: LoanScreen.ApplyForm(
                    showRejectedHint = loans.any { it.loan.status == LoanStatus.REJECTED },
                )
        }
    }
}

data class LoanUiModel(
    val loan: Loan,
    val schedules: List<RepaymentSchedule> = emptyList(),
    val hasOverdue: Boolean = false,
    /** 未还应还合计（元） */
    val totalDue: Double = 0.0,
    /** 已还合计（元） */
    val totalPaid: Double = 0.0,
    /** 渲染时使用的"今天"（来自注入的 Clock），供 UI 判定单期逾期 */
    val today: LocalDate = LocalDate.MIN,
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

/**
 * 当前应展示的屏幕，由 ViewModel 从贷款列表按优先级推导，Composable 只负责渲染。
 * 优先级：PENDING > APPROVED > ACTIVE/OVERDUE > SETTLED > REJECTED/无贷款。
 */
sealed interface LoanScreen {
    data object Loading : LoanScreen

    /** 无贷款或最近贷款被拒；showRejectedHint 时展示"综合评分不足" */
    data class ApplyForm(val showRejectedHint: Boolean) : LoanScreen

    data object Reviewing : LoanScreen

    data class ApprovePending(val loan: LoanUiModel) : LoanScreen

    data class Repayment(val loan: LoanUiModel) : LoanScreen

    data class Settled(val loan: LoanUiModel) : LoanScreen
}
