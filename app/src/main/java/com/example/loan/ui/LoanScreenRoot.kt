package com.example.loan.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.loan.ui.screens.ApproveScreen
import com.example.loan.ui.screens.ApplyForm
import com.example.loan.ui.screens.RepaymentScreen
import com.example.loan.ui.screens.ReviewingScreen
import com.example.loan.ui.screens.SettledScreen
import kotlinx.coroutines.flow.collectLatest

/** 根视图：接收 UiState，按 screen 分发到各子屏；错误统一走 Snackbar */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanScreenRoot(
    viewModel: LoanViewModel,
    modifier: Modifier = Modifier,
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.uiState.collectLatest { uiState ->
            val error = uiState.error ?: return@collectLatest
            snackbarHostState.showSnackbar(error.toMessage())
            viewModel.dismissError()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("借贷") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        when (val screen = state.value.screen) {
            LoanScreen.Loading -> ReviewingScreen(
                modifier = Modifier.padding(innerPadding),
                title = "加载中…",
                subtitle = "正在读取借款数据",
            )
            is LoanScreen.ApplyForm -> ApplyForm(
                isSubmitting = state.value.isSubmitting,
                showRejectedHint = screen.showRejectedHint,
                onSubmit = viewModel::applyForLoan,
                modifier = Modifier.padding(innerPadding),
            )
            LoanScreen.Reviewing -> ReviewingScreen(Modifier.padding(innerPadding))
            is LoanScreen.ApprovePending -> ApproveScreen(
                model = screen.loan,
                onDisburse = viewModel::disburseLoan,
                modifier = Modifier.padding(innerPadding),
            )
            is LoanScreen.Repayment -> RepaymentScreen(
                model = screen.loan,
                onRepay = viewModel::repaySchedule,
                modifier = Modifier.padding(innerPadding),
            )
            is LoanScreen.Settled -> SettledScreen(
                model = screen.loan,
                isSubmitting = state.value.isSubmitting,
                onSubmit = viewModel::applyForLoan,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

private fun UiError.toMessage(): String = when (this) {
    is UiError.InvalidAmount -> "请输入有效金额"
    is UiError.UserMissing -> "用户信息缺失，请重启应用"
    is UiError.InvalidTransition -> "当前状态不允许「$action」"
    is UiError.Unknown -> detail
}
