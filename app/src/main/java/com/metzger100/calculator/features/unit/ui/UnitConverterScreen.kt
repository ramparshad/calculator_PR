package com.metzger100.calculator.features.unit.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.metzger100.calculator.R
import com.metzger100.calculator.features.unit.viewmodel.UnitConverterViewModel
import com.metzger100.calculator.features.unit.ui.UnitConverterConstants.UnitDef
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun UnitConverterScreen(
    viewModel: UnitConverterViewModel,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope
) {
    val uiState by viewModel::uiState

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val keyboardHeight = maxHeight * 0.5f

        Column(
            Modifier
                .fillMaxSize()
                .padding(bottom = keyboardHeight)
        ) {
            UnitRow(
                labelRes       = uiState.fromUnit.nameRes,
                value          = uiState.fromValue,
                isSel          = (uiState.selectedField == 1),
                units          = viewModel.availableUnits,
                onUnitSelected = viewModel::onFromUnitChanged,
                onClick        = { viewModel.onSelectField(1) },
                formatNumber   = { str, short -> viewModel.formatNumber(str, short) },
                snackbarHostState = snackbarHostState,
                coroutineScope = coroutineScope
            )
            Spacer(Modifier.height(8.dp))
            UnitRow(
                labelRes       = uiState.toUnit.nameRes,
                value          = uiState.toValue,
                isSel          = (uiState.selectedField == 2),
                units          = viewModel.availableUnits,
                onUnitSelected = viewModel::onToUnitChanged,
                onClick        = { viewModel.onSelectField(2) },
                formatNumber   = { str, short -> viewModel.formatNumber(str, short) },
                snackbarHostState = snackbarHostState,
                coroutineScope = coroutineScope
            )
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(keyboardHeight)
                .align(Alignment.BottomCenter)
        ) {
            UnitConverterKeyboard(
                onInput = { label ->
                    val current = if (viewModel.uiState.selectedField == 1)
                        viewModel.uiState.fromValue
                    else
                        viewModel.uiState.toValue

                    viewModel.onValueChange(current + label)
                },
                onClear = { viewModel.onValueChange("") },
                onBack = {
                    val current = if (viewModel.uiState.selectedField == 1)
                        viewModel.uiState.fromValue
                    else
                        viewModel.uiState.toValue

                    if (current.isNotEmpty())
                        viewModel.onValueChange(current.dropLast(1))
                }
            )
        }
    }
}

@Composable
fun UnitRow(
    @StringRes labelRes: Int,
    value: String,
    isSel: Boolean,
    units: List<UnitDef>,
    onUnitSelected: (UnitDef) -> Unit,
    onClick: () -> Unit,
    formatNumber: (String, Boolean) -> String,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope
) {
    var show by remember { mutableStateOf(false) }
    val borderM = if (isSel) Modifier.border(
        2.dp,
        MaterialTheme.colorScheme.primary,
        MaterialTheme.shapes.medium
    ) else Modifier

    val clipboard = LocalClipboard.current

    Card(
        Modifier
            .fillMaxWidth()
            .then(borderM)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val unitLabel = stringResource(labelRes)
            val changeUnitDesc = stringResource(R.string.change_unit_content_description, unitLabel)
            Text(
                text = stringResource(labelRes),
                fontSize = 18.sp,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .clickable { show = true }
                    .semantics {
                        contentDescription = changeUnitDesc
                    }
            )
            Spacer(Modifier.width(16.dp))

            val displayText = if (isSel) {
                value.ifEmpty { "0" }
            } else {
                formatNumber(value, false)
            }

            Text(
                text = displayText,
                fontSize = if (isSel) 24.sp else 20.sp,
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
                softWrap = true,
                maxLines = Int.MAX_VALUE
            )

            if (value.isNotEmpty() && value != "0") {
                val snackDesc = stringResource(R.string.value_copied)
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            clipboard.setClipEntry(
                                ClipEntry(
                                    ClipData.newPlainText("Currency Value", value)
                                )
                            )
                            snackbarHostState.showSnackbar(
                                message = snackDesc,
                                withDismissAction = true
                            )
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.copy_value)
                    )
                }
            }
        }
    }

    if (show) {
        UnitSelectorDialogRV(
            units = units,
            onUnitSelected = {
                onUnitSelected(it)
                show = false
            },
            onDismissRequest = { show = false }
        )
    }
}

@Composable
fun UnitSelectorDialogRV(
    units: List<UnitDef>,
    onUnitSelected: (UnitDef) -> Unit,
    onDismissRequest: () -> Unit
) {
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()

    Dialog(onDismissRequest = onDismissRequest) {
        BoxWithConstraints {
            val maxDialogHeight = maxHeight * 0.75f

            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .heightIn(max = maxDialogHeight)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { context ->
                        RecyclerView(context).apply {
                            layoutManager = LinearLayoutManager(context)
                            adapter = object : RecyclerView.Adapter<UnitViewHolder>() {
                                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UnitViewHolder {
                                    val textView = TextView(context).apply {
                                        setPadding(32, 24, 32, 24)
                                        textSize = 18f
                                        setTextColor(textColor)
                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.WRAP_CONTENT
                                        )
                                    }
                                    return UnitViewHolder(textView)
                                }
                                override fun getItemCount() = units.size
                                override fun onBindViewHolder(holder: UnitViewHolder, position: Int) {
                                    val unit = units[position]
                                    val unitName = holder.textView.context.getString(unit.nameRes)
                                    holder.textView.text = unitName

                                    holder.textView.contentDescription = holder.textView.context.getString(
                                        R.string.select_unit_content_description, unitName
                                    )

                                    holder.itemView.setOnClickListener {
                                        onUnitSelected(unit)
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

private class UnitViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
