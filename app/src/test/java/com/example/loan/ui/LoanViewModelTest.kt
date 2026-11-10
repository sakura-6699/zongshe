package com.example.loan.ui

import com.example.loan.domain.model.KycStatus
import com.example.loan.domain.model.Loan
import com.example.loan.domain.model.LoanStatus
import com.example.loan.domain.model.RepaymentSchedule
import com.example.loan.domain.model.User
import com.example.loan.domain.repository.LoanRepository
import com.example.loan.domain.repository.UserRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoanViewModelTest {

    private val today: LocalDate = LocalDate.of(2026, 11, 9)
    private val clock: Clock =
        Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)

    private lateinit var userRepo: FakeUserRepository
    private lateinit var loanRepo: FakeLoanRepository

    @Before
    fun setUp() {
        userRepo = FakeUserRepository(testUser())
        loanRepo = FakeLoanRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** viewModelScope 使用 Main 调度器，必须与 runTest 的调度器共享才能被虚拟时间控制 */
    private fun TestScope.bindMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    }

    private fun testUser() = User(
        id = 1,
        name = "张三",
        phone = "13800000000",
        kycStatus = KycStatus.VERIFIED,
        creditLimit = 5_000.0,
    )

    /** 在 backgroundScope 订阅 uiState 以驱动 stateIn（WhileSubscribed）；测试结束自动取消 */
    private fun TestScope.collectState(vm: LoanViewModel) {
        backgroundScope.launch { vm.uiState.collect {} }
    }

    // ---- 模拟机审 ----

    @Test
    fun `apply within limit - PENDING during review then APPROVED`() = runTest {
        bindMainDispatcher()
        val vm = LoanViewModel(userRepo, loanRepo, clock, reviewDelayMillis = 1_000)
        collectState(vm)
        advanceUntilIdle()

        vm.applyForLoan(principal = 3_000.0, termMonths = 3, annualRateBp = 780)
        runCurrent() // 执行到 delay(1000) 挂起点，不推进虚拟时间

        val stateDuringReview = vm.uiState.value
        assertTrue(stateDuringReview.isSubmitting)
        assertEquals(1, stateDuringReview.loans.size)
        assertEquals(LoanStatus.PENDING, stateDuringReview.loans.first().loan.status)

        advanceTimeBy(1_000)
        advanceUntilIdle()

        val stateAfterReview = vm.uiState.value
        assertEquals(false, stateAfterReview.isSubmitting)
        assertEquals(LoanStatus.APPROVED, stateAfterReview.loans.first().loan.status)
    }

    @Test
    fun `apply over creditLimit - REJECTED`() = runTest {
        bindMainDispatcher()
        val vm = LoanViewModel(userRepo, loanRepo, clock, reviewDelayMillis = 0)
        collectState(vm)
        advanceUntilIdle()

        vm.applyForLoan(principal = 6_000.0, termMonths = 3, annualRateBp = 780)
        advanceUntilIdle()

        assertEquals(LoanStatus.REJECTED, vm.uiState.value.loans.first().loan.status)
    }

    @Test
    fun `apply with invalid amount - error and no loan created`() = runTest {
        bindMainDispatcher()
        val vm = LoanViewModel(userRepo, loanRepo, clock, reviewDelayMillis = 0)
        collectState(vm)
        advanceUntilIdle()

        vm.applyForLoan(principal = 0.0, termMonths = 3, annualRateBp = 780)
        advanceUntilIdle()

        assertEquals(UiError.InvalidAmount(0.0), vm.uiState.value.error)
        assertTrue(vm.uiState.value.loans.isEmpty())
    }

    @Test
    fun `apply without user - UserMissing`() = runTest {
        bindMainDispatcher()
        userRepo = FakeUserRepository(null)
        val vm = LoanViewModel(userRepo, loanRepo, clock, reviewDelayMillis = 0)
        collectState(vm)
        advanceUntilIdle()

        vm.applyForLoan(principal = 1_000.0, termMonths = 3, annualRateBp = 780)
        advanceUntilIdle()

        assertEquals(UiError.UserMissing, vm.uiState.value.error)
    }

    @Test
    fun `init completes stuck pending review - no delay replay`() = runTest {
        bindMainDispatcher()
        loanRepo.loansFlow.value = listOf(
            Loan(id = 1, userId = 1, principal = 3_000.0, annualRateBp = 780, termMonths = 3, startDate = today, status = LoanStatus.PENDING),
        )
        // 机审延迟设为 1 秒：补审不应重放 delay，advanceUntilIdle 即完成
        val vm = LoanViewModel(userRepo, loanRepo, clock, reviewDelayMillis = 1_000)
        collectState(vm)
        advanceUntilIdle()

        assertEquals(LoanStatus.APPROVED, loanRepo.loansFlow.value.first().status)
    }

    // ---- 确认放款 ----

    @Test
    fun `disburse approved loan - ACTIVE with schedules generated`() = runTest {
        bindMainDispatcher()
        val vm = LoanViewModel(userRepo, loanRepo, clock, reviewDelayMillis = 0)
        collectState(vm)
        advanceUntilIdle()

        vm.applyForLoan(principal = 3_000.0, termMonths = 3, annualRateBp = 780)
        advanceUntilIdle()
        val loanId = vm.uiState.value.loans.first().loan.id

        vm.disburseLoan(loanId)
        advanceUntilIdle()

        val model = vm.uiState.value.loans.first()
        assertEquals(LoanStatus.ACTIVE, model.loan.status)
        assertEquals(3, model.schedules.size)
        assertEquals(listOf(1, 2, 3), model.schedules.map { it.period })
    }

    @Test
    fun `disburse rejected loan - InvalidTransition`() = runTest {
        bindMainDispatcher()
        val vm = LoanViewModel(userRepo, loanRepo, clock, reviewDelayMillis = 0)
        collectState(vm)
        advanceUntilIdle()

        vm.applyForLoan(principal = 6_000.0, termMonths = 3, annualRateBp = 780)
        advanceUntilIdle()
        val loanId = vm.uiState.value.loans.first().loan.id
        vm.dismissError() // 清除机审流程本身无错误，此处防御

        vm.disburseLoan(loanId)
        advanceUntilIdle()

        val error = vm.uiState.value.error
        assertNotNull(error)
        assertEquals(LoanStatus.REJECTED, (error as UiError.InvalidTransition).from)
    }

    // ---- 逾期 ----

    @Test
    fun `overdue unpaid schedule - hasOverdue true and status written back to OVERDUE`() = runTest {
        bindMainDispatcher()
        seedActiveLoanWithSchedules(
            schedules = listOf(
                schedule(period = 1, dueDate = today.minusDays(1), isPaid = false),
                schedule(period = 2, dueDate = today.plusMonths(1), isPaid = false),
            ),
        )
        val vm = LoanViewModel(userRepo, loanRepo, clock, reviewDelayMillis = 0)
        collectState(vm)
        advanceUntilIdle()

        val model = vm.uiState.value.loans.first()
        assertTrue(model.hasOverdue)
        assertEquals(LoanStatus.OVERDUE, loanRepo.loansFlow.value.first().status)
    }

    // ---- 还款 ----

    @Test
    fun `repay all schedules - SETTLED`() = runTest {
        bindMainDispatcher()
        val firstId = seedActiveLoanWithSchedules(
            schedules = listOf(
                schedule(period = 1, dueDate = today.plusDays(5), isPaid = false),
                schedule(period = 2, dueDate = today.plusMonths(1), isPaid = false),
            ),
        )
        val vm = LoanViewModel(userRepo, loanRepo, clock, reviewDelayMillis = 0)
        collectState(vm)
        advanceUntilIdle()
        val scheduleIds = vm.uiState.value.loans.first().schedules.map { it.id }

        scheduleIds.forEach { vm.repaySchedule(firstId, it) }
        advanceUntilIdle()

        assertEquals(LoanStatus.SETTLED, loanRepo.loansFlow.value.first().status)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `repay overdue schedule only - status recovers to ACTIVE`() = runTest {
        bindMainDispatcher()
        seedActiveLoanWithSchedules(
            schedules = listOf(
                schedule(period = 1, dueDate = today.minusDays(1), isPaid = false),
                schedule(period = 2, dueDate = today.plusMonths(1), isPaid = false),
            ),
        )
        val vm = LoanViewModel(userRepo, loanRepo, clock, reviewDelayMillis = 0)
        collectState(vm)
        advanceUntilIdle() // 先触发 OVERDUE 写回
        val overdueId = vm.uiState.value.loans.first().schedules.first { it.period == 1 }.id

        vm.repaySchedule(1L, overdueId)
        advanceUntilIdle()

        assertEquals(LoanStatus.ACTIVE, loanRepo.loansFlow.value.first().status)
    }

    @Test
    fun `partial repay without overdue - stays ACTIVE`() = runTest {
        bindMainDispatcher()
        seedActiveLoanWithSchedules(
            schedules = listOf(
                schedule(period = 1, dueDate = today.plusDays(5), isPaid = false),
                schedule(period = 2, dueDate = today.plusMonths(1), isPaid = false),
            ),
        )
        val vm = LoanViewModel(userRepo, loanRepo, clock, reviewDelayMillis = 0)
        collectState(vm)
        advanceUntilIdle()
        val firstScheduleId = vm.uiState.value.loans.first().schedules.first().id

        vm.repaySchedule(1L, firstScheduleId)
        advanceUntilIdle()

        assertEquals(LoanStatus.ACTIVE, loanRepo.loansFlow.value.first().status)
    }

    // ---- 屏幕推导 ----

    @Test
    fun `screen - no loans shows ApplyForm without hint`() = runTest {
        bindMainDispatcher()
        val vm = LoanViewModel(userRepo, loanRepo, clock, reviewDelayMillis = 0)
        collectState(vm)
        advanceUntilIdle()

        assertEquals(LoanScreen.ApplyForm(showRejectedHint = false), vm.uiState.value.screen)
    }

    @Test
    fun `screen - rejected loan shows ApplyForm with hint`() = runTest {
        bindMainDispatcher()
        loanRepo.loansFlow.value = listOf(
            Loan(id = 1, userId = 1, principal = 6_000.0, annualRateBp = 780, termMonths = 3, startDate = today, status = LoanStatus.REJECTED),
        )
        val vm = LoanViewModel(userRepo, loanRepo, clock, reviewDelayMillis = 0)
        collectState(vm)
        advanceUntilIdle()

        assertEquals(LoanScreen.ApplyForm(showRejectedHint = true), vm.uiState.value.screen)
    }

    @Test
    fun `screen - pending loan shows Reviewing`() = runTest {
        bindMainDispatcher()
        val vm = LoanViewModel(userRepo, loanRepo, clock, reviewDelayMillis = 1_000)
        collectState(vm)
        advanceUntilIdle()

        vm.applyForLoan(principal = 3_000.0, termMonths = 3, annualRateBp = 780)
        runCurrent() // 停在机审 delay 中，贷款处于 PENDING

        assertEquals(LoanScreen.Reviewing, vm.uiState.value.screen)
    }

    @Test
    fun `screen - approved loan shows ApprovePending`() = runTest {
        bindMainDispatcher()
        loanRepo.loansFlow.value = listOf(
            Loan(id = 1, userId = 1, principal = 3_000.0, annualRateBp = 780, termMonths = 3, startDate = today, status = LoanStatus.APPROVED),
        )
        val vm = LoanViewModel(userRepo, loanRepo, clock, reviewDelayMillis = 0)
        collectState(vm)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.screen is LoanScreen.ApprovePending)
    }

    @Test
    fun `screen - approved loan takes priority over active`() = runTest {
        bindMainDispatcher()
        loanRepo.loansFlow.value = listOf(
            Loan(id = 1, userId = 1, principal = 3_000.0, annualRateBp = 780, termMonths = 3, startDate = today, status = LoanStatus.APPROVED),
            Loan(id = 2, userId = 1, principal = 3_000.0, annualRateBp = 780, termMonths = 3, startDate = today, status = LoanStatus.ACTIVE),
        )
        val vm = LoanViewModel(userRepo, loanRepo, clock, reviewDelayMillis = 0)
        collectState(vm)
        advanceUntilIdle()

        val screen = vm.uiState.value.screen
        assertTrue(screen is LoanScreen.ApprovePending)
        assertEquals(1L, (screen as LoanScreen.ApprovePending).loan.loan.id)
    }

    @Test
    fun `screen - settled loan shows Settled`() = runTest {
        bindMainDispatcher()
        loanRepo.loansFlow.value = listOf(
            Loan(id = 1, userId = 1, principal = 3_000.0, annualRateBp = 780, termMonths = 3, startDate = today, status = LoanStatus.SETTLED),
        )
        val vm = LoanViewModel(userRepo, loanRepo, clock, reviewDelayMillis = 0)
        collectState(vm)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.screen is LoanScreen.Settled)
    }

    // ---- 测试用内存 Fake ----

    /** 直接在仓库里造一笔 ACTIVE 贷款（绕过机审流程），返回贷款 id */
    private fun seedActiveLoanWithSchedules(schedules: List<RepaymentSchedule>): Long {
        val loan = Loan(
            id = 1,
            userId = 1,
            principal = 10_000.0,
            annualRateBp = 780,
            termMonths = schedules.size,
            startDate = today,
            status = LoanStatus.ACTIVE,
        )
        loanRepo.loansFlow.value = listOf(loan)
        loanRepo.schedulesFlow.value = schedules
        return loan.id
    }

    private fun schedule(period: Int, dueDate: LocalDate, isPaid: Boolean) = RepaymentSchedule(
        id = period.toLong(),
        loanId = 1,
        period = period,
        dueDate = dueDate,
        amountDue = 3_400.0,
        isPaid = isPaid,
    )
}

private class FakeUserRepository(initialUser: User?) : UserRepository {
    private val flow = MutableStateFlow(initialUser)

    override fun getFirstUser() = flow

    override suspend fun insertUser(user: User): Long {
        flow.value = user
        return user.id
    }
}

private class FakeLoanRepository : LoanRepository {
    val loansFlow = MutableStateFlow<List<Loan>>(emptyList())
    val schedulesFlow = MutableStateFlow<List<RepaymentSchedule>>(emptyList())
    private var nextLoanId = 1L
    private var nextScheduleId = 1_000L

    override fun getLoans() = loansFlow

    override fun getSchedulesForLoan(loanId: Long) =
        schedulesFlow.map { list -> list.filter { it.loanId == loanId } }

    override fun getAllSchedules() = schedulesFlow

    override suspend fun insertLoanWithSchedules(loan: Loan, schedules: List<RepaymentSchedule>): Long {
        val loanId = insertLoan(loan)
        schedulesFlow.value = schedulesFlow.value + schedules
        return loanId
    }

    override suspend fun insertLoan(loan: Loan): Long {
        val id = nextLoanId++
        loansFlow.value = loansFlow.value + loan.copy(id = id)
        return id
    }

    override suspend fun updateLoanStatus(loanId: Long, status: LoanStatus) {
        loansFlow.value = loansFlow.value.map { if (it.id == loanId) it.copy(status = status) else it }
    }

    override suspend fun markSchedulePaid(scheduleId: Long) {
        schedulesFlow.value = schedulesFlow.value.map {
            if (it.id == scheduleId) it.copy(isPaid = true) else it
        }
    }

    override suspend fun activateLoan(loanId: Long, today: LocalDate): Boolean {
        val loan = loansFlow.value.firstOrNull { it.id == loanId } ?: return false
        if (loan.status != LoanStatus.APPROVED) return false
        schedulesFlow.value = schedulesFlow.value + (1..loan.termMonths).map { period ->
            RepaymentSchedule(
                id = nextScheduleId++,
                loanId = loanId,
                period = period,
                dueDate = today.plusMonths(period.toLong()),
                amountDue = loan.principal / loan.termMonths,
            )
        }
        updateLoanStatus(loanId, LoanStatus.ACTIVE)
        return true
    }
}
