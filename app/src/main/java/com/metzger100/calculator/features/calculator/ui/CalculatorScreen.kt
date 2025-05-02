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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.metzger100.calculator.data.local.entity.CalculationEntity
import com.metzger100.calculator.features.calculator.model.CalculatorMode
import com.metzger100.calculator.features.calculator.viewmodel.CalculatorViewModel

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun CalculatorScreen(viewModel: CalculatorViewModel = hiltViewModel()) {
    var keyboardVisible by remember { mutableStateOf(false) }

    // observe the single uiState
    val uiState by viewModel::uiState

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

        Column(modifier = Modifier.fillMaxSize()) {
            // History list with RecyclerView
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
                                viewModel,
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
                            rv.scrollToPosition(0)
                        }
                    },
                    modifier = Modifier.fillMaxSize().clipToBounds()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Input & preview card
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
                    IconButton(onClick = { keyboardVisible = !keyboardVisible }) {
                        Icon(
                            imageVector = if (keyboardVisible) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = "Toggle Keyboard",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        if (uiState.preview.isNotEmpty()) {
                            Text(
                                text = viewModel.formatNumber(uiState.input, shortMode = false, inputLine = true),
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "= ${viewModel.formatNumber(uiState.preview, shortMode = false, inputLine = false)}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = viewModel.formatNumber(uiState.input, shortMode = false, inputLine = true),
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Keyboard area
            if (keyboardVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(keyboardHeight)
                ) {
                    if (uiState.mode == CalculatorMode.STANDARD) {
                        StandardKeyboard(
                            onInput = viewModel::onInput,
                            onClear = viewModel::clear,
                            onBack = viewModel::backspace,
                            onEquals = viewModel::calculate,
                            onToggleMode = viewModel::toggleMode
                        )
                    } else {
                        ScientificKeyboard(
                            inverse = uiState.inverse,
                            onInput = viewModel::onInput,
                            onClear = viewModel::clear,
                            onBack = viewModel::backspace,
                            onEquals = viewModel::calculate,
                            onToggleMode = viewModel::toggleMode,
                            onToggleInverse = viewModel::toggleInverse,
                            onToggleDegreeMode = viewModel::toggleDegreeMode,
                            isDegreeMode = uiState.isDegree
                        )
                    }
                }
            }
        }
    }
}

// RecyclerView Adapter & ViewHolder remain unchanged except for input/result via uiState
private class CalculationAdapter(
    private val viewModel: CalculatorViewModel,
    private val textColor: Int,
    private val resultColor: Int
) : RecyclerView.Adapter<CalculationViewHolder>() {

    private var items: List<CalculationEntity> = emptyList()

    fun updateData(newItems: List<CalculationEntity>) {
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
        holder.inputView.text = viewModel.formatNumber(entry.input, shortMode = false, inputLine = false)
        holder.resultView.text = "= ${viewModel.formatNumber(entry.result, shortMode = false, inputLine = false)}"
    }
}

class CalculationDiffCallback(
    private val oldList: List<CalculationEntity>,
    private val newList: List<CalculationEntity>
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