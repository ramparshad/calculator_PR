package com.metzger100.calculator.features.unit.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.metzger100.calculator.features.unit.viewmodel.UnitConverterViewModel
import com.metzger100.calculator.features.unit.ui.UnitConverterConstants.UnitDef

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun UnitConverterScreen(
    viewModel: UnitConverterViewModel
) {
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
                labelRes = viewModel.fromUnit.nameRes,
                value    = viewModel.fromValue,
                isSel    = viewModel.selectedField==1,
                units    = viewModel.availableUnits,
                onUnitSelected = viewModel::onFromUnitChanged,
                onClick        = { viewModel.onSelectField(1) }
            )
            Spacer(Modifier.height(8.dp))
            UnitRow(
                labelRes = viewModel.toUnit.nameRes,
                value    = viewModel.toValue,
                isSel    = viewModel.selectedField==2,
                units    = viewModel.availableUnits,
                onUnitSelected = viewModel::onToUnitChanged,
                onClick        = { viewModel.onSelectField(2) }
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
                    val current = if (viewModel.selectedField == 1) viewModel.fromValue else viewModel.toValue
                    viewModel.onValueChange(current + label)
                },
                onClear = { viewModel.onValueChange("") },
                onBack = {
                    val current = if (viewModel.selectedField == 1) viewModel.fromValue else viewModel.toValue
                    if (current.isNotEmpty()) viewModel.onValueChange(current.dropLast(1))
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
    onUnitSelected: (UnitDef)->Unit,
    onClick: ()->Unit
) {
    var show by remember { mutableStateOf(false) }
    val borderM = if (isSel) Modifier.border(
        2.dp,
        MaterialTheme.colorScheme.primary,
        MaterialTheme.shapes.medium
    ) else Modifier

    Card(
        Modifier.fillMaxWidth().height(56.dp).then(borderM).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(labelRes),
                fontSize = 18.sp,
                modifier = Modifier.clickable { show = true })
            Spacer(Modifier.width(16.dp))
            Text(
                text = value.ifEmpty { "0" },
                fontSize = if (isSel) 24.sp else 20.sp,
                modifier = Modifier.weight(1f)
            )
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
                                    holder.textView.text = holder.textView.context.getString(unit.nameRes)
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