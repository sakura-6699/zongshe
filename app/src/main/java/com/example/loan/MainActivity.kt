package com.example.loan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.loan.ui.LoanScreenRoot
import com.example.loan.ui.LoanViewModel
import com.example.loan.ui.theme.LoanTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LoanTheme {
                LoanScreenRoot(viewModel = hiltViewModel())
            }
        }
    }
}
