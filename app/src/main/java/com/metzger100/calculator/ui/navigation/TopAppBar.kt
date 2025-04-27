import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metzger100.calculator.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(
    showBackButton: Boolean,
    onBackClick: () -> Unit,
    onClearHistory: () -> Unit,
    onRefreshRates: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        navigationIcon = {
            if (showBackButton) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.Back)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
        },
        title = {
            Text(
                text = stringResource(R.string.TopAppBar_Title)
            )
        },
        actions = {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.TopAppBar_Menu))
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.TopAppBar_PurgeHistory)) },
                    onClick = {
                        onClearHistory()
                        menuExpanded = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.TopAppBar_PurgeHistory)
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.TopAppBar_RefreshRates)) },
                    onClick = {
                        onRefreshRates()
                        menuExpanded = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.TopAppBar_RefreshRates)
                        )
                    }
                )
            }
        }
    )
}