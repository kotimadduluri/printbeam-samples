package com.freshcart.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freshcart.model.Product
import com.freshcart.ui.theme.FreshShapes
import com.freshcart.ui.theme.FreshTokens

@Composable
fun ShopScreen(vm: ShopViewModel, onOpenCart: () -> Unit, onOpenSettings: () -> Unit) {
    val state by vm.uiState.collectAsState()

    Scaffold(containerColor = FreshTokens.Ground) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            ShopTopBar(
                cartCount = state.cartCount,
                onOpenCart = onOpenCart,
                onOpenSettings = onOpenSettings,
            )
            SearchPill(
                query = state.query,
                onQueryChange = vm::onQueryChange,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(12.dp))
            ChipRow(state = state, vm = vm)
            Spacer(Modifier.height(16.dp))
            Text(
                "Fresh Items",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = FreshTokens.Ink,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(8.dp))
            if (state.products.isEmpty()) {
                EmptySearchState(
                    query = state.query,
                    onClearSearch = vm::clearSearch,
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.products, key = { it.id }) { product ->
                        ProductCard(
                            product = product,
                            quantityInCart = state.cartQuantities[product.id] ?: 0,
                            isFavorite = product.id in state.favorites,
                            onAdd = { vm.addToCart(product) },
                            onToggleFavorite = { vm.toggleFavorite(product.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShopTopBar(cartCount: Int, onOpenCart: () -> Unit, onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "FreshCart",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = FreshTokens.Ink,
        )
        Spacer(Modifier.weight(1f))
        // Persistent entry point for configuring or switching the receipt printer.
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onOpenSettings)
                .semantics { contentDescription = "Printer settings" },
            contentAlignment = Alignment.Center,
        ) {
            PrinterIcon(color = FreshTokens.Ink, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onOpenCart)
                .semantics { contentDescription = "Cart" },
            contentAlignment = Alignment.Center,
        ) {
            CartIcon(color = FreshTokens.Ink, modifier = Modifier.size(26.dp))
            if (cartCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(FreshTokens.Accent),
                    contentAlignment = Alignment.Center,
                ) {
                    // The badge animates when the count changes — a small acknowledgment
                    // that the tap on an add button landed.
                    AnimatedContent(
                        targetState = cartCount,
                        transitionSpec = {
                            (slideInVertically(tween(180)) { it } + fadeIn(tween(180)))
                                .togetherWith(slideOutVertically(tween(180)) { -it } + fadeOut(tween(180)))
                        },
                    ) { count ->
                        Text(
                            count.toString(),
                            color = FreshTokens.OnAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchPill(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = FreshShapes.Pill,
        color = FreshTokens.Surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MagnifierIcon(color = FreshTokens.Muted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text("Search for groceries…", color = FreshTokens.Muted, fontSize = 15.sp)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(color = FreshTokens.Ink, fontSize = 15.sp),
                    cursorBrush = SolidColor(FreshTokens.Accent),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ChipRow(state: ShopUiState, vm: ShopViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChipPill(
            // The label carries the current sort direction so the cycling state is legible.
            label = when (state.sort) {
                SortOrder.OFF -> "Sort By"
                SortOrder.PRICE_LOW_HIGH -> "Price: Low to High"
                SortOrder.PRICE_HIGH_LOW -> "Price: High to Low"
            },
            selected = state.sort != SortOrder.OFF,
            onClick = vm::cycleSort,
        )
        FilterChipPill(
            label = state.category?.label ?: "Category",
            selected = state.category != null,
            onClick = vm::cycleCategory,
        )
        FilterChipPill(
            label = "Offers",
            selected = state.offersOnly,
            onClick = vm::toggleOffers,
        )
    }
}

@Composable
private fun FilterChipPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = FreshShapes.Pill,
        color = if (selected) FreshTokens.Accent else FreshTokens.Surface,
        modifier = Modifier
            .clip(FreshShapes.Pill)
            .clickable(onClick = onClick),
    ) {
        Text(
            label,
            color = if (selected) FreshTokens.OnAccent else FreshTokens.Ink,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
        )
    }
}

@Composable
private fun ProductCard(
    product: Product,
    quantityInCart: Int,
    isFavorite: Boolean,
    onAdd: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Surface(
        shape = FreshShapes.Card,
        color = FreshTokens.Surface,
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
            ) {
                Text(
                    product.emoji,
                    fontSize = 64.sp,
                    modifier = Modifier.align(Alignment.Center),
                )
                Text(
                    if (isFavorite) "♥" else "♡",
                    fontSize = 18.sp,
                    color = if (isFavorite) FreshTokens.Favorite else FreshTokens.Muted,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .clickable(onClick = onToggleFavorite)
                        .padding(4.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .clip(FreshShapes.Badge)
                    .background(FreshTokens.BadgeBg),
            ) {
                Text(
                    product.weight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = FreshTokens.BadgeText,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                product.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = FreshTokens.Ink,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(FreshTokens.Fresh),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    "10 MINS",
                    fontSize = 10.sp,
                    letterSpacing = 0.8.sp,
                    color = FreshTokens.Muted,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "₹${product.price}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = FreshTokens.Ink,
                )
                if (product.hasDiscount) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "₹${product.mrp}",
                        fontSize = 12.sp,
                        color = FreshTokens.Muted,
                        textDecoration = TextDecoration.LineThrough,
                    )
                }
                Spacer(Modifier.weight(1f))
                AddButton(quantityInCart = quantityInCart, onAdd = onAdd)
            }
        }
    }
}

@Composable
private fun AddButton(quantityInCart: Int, onAdd: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // Press feedback per the motion spec: a quick ~0.96 scale, nothing decorative.
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, tween(150))
    Box(
        modifier = Modifier
            .size(36.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(FreshTokens.Accent)
            .clickable(interactionSource = interaction, indication = null, onClick = onAdd),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            // Once the item is in the cart, the button shows how many — tapping keeps adding.
            if (quantityInCart > 0) quantityInCart.toString() else "+",
            color = FreshTokens.OnAccent,
            fontSize = if (quantityInCart > 0) 14.sp else 20.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun EmptySearchState(
    query: String,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(FreshTokens.Muted.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            MagnifierIcon(color = FreshTokens.Muted, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(
            if (query.isBlank()) "Nothing matches these filters" else "No groceries match \"${query.trim()}\"",
            style = MaterialTheme.typography.titleMedium,
            color = FreshTokens.Ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Try a different name, or browse the full list.",
            style = MaterialTheme.typography.bodySmall,
            color = FreshTokens.Muted,
            textAlign = TextAlign.Center,
        )
        if (query.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onClearSearch) {
                Text("Clear search", color = FreshTokens.Accent)
            }
        }
    }
}
