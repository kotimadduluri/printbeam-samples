package com.freshcart.ui.shop

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freshcart.model.Product
import com.freshcart.model.formatRupees
import com.freshcart.ui.theme.FreshCartColors
import com.freshcart.ui.theme.FreshCartShapes

@Composable
fun ShopScreen(
    viewModel: ShopViewModel,
    onOpenCart: () -> Unit,
    onOpenPrinterSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    // The filter/sort pipeline is a pure function of state — cache it per state emission
    // instead of re-running it on every recomposition of the grid.
    val visibleProducts = remember(state) { state.visibleProducts }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TopBar(
                cartCount = state.cartCount,
                onOpenCart = onOpenCart,
                onOpenPrinterSettings = onOpenPrinterSettings,
            )

            SearchPill(
                query = state.query,
                onQueryChange = viewModel::onQueryChange,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ChipPill(
                    label = "Sort By",
                    selected = state.sort != SortMode.OFF,
                    onClick = viewModel::cycleSort,
                    trailingIcon = when (state.sort) {
                        SortMode.OFF -> null
                        SortMode.PRICE_LOW_HIGH -> Icons.Rounded.ArrowUpward
                        SortMode.PRICE_HIGH_LOW -> Icons.Rounded.ArrowDownward
                    },
                    stateLabel = when (state.sort) {
                        SortMode.OFF -> "Not sorting"
                        SortMode.PRICE_LOW_HIGH -> "Price low to high"
                        SortMode.PRICE_HIGH_LOW -> "Price high to low"
                    },
                )
                ChipPill(
                    label = when (state.category) {
                        CategoryFilter.ALL -> "Category"
                        CategoryFilter.FRUITS -> "Fruits"
                        CategoryFilter.VEGETABLES -> "Vegetables"
                    },
                    selected = state.category != CategoryFilter.ALL,
                    onClick = viewModel::cycleCategory,
                )
                ChipPill(
                    label = "Offers",
                    selected = state.offersOnly,
                    onClick = viewModel::toggleOffers,
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Fresh Items",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = FreshCartColors.Ink,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            Spacer(Modifier.height(12.dp))

            if (visibleProducts.isEmpty()) {
                EmptySearchState(
                    hasQuery = state.query.isNotBlank(),
                    onClearSearch = viewModel::clearSearch,
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(visibleProducts, key = { it.id }) { product ->
                        ProductCard(
                            product = product,
                            favorite = product.id in state.favorites,
                            cartQuantity = state.cartQuantities[product.id] ?: 0,
                            onToggleFavorite = { viewModel.toggleFavorite(product.id) },
                            onAdd = { viewModel.addToCart(product) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    cartCount: Int,
    onOpenCart: () -> Unit,
    onOpenPrinterSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "FreshCart",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = FreshCartColors.Ink,
            modifier = Modifier.weight(1f),
        )
        // Persistent entry point for configuring or switching printers — muted so the
        // cart stays the top bar's primary action.
        IconButton(onClick = onOpenPrinterSettings) {
            Icon(
                Icons.Outlined.Print,
                contentDescription = "Printer settings",
                tint = FreshCartColors.Muted,
            )
        }
        Box {
            IconButton(onClick = onOpenCart) {
                Icon(
                    Icons.Outlined.ShoppingCart,
                    contentDescription = if (cartCount == 0) "Cart" else "Cart, $cartCount items",
                    tint = FreshCartColors.Ink,
                )
            }
            if (cartCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = 4.dp)
                        .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                        .clip(CircleShape)
                        .background(FreshCartColors.Accent),
                    contentAlignment = Alignment.Center,
                ) {
                    // Count change gets a quick scale+fade so a tap on any card's add
                    // button visibly lands in the badge.
                    AnimatedContent(
                        targetState = cartCount,
                        transitionSpec = {
                            (fadeIn(tween(150)) + scaleIn(tween(150), initialScale = 0.6f))
                                .togetherWith(fadeOut(tween(100)))
                        },
                        label = "cartBadge",
                    ) { count ->
                        Text(
                            count.toString(),
                            color = FreshCartColors.OnAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 5.dp),
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
        shape = CircleShape,
        color = FreshCartColors.Surface,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Search,
                contentDescription = null,
                tint = FreshCartColors.Muted,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = TextStyle(fontSize = 15.sp, color = FreshCartColors.Ink),
                cursorBrush = SolidColor(FreshCartColors.Accent),
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                "Search for groceries…",
                                fontSize = 15.sp,
                                color = FreshCartColors.Muted,
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }
    }
}

@Composable
private fun ChipPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    stateLabel: String? = null,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) FreshCartColors.Accent else FreshCartColors.Surface,
        contentColor = if (selected) FreshCartColors.OnAccent else FreshCartColors.Ink,
        shadowElevation = 1.dp,
        modifier = if (stateLabel != null) {
            Modifier.semantics { contentDescription = "$label, $stateLabel" }
        } else {
            Modifier
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            if (trailingIcon != null) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    trailingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun ProductCard(
    product: Product,
    favorite: Boolean,
    cartQuantity: Int,
    onToggleFavorite: () -> Unit,
    onAdd: () -> Unit,
) {
    Surface(
        shape = FreshCartShapes.Card,
        color = FreshCartColors.Surface,
        shadowElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
            ) {
                // Emoji stands in for product photography — deliberate and dependency-free.
                Text(
                    product.emoji,
                    fontSize = 56.sp,
                    modifier = Modifier.align(Alignment.Center),
                )
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp),
                ) {
                    Icon(
                        if (favorite) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (favorite) {
                            "Remove ${product.name} from favorites"
                        } else {
                            "Add ${product.name} to favorites"
                        },
                        tint = if (favorite) FreshCartColors.Favorite else FreshCartColors.Muted,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Surface(shape = FreshCartShapes.Badge, color = FreshCartColors.BadgeBg) {
                Text(
                    product.weight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = FreshCartColors.BadgeText,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                product.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = FreshCartColors.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(FreshCartColors.Fresh),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    "10 MINS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp,
                    color = FreshCartColors.Muted,
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        formatRupees(product.price),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = FreshCartColors.Ink,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        formatRupees(product.mrp),
                        fontSize = 12.sp,
                        color = FreshCartColors.Muted,
                        textDecoration = TextDecoration.LineThrough,
                        modifier = Modifier.padding(bottom = 1.dp),
                    )
                }
                AddButton(
                    productName = product.name,
                    count = cartQuantity,
                    onClick = onAdd,
                )
            }
        }
    }
}

/**
 * Circular accent add button. Shows "+" until the item is in the cart, then the count.
 * Tapping always increments. Press feedback scales to ~0.96 per the motion spec.
 */
@Composable
private fun AddButton(
    productName: String,
    count: Int,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "addPress",
    )

    Box(
        modifier = Modifier
            .size(40.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(FreshCartColors.Accent)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = FreshCartColors.OnAccent),
                onClick = onClick,
            )
            .semantics { contentDescription = "Add $productName to cart" },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = count,
            transitionSpec = {
                (fadeIn(tween(150)) + scaleIn(tween(150), initialScale = 0.6f))
                    .togetherWith(fadeOut(tween(100)))
            },
            label = "addCount",
        ) { current ->
            if (current == 0) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = null,
                    tint = FreshCartColors.OnAccent,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Text(
                    current.toString(),
                    color = FreshCartColors.OnAccent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun EmptySearchState(
    hasQuery: Boolean,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.SearchOff,
            contentDescription = null,
            tint = FreshCartColors.Muted,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "No groceries match your search",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = FreshCartColors.Ink,
            textAlign = TextAlign.Center,
        )
        if (hasQuery) {
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onClearSearch) {
                Text("Clear search", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
