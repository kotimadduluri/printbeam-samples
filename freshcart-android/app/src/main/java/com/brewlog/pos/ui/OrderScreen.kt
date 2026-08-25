package com.brewlog.pos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.brewlog.pos.data.Menu
import com.brewlog.pos.data.MenuItem
import com.brewlog.pos.data.formatPrice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderScreen(
    viewModel: OrderViewModel,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val event by viewModel.events.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(event) {
        val e = event ?: return@LaunchedEffect
        val message = when (e) {
            is OrderEvent.PrintSucceeded -> e.message
            is OrderEvent.PrintFailed -> "Print failed: ${e.message}"
            is OrderEvent.ConfigError -> e.message
        }
        snackbarHost.showSnackbar(message)
        viewModel.consumeEvent()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BrewLog POS") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        OrderContent(
            state = state,
            onAdd = viewModel::addItem,
            onClear = viewModel::clear,
            onPrint = viewModel::printReceipt,
            padding = padding,
        )
    }
}

@Composable
private fun OrderContent(
    state: OrderUiState,
    onAdd: (MenuItem) -> Unit,
    onClear: () -> Unit,
    onPrint: () -> Unit,
    padding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
    ) {
        Text("Menu", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(Menu.items) { item ->
                MenuItemRow(item, onClick = { onAdd(item) })
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        Text("Order (${state.items.size} item${if (state.items.size == 1) "" else "s"})",
            style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        if (state.items.isEmpty()) {
            Text("Tap menu items above to add.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
            ) {
                items(state.items) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(item.name)
                        Text(item.priceLabel)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Total: ${formatPrice(state.totalCents)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onClear,
                modifier = Modifier.weight(1f),
                enabled = state.items.isNotEmpty() && !state.printing,
            ) { Text("Clear") }

            Button(
                onClick = onPrint,
                modifier = Modifier.weight(1f),
                enabled = state.items.isNotEmpty() && !state.printing,
            ) { Text(if (state.printing) "Printing..." else "Print Receipt") }
        }
    }
}

@Composable
private fun MenuItemRow(item: MenuItem, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(item.name, style = MaterialTheme.typography.bodyLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.priceLabel)
                Spacer(Modifier.height(0.dp))
                Box(modifier = Modifier.padding(start = 12.dp)) {
                    Button(onClick = onClick) { Text("Add") }
                }
            }
        }
    }
}
