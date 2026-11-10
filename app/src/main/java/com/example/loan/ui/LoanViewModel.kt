package com.example.loan.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loan.domain.logic.LoanRules
import com.example.loan.domain.model.Loan
import com.example.loan.domain.model.LoanStatus
import com.example.loan.domain.model.RepaymentSchedule
import com.example.loan.domain.model.User
import com.example.loan.domain.repository.LoanRepository
import com.example.loan.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LoanViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val loanRepository: LoanRepository,
    private val clock: Clock,
    /** 模拟机审网络延迟（毫秒），测试可注入 0 */
    @Named("reviewDelayMillis") private val reviewDelayMillis: Long,
) : ViewModel() {

    private val submitting = MutableStateFlow(false)
    private val error = MutableStateFlow<UiError?>(null)

    init {
        // 进程可能在机审 delay 期间被杀，留下永久 PENDING；启动时立即补完机审判定
        viewModelScope.launch {
            val user = userRepository.getFirstUser().first() ?: return@launch
            loanRepository.getLoans().first()
                .filter { it.status == LoanStatus.PENDING }
                .forEach { loan ->
                    loanRepository.updateLoanStatus(loan.id, reviewDecision(loan.principal, user))
                }
        }
    }

    val uiState: StateFlow<LoanUiState> =
        combine(
            userRepository.getFirstUser(),
            loanRepository.getLoans(),
            loanRepository.getAllSchedules(),
            submitting,
            error,
        ) { user, loans, allSchedules, isSubmitting, currentError ->
            buildUiState(
                user = user,
                loans = loans,
                allSchedules = allSchedules,
                isSubmitting = isSubmitting,
                error = currentError,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LoanUiState(),
        )

    /** 用户申请贷款：先建 PENDING，模拟机审延迟后按额度判定 APPROVED/REJECTED */
    fun applyForLoan(principal: Double, termMonths: Int, annualRateBp: Int) {
        viewModelScope.launch {
            val user = uiState.value.user ?: run {
                error.update { UiError.UserMissing }
                return@launch
            }
            if (principal <= 0.0 || termMonths <= 0) {
                error.update { UiError.InvalidAmount(principal) }
                return@launch
            }
            if (submitting.value) return@launch

            submitting.update { true }
            try {
                val loanId = loanRepository.insertLoan(
                    Loan(
                        userId = user.id,
                        principal = principal,
                        annualRateBp = annualRateBp,
                        termMonths = termMonths,
                        startDate = clock.today(),
                        status = LoanStatus.PENDING,
                    ),
                )
                delay(reviewDelayMillis) // 模拟机审网络请求
                loanRepository.updateLoanStatus(loanId, reviewDecision(principal, user))
            } catch (e: Exception) {
                error.update { UiError.Unknown(e.message ?: "机审失败") }
            } finally {
                submitting.update { false }
            }
        }
    }

    /** 确认放款：仅 APPROVED 可放款，放款后 ACTIVE 并生成还款计划 */
    fun disburseLoan(loanId: Long) {
        viewModelScope.launch {
            val loan = uiState.value.loans
                .firstOrNull { it.loan.id == loanId }?.loan ?: return@launch
            if (loan.status != LoanStatus.APPROVED) {
                error.update { UiError.InvalidTransition(loan.status, "确认放款") }
                return@launch
            }
            try {
                loanRepository.activateLoan(loanId, clock.today())
            } catch (e: Exception) {
                error.update { UiError.Unknown(e.message ?: "放款失败") }
            }
        }
    }

    /** 正常还款：标记该期已还，状态由数据流重算自动流转 */
    fun repaySchedule(loanId: Long, scheduleId: Long) {
        viewModelScope.launch {
            try {
                loanRepository.markSchedulePaid(scheduleId)
            } catch (e: Exception) {
                error.update { UiError.Unknown(e.message ?: "还款失败") }
            }
        }
    }

    fun dismissError() {
        error.update { null }
    }

    /** 机审判定：超过信用额度即拒绝（init 补审与正常申请共用同一规则） */
    private fun reviewDecision(principal: Double, user: User): LoanStatus =
        if (principal > user.creditLimit) LoanStatus.REJECTED else LoanStatus.APPROVED

    private suspend fun buildUiState(
        user: User?,
        loans: List<Loan>,
        allSchedules: List<RepaymentSchedule>,
        isSubmitting: Boolean,
        error: UiError?,
    ): LoanUiState {
        val today = clock.today()
        val schedulesByLoan = allSchedules.groupBy { it.loanId }
        val uiModels = loans.map { loan ->
            val schedules = schedulesByLoan[loan.id].orEmpty().sortedBy { it.period }
            LoanUiModel(
                loan = loan,
                schedules = schedules,
                hasOverdue = schedules.any { LoanRules.isOverdue(it, today) },
                totalDue = schedules.filter { !it.isPaid }.sumOf { it.amountDue },
                totalPaid = schedules.filter { it.isPaid }.sumOf { it.amountDue },
                today = today,
            )
        }
        // 状态写回 DB：还款后结算 / 因时间流逝逾期 / 逾期还清恢复，均由同一规则驱动
        uiModels.forEach { model ->
            val expected = LoanRules.computeStatus(model.schedules, today, model.loan.status)
            if (expected != model.loan.status) {
                loanRepository.updateLoanStatus(model.loan.id, expected)
            }
        }
        return LoanUiState(
            isLoading = false,
            user = user,
            loans = uiModels,
            isSubmitting = isSubmitting,
            error = error,
        )
    }

    private fun Clock.today(): LocalDate = instant().atZone(zone).toLocalDate()
}
