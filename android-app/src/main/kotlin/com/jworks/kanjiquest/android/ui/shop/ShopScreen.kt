package com.jworks.kanjiquest.android.ui.shop

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jworks.kanjiquest.core.domain.model.PurchaseResult
import com.jworks.kanjiquest.core.domain.model.ShopCategory
import com.jworks.kanjiquest.core.domain.model.ShopItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    onBack: () -> Unit,
    viewModel: ShopViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    fun openTutoringJayBooking() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://portal.tutoringjay.com/schedule/book"))
        context.startActivity(intent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("J Coin Shop")
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${uiState.balance.displayBalance} coins",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("\u2190", fontSize = 24.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                LinearProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Featured TutoringJay banner
                uiState.featuredItem?.let { featured ->
                    FeaturedBanner(
                        item = featured,
                        isRedeemed = uiState.hasRedeemedTrial,
                        canAfford = uiState.balance.displayBalance >= featured.cost,
                        onRedeem = { viewModel.purchaseFeatured() },
                        onBookNow = { openTutoringJayBooking() }
                    )
                }

                // Category filter chips
                CategoryFilterRow(
                    categories = uiState.categories,
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelected = { viewModel.selectCategory(it) }
                )

                // Item grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.filteredItems) { item ->
                        val isOwned = item.contentId != null && item.contentId in uiState.ownedContentIds
                        ShopItemCard(
                            item = item,
                            isOwned = isOwned,
                            canAfford = uiState.balance.displayBalance >= item.cost,
                            onClick = {
                                if (!isOwned) viewModel.showPurchaseDialog(item)
                            }
                        )
                    }
                }
            }
        }

        // Purchase dialog
        uiState.purchaseDialogItem?.let { item ->
            val isTutoringJayItem = item.category == ShopCategory.CROSS_BUSINESS &&
                item.id.startsWith("tutoringjay_")
            PurchaseDialog(
                item = item,
                balance = uiState.balance.displayBalance,
                purchaseResult = uiState.purchaseResult,
                onConfirm = { viewModel.confirmPurchase(item) },
                onDismiss = { viewModel.dismissPurchaseDialog() },
                onOpenBooking = if (isTutoringJayItem) {{ openTutoringJayBooking() }} else null
            )
        }
    }
}

@Composable
private fun CategoryFilterRow(
    categories: List<ShopCategory>,
    selectedCategory: ShopCategory?,
    onCategorySelected: (ShopCategory?) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = if (selectedCategory == null) 0 else categories.indexOf(selectedCategory) + 1,
        modifier = Modifier.fillMaxWidth(),
        edgePadding = 12.dp,
        divider = {},
        indicator = {}
    ) {
        FilterChip(
            selected = selectedCategory == null,
            onClick = { onCategorySelected(null) },
            label = { Text("All") },
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        categories.forEach { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(category.displayName) },
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun ShopItemCard(
    item: ShopItem,
    isOwned: Boolean,
    canAfford: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        enabled = !isOwned,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isOwned -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isOwned) 0.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Category icon placeholder
            Text(
                text = categoryIcon(item.category),
                fontSize = 28.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isOwned) {
                Text(
                    text = "Owned",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            } else {
                Text(
                    text = "${item.cost} coins",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (canAfford) Color(0xFFFFD700) else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun PurchaseDialog(
    item: ShopItem,
    balance: Long,
    purchaseResult: PurchaseResult?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onOpenBooking: (() -> Unit)? = null
) {
    val isTutoringJaySuccess = purchaseResult is PurchaseResult.Success &&
        item.category == ShopCategory.CROSS_BUSINESS &&
        item.id.startsWith("tutoringjay_")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (purchaseResult) {
                    is PurchaseResult.Success -> if (isTutoringJaySuccess) "Lesson Redeemed!" else "Purchased!"
                    is PurchaseResult.InsufficientFunds -> "Not Enough Coins"
                    is PurchaseResult.AlreadyOwned -> "Already Owned"
                    is PurchaseResult.Error -> "Error"
                    null -> "Confirm Purchase"
                }
            )
        },
        text = {
            Column {
                when (purchaseResult) {
                    is PurchaseResult.Success -> {
                        if (isTutoringJaySuccess) {
                            Text("Your trial lesson has been redeemed!")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Book your lesson at portal.tutoringjay.com",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "New balance: ${purchaseResult.newBalance} coins",
                                color = Color(0xFFFFD700),
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text("${item.name} unlocked!")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "New balance: ${purchaseResult.newBalance} coins",
                                color = Color(0xFFFFD700),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    is PurchaseResult.InsufficientFunds -> {
                        Text("You need ${purchaseResult.required} coins but only have ${purchaseResult.available}.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Keep studying to earn more J Coins!")
                    }
                    is PurchaseResult.AlreadyOwned -> {
                        Text("You already own ${item.name}.")
                    }
                    is PurchaseResult.Error -> {
                        Text("Something went wrong: ${purchaseResult.message}")
                    }
                    null -> {
                        Text("Buy ${item.name} for ${item.cost} coins?")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your balance: $balance coins",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (balance < item.cost) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Insufficient funds",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (purchaseResult) {
                null -> {
                    Button(
                        onClick = onConfirm,
                        enabled = balance >= item.cost
                    ) {
                        Text("Buy (${item.cost})")
                    }
                }
                else -> {
                    Row {
                        if (isTutoringJaySuccess && onOpenBooking != null) {
                            Button(
                                onClick = {
                                    onOpenBooking()
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE65100)
                                )
                            ) {
                                Text("Book Now")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Button(onClick = onDismiss) {
                            Text("OK")
                        }
                    }
                }
            }
        },
        dismissButton = {
            if (purchaseResult == null) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
private fun FeaturedBanner(
    item: ShopItem,
    isRedeemed: Boolean,
    canAfford: Boolean,
    onRedeem: () -> Unit,
    onBookNow: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFFE65100), Color(0xFFFF8F00))
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "TutoringJay",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Free 30-min Japanese Lesson",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Earn coins playing KanjiQuest, redeem for a real tutoring session!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isRedeemed) {
                        // Redeemed state
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "\u2713",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = onBookNow,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "Book Now",
                                color = Color(0xFFE65100),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        // Cost badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.2f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${item.cost}",
                                color = if (canAfford) Color(0xFFFFD700) else Color(0xFFFF8A80),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = onRedeem,
                            enabled = canAfford,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                disabledContainerColor = Color.White.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "Redeem",
                                color = if (canAfford) Color(0xFFE65100) else Color.White.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun categoryIcon(category: ShopCategory): String = when (category) {
    ShopCategory.THEME -> "\uD83C\uDFA8"       // art palette
    ShopCategory.BOOSTER -> "\u26A1"            // lightning
    ShopCategory.UTILITY -> "\uD83D\uDEE0\uFE0F" // wrench
    ShopCategory.CONTENT -> "\uD83D\uDCDA"      // books
    ShopCategory.CROSS_BUSINESS -> "\u2B50"      // star
}
