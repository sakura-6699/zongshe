package com.example.loan.domain.logic

import com.example.loan.domain.model.LoanStatus
import com.example.loan.domain.model.RepaymentSchedule
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoanRulesTest {

    private val today: LocalDate = LocalDate.of(2026, 11, 9)

    private fun schedule(
        isPaid: Boolean = false,
        dueDate: LocalDate = today.plusDays(10),
    ) = RepaymentSchedule(
        id = 1,
        loanId = 1,
        period = 1,
        dueDate = dueDate,
        amountDue = 3_333.0,
        isPaid = isPaid,
    )

    // ---- isOverdue ----

    @Test
    fun `unpaid past-due schedule is overdue`() {
        assertTrue(LoanRules.isOverdue(schedule(dueDate = today.minusDays(1)), today))
    }

    @Test
    fun `unpaid future schedule is not overdue`() {
        assertFalse(LoanRules.isOverdue(schedule(dueDate = today), today))
    }

    @Test
    fun `paid past-due schedule is not overdue`() {
        assertFalse(LoanRules.isOverdue(schedule(isPaid = true, dueDate = today.minusDays(5)), today))
    }

    // ---- computeStatus ----

    @Test
    fun `no schedules keeps current status`() {
        assertEquals(LoanStatus.PENDING, LoanRules.computeStatus(emptyList(), today, LoanStatus.PENDING))
        assertEquals(LoanStatus.APPROVED, LoanRules.computeStatus(emptyList(), today, LoanStatus.APPROVED))
    }

    @Test
    fun `all paid becomes settled`() {
        val schedules = listOf(schedule(isPaid = true), schedule(isPaid = true))
        assertEquals(LoanStatus.SETTLED, LoanRules.computeStatus(schedules, today, LoanStatus.ACTIVE))
    }

    @Test
    fun `any overdue unpaid becomes overdue`() {
        val schedules = listOf(schedule(isPaid = true), schedule(dueDate = today.minusDays(1)))
        assertEquals(LoanStatus.OVERDUE, LoanRules.computeStatus(schedules, today, LoanStatus.ACTIVE))
    }

    @Test
    fun `unpaid not overdue stays active`() {
        val schedules = listOf(schedule(isPaid = true), schedule())
        assertEquals(LoanStatus.ACTIVE, LoanRules.computeStatus(schedules, today, LoanStatus.ACTIVE))
    }
}
