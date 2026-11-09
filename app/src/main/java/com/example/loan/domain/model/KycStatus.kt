package com.example.loan.domain.model

/** KYC 认证状态（字段先保留，流程后续阶段实现） */
enum class KycStatus {
    PENDING,
    VERIFIED,
    REJECTED,
}
