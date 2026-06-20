package com.example.dailyplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dailyplanner.ui.PlanScreen
import com.example.dailyplanner.ui.theme.DailyPlannerTheme
import com.example.dailyplanner.viewmodel.PlanViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DailyPlannerTheme {
                val viewModel: PlanViewModel = viewModel()
                PlanScreen(viewModel = viewModel)
            }
        }
    }
}
