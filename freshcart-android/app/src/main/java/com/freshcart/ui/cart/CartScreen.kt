package com.freshcart.ui.cart

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freshcart.model.CartItem
import com.freshcart.model.formatRupees
import com.freshcart.printing.formatOrderNumber
import com.freshcart.ui.theme.FreshCartColors
import com.freshcart.ui.theme.FreshCartShapes

@Composable
fun CartScreen(
    viewModel: CartViewModel,
    onBack: () -> Unit,
    onOpenPrinterSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    // "Place Order" with no printer configured hands off to the settings flow.
    LaunchedEffect(state.openPrinterSettings) {
        if (state.openPrinterSettings) {
            viewModel.consumeOpenPrinterSettings()
            onOpenPrinterSettings()
        }
    }

    // Leaving the success state by any route resets the flow for the next order.
    fun leave() {
        if (state.phase is OrderPhase.Success) viewModel.resetPhase()
        onBack()
    }
    BackHandler { leave() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = ::leave) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = FreshCartColors.Ink,
                    )
                }
                Text(
                    "Cart",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = FreshCartColors.Ink,
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.phase is OrderPhase.Success -> SuccessState(
                    orderNumber = (state.phase as OrderPhase.Success).orderNumber,
                    onBackToShop = ::leave,
                )
                state.items.isEmpty() -> EmptyCartState(onBrowse = onBack)
                else -> CartContent(
                    state = state,
                    onIncrement = viewModel::increment,
                    onDecrement = viewModel::decrement,
                    onPlaceOrder = viewModel::placeOrder,
                    onRetry = viewModel::retry,
                )
            }
        }
    }
}

@Composable
private fun CartContent(
    state: CartUiState,
    onIncrement: (String) -> Unit,
    onDecrement: (String) -> Unit,
    onPlaceOrder: () -> Unit,
    onRetry: () -> Unit,
) {
    val printing = state.phase is OrderPhase.Printing
    val failure = state.phase as? OrderPhase.Failure

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.items, key = { it.product.id }) { item ->
                CartLineItem(
                    item = item,
                    enabled = !printing,
                    onIncrement = { onIncrement(item.product.id) },
                    onDecrement = { onDecrement(item.product.id) },
                )
            }
            item {
                SummaryCard(
                    itemsTotal = state.itemsTotal,
                    saved = state.saved,
                    total = state.total,
                )
            }
            if (failure != null) {
                item {
                    FailureCard(message = failure.message, onRetry = onRetry)
                }
            }
        }

        Button(
            onClick = onPlaceOrder,
            enabled = !printing,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = FreshCartColors.Accent,
                contentColor = FreshCartColors.OnAccent,
                disabledContainerColor = FreshCartColors.Accent.copy(alpha = 0.7f),
                disabledContentColor = FreshCartColors.OnAccent,
            ),
        ) {
            if (printing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = FreshCartColors.OnAccent,
                )
                Spacer(Modifier.width(10.dp))
                Text("Printing receipt…", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            } else {
                Text("Place Order", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CartLineItem(
    item: CartItem,
    enabled: Boolean,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    Surface(
        shape = FreshCartShapes.Card,
        color = FreshCartColors.Surface,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(FreshCartShapes.Card)
                    .background(FreshCartColors.Ground),
                contentAlignment = Alignment.Center,
            ) {
                Text(item.product.emoji, fontSize = 26.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.product.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FreshCartColors.Ink,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${item.product.weight} · ${formatRupees(item.product.price)}",
                    fontSize = 12.sp,
                    color = FreshCartColors.Muted,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatRupees(item.lineTotal),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = FreshCartColors.Ink,
                )
                Spacer(Modifier.height(6.dp))
                QuantityStepper(
                    productName = item.product.name,
                    quantity = item.quantity,
                    enabled = enabled,
                    onIncrement = onIncrement,
                    onDecrement = onDecrement,
                )
            }
        }
    }
}

@Composable
private fun QuantityStepper(
    productName: String,
    quantity: Int,
    enabled: Boolean,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    Surface(shape = CircleShape, color = FreshCartColors.Ground) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onDecrement,
                enabled = enabled,
                modifier = Modifier.size(30.dp),
            ) {
                Icon(
                    Icons.Rounded.Remove,
                    contentDescription = if (quantity == 1) {
                        "Remove $productName from cart"
                    } else {
                        "Decrease $productName quantity"
                    },
                    tint = FreshCartColors.Ink,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                quantity.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = FreshCartColors.Ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.defaultMinSize(minWidth = 20.dp),
            )
            IconButton(
                onClick = onIncrement,
                enabled = enabled,
                modifier = Modifier.size(30.dp),
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = "Increase $productName quantity",
                    tint = FreshCartColors.Ink,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(itemsTotal: Int, saved: Int, total: Int) {
    Surface(
        shape = FreshCartShapes.Card,
        color = FreshCartColors.Surface,
        shadowElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SummaryRow(label = "Items", value = formatRupees(itemsTotal))
            if (saved > 0) {
                Spacer(Modifier.height(8.dp))
                SummaryRow(label = "You saved", value = formatRupees(saved))
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Total",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = FreshCartColors.Ink,
                )
                Text(
                    formatRupees(total),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = FreshCartColors.Ink,
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 14.sp, color = FreshCartColors.Muted)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = FreshCartColors.Ink)
    }
}

@Composable
private fun FailureCard(message: String, onRetry: () -> Unit) {
    Surface(
        shape = FreshCartShapes.Card,
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Couldn't print the receipt",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    message,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onRetry) {
                Text("Retry", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SuccessState(orderNumber: Int, onBackToShop: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(FreshCartColors.Accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = FreshCartColors.OnAccent,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Order #${formatOrderNumber(orderNumber)} placed",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = FreshCartColors.Ink,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Receipt printed",
            fontSize = 14.sp,
            color = FreshCartColors.Muted,
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onBackToShop,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text("Back to shop", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EmptyCartState(onBrowse: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.ShoppingCart,
            contentDescription = null,
            tint = FreshCartColors.Muted,
            modifier = Modifier.size(44.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Your cart is empty",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = FreshCartColors.Ink,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Fresh groceries are a tap away.",
            fontSize = 14.sp,
            color = FreshCartColors.Muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onBrowse,
            modifier = Modifier.height(48.dp),
        ) {
            Text("Browse groceries", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
