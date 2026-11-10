package com.example.loan.data.repository

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class RepaymentPlanCalculatorTest {

    @Test
    fun `generates one schedule per month with equal principal and decreasing interest`() {
        val startDate = LocalDate.of(2026, 11, 9)
        val schedules = RepaymentPlanCalculator.buildSchedules(
            loanId = 1,
            principal = 12_000.0,
            annualRateBp = 1200, // 年化 12%，月息 1%
            termMonths = 3,
            startDate = startDate,
        )

        assertEquals(3, schedules.size)
        assertEquals(listOf(1, 2, 3), schedules.map { it.period })
        assertEquals(
            listOf(startDate.plusMonths(1), startDate.plusMonths(2), startDate.plusMonths(3)),
            schedules.map { it.dueDate },
        )
        // 每期本金固定 4000；利息按剩余本金递减：120 / 80 / 40
        assertEquals(4_120.0, schedules[0].amountDue, 1e-6)
        assertEquals(4_080.0, schedules[1].amountDue, 1e-6)
        assertEquals(4_040.0, schedules[2].amountDue, 1e-6)
        assertEquals(false, schedules.any { it.isPaid })
    }
}
