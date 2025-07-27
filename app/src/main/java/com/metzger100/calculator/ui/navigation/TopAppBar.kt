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
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metzger100.calculator.R
import com.metzger100.calculator.util.format.FeedbackManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(
    showBackButton: Boolean,
    onBackClick: () -> Unit,
    onClearHistory: () -> Unit,
    onRefreshRates: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val feedbackManager = FeedbackManager.rememberFeedbackManager()
    val coroutineScope = rememberCoroutineScope()
    val hapticEnabled by feedbackManager.prefs.hapticEnabled.collectAsStateWithLifecycle(initialValue = true)
    val soundEnabled by feedbackManager.prefs.soundEnabled.collectAsStateWithLifecycle(initialValue = true)

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

                DropdownMenuItem(
                    text = { Text(if (hapticEnabled) "Disable Haptics" else "Enable Haptics") },
                    onClick = {
                        coroutineScope.launch { feedbackManager.toggleHaptic() }
                        menuExpanded = false
                    },
                    leadingIcon = { Icon(Icons.Default.Vibration, null) }
                )

                DropdownMenuItem(
                    text = { Text(if (soundEnabled) "Disable Sound" else "Enable Sound") },
                    onClick = {
                        coroutineScope.launch { feedbackManager.toggleSound() }
                        menuExpanded = false
                    },
                    leadingIcon = { Icon(Icons.Default.MusicNote, null) }
                )
            }
        }
    )
}