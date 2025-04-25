package com.metzger100.calculator.features.calculator.ui

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.metzger100.calculator.features.calculator.model.CalculatorMode
import com.metzger100.calculator.features.calculator.viewmodel.CalculatorViewModel

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun CalculatorScreen(viewModel: CalculatorViewModel) {
    var keyboardVisible by remember { mutableStateOf(false) }

    // reversed history snapshot
    val reversedHistory by remember(viewModel.history) {
        derivedStateOf { viewModel.history.reversed() }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val keyboardHeight = maxHeight * 0.5f
        val textColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
        val resultColor = MaterialTheme.colorScheme.primary.toArgb()

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // --- replaced LazyColumn with RecyclerView ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val context = LocalContext.current

                AndroidView(
                    factory = {
                        RecyclerView(context).apply {
                            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, /*reverseLayout=*/ true)
                            adapter = CalculationAdapter(
                                textColor,
                                resultColor
                            )
                        }
                    },
                    update = { rv ->
                        val adapter = rv.adapter as CalculationAdapter
                        adapter.updateData(reversedHistory)
                        if (reversedHistory.isNotEmpty()) {
                            // always show newest at top
                            rv.scrollToPosition(0)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Input card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { keyboardVisible = true },
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    if (viewModel.previewResult.isNotEmpty()) {
                        Text(
                            text = viewModel.input,
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "= ${viewModel.previewResult}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = viewModel.input,
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Keyboard
            if (keyboardVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(keyboardHeight)
                ) {
                    if (viewModel.mode == CalculatorMode.STANDARD) {
                        StandardKeyboard(
                            onInput = viewModel::onInput,
                            onClear = viewModel::clear,
                            onBack = viewModel::backspace,
                            onEquals = { viewModel.calculate() },
                            onToggleMode = viewModel::toggleMode
                        )
                    } else {
                        ScientificKeyboard(
                            inverse = viewModel.inverseMode,
                            onInput = viewModel::onInput,
                            onClear = viewModel::clear,
                            onBack = viewModel::backspace,
                            onEquals = { viewModel.calculate() },
                            onToggleMode = viewModel::toggleMode,
                            onToggleInverse = viewModel::toggleInverse,
                            onToggleDegreeMode = viewModel::toggleDegreeMode,
                            isDegreeMode = viewModel.isDegreeMode
                        )
                    }
                }
            }
        }
    }
}

// --- RecyclerView Adapter & ViewHolder ---

private class CalculationAdapter(
    private val textColor: Int,
    private val resultColor: Int
) : RecyclerView.Adapter<CalculationViewHolder>() {

    private var items: List<com.metzger100.calculator.data.local.CalculationEntity> = emptyList()

    fun updateData(newItems: List<com.metzger100.calculator.data.local.CalculationEntity>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalculationViewHolder {
        val ctx = parent.context
        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(16, 12, 16, 12)
        }
        val inputTv = TextView(ctx).apply {
            textSize = 20f
            setTextColor(textColor)
        }
        val resultTv = TextView(ctx).apply {
            textSize = 20f
            setTextColor(resultColor)
        }
        container.addView(inputTv)
        container.addView(resultTv)
        return CalculationViewHolder(container, inputTv, resultTv)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: CalculationViewHolder, position: Int) {
        val entry = items[position]
        holder.inputView.text = entry.input
        holder.resultView.text = "= ${entry.result}"
    }
}

private class CalculationViewHolder(
    view: View,
    val inputView: TextView,
    val resultView: TextView
) : RecyclerView.ViewHolder(view)
