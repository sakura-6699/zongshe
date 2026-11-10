package com.example.loan.data.repository

import com.example.loan.data.local.entity.RepaymentScheduleEntity
import java.time.LocalDate

/** 还款计划生成：等额本金（每期固定本金 + 剩余本金的当期利息，简化计算，非精确金融公式） */
object RepaymentPlanCalculator {

    fun buildSchedules(
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
}
