package com.jumpmaster.app.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jumpmaster.app.ui.theme.JumpMasterButtonShape

@Composable
fun CounterHomeScreen(
    onOpenTraining: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CounterHomeViewModel = hiltViewModel(),
) {
    val state by viewModel.homeState.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = scheme.background,
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .padding(top = 12.dp, bottom = 104.dp),
            ) {
                item {
                    CounterHomeSummaryCard(
                        totalJumps = state.totalJumps,
                        sessionCount = state.sessionCount,
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
                item { CounterContributionHeatmap(dailyTotals = state.dailyTotals) }
            }
            Button(
                onClick = onOpenTraining,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .fillMaxWidth()
                        .height(58.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = JumpMasterButtonShape,
                            spotColor = scheme.primary.copy(alpha = 0.55f),
                            ambientColor = scheme.primary.copy(alpha = 0.2f),
                        ),
                shape = JumpMasterButtonShape,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = scheme.primary,
                        contentColor = scheme.onPrimary,
                    ),
                elevation =
                    ButtonDefaults.buttonElevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 2.dp,
                    ),
            ) {
                Text(
                    text = "去跳绳",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
