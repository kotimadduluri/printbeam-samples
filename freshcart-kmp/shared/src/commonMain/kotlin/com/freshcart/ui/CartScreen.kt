package com.freshcart.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.freshcart.ui.theme.FreshShapes
import com.freshcart.ui.theme.FreshTokens

@Composable
fun CartScreen(
    vm: CartViewModel,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by vm.uiState.collectAsState()

    Scaffold(
        containerColor = FreshTokens.Ground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) { Text("Back", color = FreshTokens.Accent) }
                Spacer(Modifier.width(4.dp))
                Text(
                    "Cart",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = FreshTokens.Ink,
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onOpenSettings) { Text("Printer", color = FreshTokens.Accent) }
            }
        },
    ) { padding ->
        val stage = state.stage
        when {
            stage is OrderStage.Success -> OrderSuccess(
                orderNumber = stage.orderNumber,
                onBackToShop = {
                    vm.resetStage()
                    onBack()
                },
                modifier = Modifier.padding(padding),
            )
            state.items.isEmpty() -> EmptyCart(
                onBrowse = onBack,
                modifier = Modifier.padding(padding),
            )
            else -> CartContent(
                state = state,
                onIncrement = vm::increment,
                onDecrement = vm::decrement,
                onPlaceOrder = { vm.placeOrder(onPrinterNeeded = onOpenSettings) },
                onRetry = { vm.placeOrder(onPrinterNeeded = onOpenSettings) },
                modifier = Modifier.padding(padding),
            )
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
    modifier: Modifier = Modifier,
) {
    val printing = state.stage is OrderStage.Printing
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
    ) {
        Surface(shape = FreshShapes.Card, color = FreshTokens.Surface, tonalElevation = 1.dp) {
            Column(Modifier.padding(vertical = 4.dp)) {
                state.items.forEachIndexed { index, item ->
                    CartLine(
                        item = item,
                        enabled = !printing,
                        onIncrement = { onIncrement(item.product.id) },
                        onDecrement = { onDecrement(item.product.id) },
                    )
                    if (index < state.items.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = FreshTokens.Ground,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Surface(shape = FreshShapes.Card, color = FreshTokens.Surface, tonalElevation = 1.dp) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryRow(label = "Items", value = "₹${state.itemsTotal}")
                if (state.savings > 0) {
                    SummaryRow(label = "You saved", value = "₹${state.savings}")
                }
                HorizontalDivider(color = FreshTokens.Ground)
                Row {
                    Text("Total", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = FreshTokens.Ink)
                    Spacer(Modifier.weight(1f))
                    Text("₹${state.itemsTotal}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = FreshTokens.Ink)
                }
            }
        }

        val failure = state.stage as? OrderStage.Failure
        if (failure != null) {
            Spacer(Modifier.height(12.dp))
            Surface(shape = FreshShapes.Card, color = FreshTokens.Surface, tonalElevation = 1.dp) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Couldn't print the receipt",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(failure.message, style = MaterialTheme.typography.bodySmall, color = FreshTokens.Muted)
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = onRetry) { Text("Retry", color = FreshTokens.Accent) }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onPlaceOrder,
            enabled = !printing,
            shape = FreshShapes.Pill,
            colors = ButtonDefaults.buttonColors(
                containerColor = FreshTokens.Accent,
                contentColor = FreshTokens.OnAccent,
                disabledContainerColor = FreshTokens.Accent.copy(alpha = 0.6f),
                disabledContentColor = FreshTokens.OnAccent,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            if (printing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = FreshTokens.OnAccent,
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
private fun SummaryRow(label: String, value: String) {
    Row {
        Text(label, fontSize = 14.sp, color = FreshTokens.Muted)
        Spacer(Modifier.weight(1f))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = FreshTokens.Ink)
    }
}

@Composable
private fun CartLine(
    item: CartItem,
    enabled: Boolean,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(item.product.emoji, fontSize = 28.sp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "${item.product.name} ${item.product.weight}",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = FreshTokens.Ink,
            )
            Text(
                "₹${item.product.price} each",
                fontSize = 12.sp,
                color = FreshTokens.Muted,
            )
        }
        QuantityStepper(
            quantity = item.quantity,
            enabled = enabled,
            onIncrement = onIncrement,
            onDecrement = onDecrement,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            "₹${item.lineTotal}",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = FreshTokens.Ink,
        )
    }
}

@Composable
private fun QuantityStepper(
    quantity: Int,
    enabled: Boolean,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    Surface(shape = FreshShapes.Pill, color = FreshTokens.Ground) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepperButton(glyph = "−", enabled = enabled, onClick = onDecrement)
            Text(
                quantity.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = FreshTokens.Ink,
            )
            StepperButton(glyph = "+", enabled = enabled, onClick = onIncrement)
        }
    }
}

@Composable
private fun StepperButton(glyph: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            glyph,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) FreshTokens.Accent else FreshTokens.Muted,
        )
    }
}

@Composable
private fun EmptyCart(onBrowse: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(FreshTokens.Muted.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            CartIcon(color = FreshTokens.Muted, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Your cart is empty",
            style = MaterialTheme.typography.titleLarge,
            color = FreshTokens.Ink,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Add fresh groceries from the shop and they'll show up here.",
            style = MaterialTheme.typography.bodyMedium,
            color = FreshTokens.Muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onBrowse,
            shape = FreshShapes.Pill,
            colors = ButtonDefaults.buttonColors(
                containerColor = FreshTokens.Accent,
                contentColor = FreshTokens.OnAccent,
            ),
        ) {
            Text("Browse groceries", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun OrderSuccess(
    orderNumber: Int,
    onBackToShop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CheckBadge(background = FreshTokens.Accent, tint = FreshTokens.OnAccent, size = 72.dp)
        Spacer(Modifier.height(20.dp))
        Text(
            "Order #${orderNumber.toString().padStart(3, '0')} placed",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = FreshTokens.Ink,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Receipt printed",
            style = MaterialTheme.typography.bodyMedium,
            color = FreshTokens.Muted,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onBackToShop,
            shape = FreshShapes.Pill,
            colors = ButtonDefaults.buttonColors(
                containerColor = FreshTokens.Accent,
                contentColor = FreshTokens.OnAccent,
            ),
        ) {
            Text("Back to shop", fontWeight = FontWeight.SemiBold)
        }
    }
}
