package com.example.loan.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.loan.domain.model.LoanStatus
import java.time.LocalDate
import java.util.Locale

fun formatAmount(amount: Double): String =
    String.format(Locale.CHINA, "¥%,.2f", amount)

fun formatDate(date: LocalDate): String = date.toString()

private data class StatusStyle(val label: String, val background: Color, val content: Color)

/** 状态徽章；OVERDUE 使用红色警示配色 */
@Composable
fun LoanStatusChip(status: LoanStatus, modifier: Modifier = Modifier) {
    val style = when (status) {
        LoanStatus.PENDING -> StatusStyle("审核中", Color(0xFFFFF3CD), Color(0xFF856404))
        LoanStatus.APPROVED -> StatusStyle("待放款", Color(0xFFD1ECF1), Color(0xFF0C5460))
        LoanStatus.REJECTED -> StatusStyle("已拒绝", Color(0xFFF8D7DA), Color(0xFF721C24))
        LoanStatus.ACTIVE -> StatusStyle("还款中", Color(0xFFD1ECF1), Color(0xFF0C5460))
        LoanStatus.OVERDUE -> StatusStyle("已逾期", Color(0xFFF8D7DA), Color(0xFF721C24))
        LoanStatus.SETTLED -> StatusStyle("已结清", Color(0xFFD4EDDA), Color(0xFF155724))
    }
    Surface(
        modifier = modifier,
        color = style.background,
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            text = style.label,
            color = style.content,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
