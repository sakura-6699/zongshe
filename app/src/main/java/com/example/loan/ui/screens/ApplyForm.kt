package com.example.loan.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.loan.domain.model.LoanProductConfig
import com.example.loan.ui.components.formatAmount

/**
 * 贷款申请表单：金额输入 + 期限选择 + 固定利率说明 + 提交。
 * 金额是纯输入态，保存在 Composable 本地（rememberSaveable），不进 ViewModel。
 * [header] 渲染在表单顶部同一滚动容器内（如已结清页的横幅），避免嵌套滚动。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplyForm(
    isSubmitting: Boolean,
    showRejectedHint: Boolean,
    onSubmit: (principal: Double, termMonths: Int, annualRateBp: Int) -> Unit,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
) {
    var amountText by rememberSaveable { mutableStateOf("") }
    var selectedTerm by rememberSaveable { mutableStateOf(LoanProductConfig.ALLOWED_TERMS.first()) }
    val amount = amountText.toDoubleOrNull()
    val isValidAmount = amount != null && amount > 0.0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        header?.invoke()
        if (showRejectedHint) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "很抱歉，您的最近一笔申请综合评分不足，已被拒绝。可调整金额后重新申请。",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        OutlinedTextField(
            value = amountText,
            onValueChange = { text -> amountText = text.filter { it.isDigit() || it == '.' } },
            label = { Text("借款金额（元）") },
            supportingText = { Text("可用额度与信用状况将决定审核结果") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            isError = amountText.isNotEmpty() && !isValidAmount,
            modifier = Modifier.fillMaxWidth(),
        )

        Text("借款期限", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LoanProductConfig.ALLOWED_TERMS.forEach { term ->
                FilterChip(
                    selected = selectedTerm == term,
                    onClick = { selectedTerm = term },
                    label = { Text("$term 期") },
                )
            }
        }

        Text(
            text = "年利率 ${(LoanProductConfig.DEFAULT_ANNUAL_RATE_BP / 100.0)}%，" +
                "等额本金还款；$selectedTerm 期月供约 " +
                formatAmount(amount?.let { it / selectedTerm } ?: 0.0),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(
            onClick = { onSubmit(amount ?: 0.0, selectedTerm, LoanProductConfig.DEFAULT_ANNUAL_RATE_BP) },
            enabled = isValidAmount && !isSubmitting,
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(if (isSubmitting) "提交中…" else "提交申请")
        }
    }
}
