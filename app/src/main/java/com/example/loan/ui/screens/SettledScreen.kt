package com.example.loan.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.loan.ui.LoanUiModel
import com.example.loan.ui.components.formatAmount

/**
 * 已结清：结清提示横幅作为 header 传入 ApplyForm（同一滚动容器，避免嵌套滚动崩溃）。
 */
@Composable
fun SettledScreen(
    model: LoanUiModel,
    isSubmitting: Boolean,
    onSubmit: (principal: Double, termMonths: Int, annualRateBp: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    ApplyForm(
        isSubmitting = isSubmitting,
        showRejectedHint = false,
        onSubmit = onSubmit,
        modifier = modifier,
        header = { SettledBanner(model) },
    )
}

@Composable
private fun SettledBanner(model: LoanUiModel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "✅ 您的借款已结清",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = "本笔 ${formatAmount(model.loan.principal)} 借款已全部还清。" +
                    "如需再次借款，可在下方重新申请。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
