package com.metzger100.calculator.features.calculator.ui

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.DiffUtil
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
                    .clipToBounds()
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
                            clipToPadding = true
                            clipChildren = true
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
                    modifier = Modifier.fillMaxSize().clipToBounds()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Input card
            // Input card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { keyboardVisible = true },
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pfeil Button
                    IconButton(
                        onClick = { keyboardVisible = !keyboardVisible }
                    ) {
                        Icon(
                            imageVector = if (keyboardVisible) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = "Toggle Keyboard",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Input + Preview Texts
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        if (viewModel.previewResult.isNotEmpty()) {
                            Text(
                                text = displayifyExpression(viewModel.input),
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "= ${displayifyExpression(viewModel.previewResult)}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = displayifyExpression(viewModel.input),
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
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

fun displayifyExpression(expr: String): String {
    return expr
        .replace("/", "÷")
        .replace("*", "×")
        .replace("-", "−")
        .replace("PI", "π")
        .replace("SQRT(", "√(")
        .replace("1/(", "1/(")
        .replace("FACT(", "x!(")
        .replace("LOG10(", "lg(")
        .replace("LOG(", "ln(")
        .replace("ASINR(", "sin⁻¹(")
        .replace("ACOSR(", "cos⁻¹(")
        .replace("ATANR(", "tan⁻¹(")
        .replace("ASIN(", "sin⁻¹(")
        .replace("ACOS(", "cos⁻¹(")
        .replace("ATAN(", "tan⁻¹(")
        .replace("SINR(", "sin(")
        .replace("COSR(", "cos(")
        .replace("TANR(", "tan(")
        .replace("SIN(", "sin(")
        .replace("COS(", "cos(")
        .replace("TAN(", "tan(")
}

// --- RecyclerView Adapter & ViewHolder ---

private class CalculationAdapter(
    private val textColor: Int,
    private val resultColor: Int
) : RecyclerView.Adapter<CalculationViewHolder>() {

    private var items: List<com.metzger100.calculator.data.local.CalculationEntity> = emptyList()

    fun updateData(newItems: List<com.metzger100.calculator.data.local.CalculationEntity>) {
        val diffCallback = CalculationDiffCallback(items, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        items = newItems
        diffResult.dispatchUpdatesTo(this)
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
        holder.inputView.text = displayifyExpression(entry.input)
        holder.resultView.text = "= ${entry.result}"
    }
}

class CalculationDiffCallback(
    private val oldList: List<com.metzger100.calculator.data.local.CalculationEntity>,
    private val newList: List<com.metzger100.calculator.data.local.CalculationEntity>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}

private class CalculationViewHolder(
    view: View,
    val inputView: TextView,
    val resultView: TextView
) : RecyclerView.ViewHolder(view)
