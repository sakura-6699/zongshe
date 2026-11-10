package com.example.loan.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.loan.domain.logic.LoanRules
import com.example.loan.domain.model.LoanStatus
import com.example.loan.ui.LoanUiModel
import com.example.loan.ui.components.LoanStatusChip
import com.example.loan.ui.components.formatAmount
import com.example.loan.ui.components.formatDate

/** 还款中/逾期屏：贷款摘要 + 还款计划列表 */
@Composable
fun RepaymentScreen(
    model: LoanUiModel,
    onRepay: (loanId: Long, scheduleId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { LoanSummary(model) }
        item {
            Text("还款计划", style = MaterialTheme.typography.titleMedium)
        }
        items(model.schedules, key = { it.id }) { schedule ->
            RepaymentItem(
                period = schedule.period,
                dueDate = schedule.dueDate,
                amountDue = schedule.amountDue,
                isPaid = schedule.isPaid,
                isOverdue = !schedule.isPaid && LoanRules.isOverdue(schedule, model.today),
                onRepay = { onRepay(model.loan.id, schedule.id) },
            )
        }
    }
}

@Composable
private fun LoanSummary(model: LoanUiModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("借款金额", style = MaterialTheme.typography.labelMedium)
                    Text(
                        formatAmount(model.loan.principal),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                LoanStatusChip(model.loan.status)
            }
            HorizontalDivider()
            SummaryRow2("待还总额", formatAmount(model.totalDue))
            SummaryRow2("已还总额", formatAmount(model.totalPaid))
            if (model.loan.status == LoanStatus.OVERDUE || model.hasOverdue) {
                Text(
                    text = "⚠ 您有逾期未还的账单，请尽快还款",
                    color = Color(0xFFB3261E),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun SummaryRow2(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RepaymentItem(
    period: Int,
    dueDate: java.time.LocalDate,
    amountDue: Double,
    isPaid: Boolean,
    isOverdue: Boolean,
    onRepay: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "第 $period 期",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (isOverdue) {
                        Text(
                            "  已逾期",
                            color = Color(0xFFB3261E),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Text(
                    "到期日 ${formatDate(dueDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatAmount(amountDue),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                if (isPaid) {
                    Text(
                        "已还清",
                        color = Color(0xFF2E7D32),
                        style = MaterialTheme.typography.labelMedium,
                    )
                } else {
                    Button(onClick = onRepay, modifier = Modifier.padding(top = 4.dp)) {
                        Text("还款")
                    }
                }
            }
        }
    }
}
