package com.example.loan.domain.logic

import com.example.loan.domain.model.LoanStatus
import com.example.loan.domain.model.RepaymentSchedule
import java.time.LocalDate

/** 贷款状态流转与逾期判定的唯一规则来源（纯函数，UI/Repository/测试共用） */
object LoanRules {

    /** 还款计划是否逾期：未还款 且 应还日期早于今天 */
    fun isOverdue(schedule: RepaymentSchedule, today: LocalDate): Boolean =
        !schedule.isPaid && schedule.dueDate < today

    /**
     * 根据还款计划计算贷款应处的状态：
     * - 无还款计划（PENDING/APPROVED/REJECTED）→ 保持当前状态
     * - 全部已还 → SETTLED
     * - 有逾期未还 → OVERDUE
     * - 其余 → ACTIVE
     */
    fun computeStatus(
        schedules: List<RepaymentSchedule>,
        today: LocalDate,
        current: LoanStatus,
    ): LoanStatus = when {
        schedules.isEmpty() -> current
        schedules.all { it.isPaid } -> LoanStatus.SETTLED
        schedules.any { isOverdue(it, today) } -> LoanStatus.OVERDUE
        else -> LoanStatus.ACTIVE
    }
}
