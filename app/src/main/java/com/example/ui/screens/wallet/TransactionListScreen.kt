package com.example.ui.screens.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionDto
import com.example.ui.theme.*
import com.example.ui.viewmodel.OnChatViewModel
import java.text.SimpleDateFormat
import java.util.*

enum class TransactionDateFilter(val label: String) {
    ALL("Toutes"),
    TODAY("Aujourd'hui"),
    YESTERDAY("Hier"),
    THIS_WEEK("Cette semaine"),
    THIS_MONTH("Ce mois-ci")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    viewModel: OnChatViewModel,
    onBack: () -> Unit,
    onOpenTransaction: (TransactionDto) -> Unit
) {
    val transactions by viewModel.transactions.collectAsState()
    val isLoading by viewModel.isTransactionsLoading.collectAsState()
    
    var selectedFilter by remember { mutableStateOf(TransactionDateFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.fetchTransactions()
    }

    val filteredTransactions = remember(transactions, selectedFilter, searchQuery) {
        transactions.filter { tx ->
            matchesDateFilter(tx, selectedFilter) && matchesSearchQuery(tx, searchQuery)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historique des Transactions", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CashPayNavy,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = CashPayNavyDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search / Date filter text field
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = CashPayNavySurface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Recherche",
                        tint = CashPayGrey,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Rechercher date (ex: 2026-09-27, 27/09), motif...", color = CashPayGrey, fontSize = 13.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = OnChatCyan,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Effacer", tint = CashPayGrey)
                        }
                    }
                }
            }

            // Filter Chips Row
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(TransactionDateFilter.values()) { filter ->
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = {
                            Text(
                                text = filter.label,
                                color = if (isSelected) CashPayNavyDark else Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = CashPayNavySurface,
                            selectedContainerColor = OnChatCyan
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = CashPayNavyBorder,
                            selectedBorderColor = OnChatCyan
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading && transactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = OnChatCyan)
                }
            } else if (filteredTransactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = CashPayGrey,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Aucune transaction pour ce filtre", color = CashPayGrey)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTransactions) { tx ->
                        TransactionItem(tx = tx, onClick = { onOpenTransaction(tx) })
                    }
                }
            }
        }
    }
}

private fun matchesSearchQuery(tx: TransactionDto, query: String): Boolean {
    if (query.isBlank()) return true
    val q = query.lowercase().trim()
    val dateStr = tx.createdAt ?: ""
    val typeStr = tx.type ?: ""
    val descriptionStr = tx.description ?: ""
    val amountStr = tx.amount?.toString() ?: ""
    val senderStr = tx.sender?.fullName ?: tx.senderWalletId ?: ""
    val receiverStr = tx.receiver?.fullName ?: tx.receiverWalletId ?: ""

    return dateStr.lowercase().contains(q) ||
            typeStr.lowercase().contains(q) ||
            descriptionStr.lowercase().contains(q) ||
            amountStr.contains(q) ||
            senderStr.lowercase().contains(q) ||
            receiverStr.lowercase().contains(q)
}

private fun matchesDateFilter(tx: TransactionDto, filter: TransactionDateFilter): Boolean {
    if (filter == TransactionDateFilter.ALL) return true

    val rawDate = tx.createdAt ?: return true
    val txDate = parseDateSafely(rawDate) ?: return true

    val now = Calendar.getInstance()
    val txCal = Calendar.getInstance().apply { time = txDate }

    return when (filter) {
        TransactionDateFilter.TODAY -> {
            now.get(Calendar.YEAR) == txCal.get(Calendar.YEAR) &&
                    now.get(Calendar.DAY_OF_YEAR) == txCal.get(Calendar.DAY_OF_YEAR)
        }
        TransactionDateFilter.YESTERDAY -> {
            val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            yesterday.get(Calendar.YEAR) == txCal.get(Calendar.YEAR) &&
                    yesterday.get(Calendar.DAY_OF_YEAR) == txCal.get(Calendar.DAY_OF_YEAR)
        }
        TransactionDateFilter.THIS_WEEK -> {
            val weekAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
            txCal.after(weekAgo)
        }
        TransactionDateFilter.THIS_MONTH -> {
            now.get(Calendar.YEAR) == txCal.get(Calendar.YEAR) &&
                    now.get(Calendar.MONTH) == txCal.get(Calendar.MONTH)
        }
        else -> true
    }
}

private fun parseDateSafely(dateStr: String): Date? {
    if (dateStr.isBlank()) return null
    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd",
        "dd/MM/yyyy HH:mm:ss",
        "dd/MM/yyyy"
    )
    for (format in formats) {
        try {
            val sdf = SimpleDateFormat(format, Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val date = sdf.parse(dateStr)
            if (date != null) return date
        } catch (e: Exception) {
            // Continue trying other formats
        }
    }
    // Try parsing numeric timestamp milliseconds
    try {
        val millis = dateStr.toLongOrNull()
        if (millis != null) {
            return Date(millis)
        }
    } catch (e: Exception) {
        // Suppress
    }
    return null
}

@Composable
fun TransactionItem(
    tx: TransactionDto,
    onClick: () -> Unit
) {
    val direction = tx.direction?.lowercase() ?: "outgoing"
    val isDebit = direction == "outgoing"
    
    val (title, icon, iconColor) = when (tx.type?.uppercase()) {
        "TR" -> {
            if (isDebit) Triple("Transfert envoyé", Icons.Default.ArrowOutward, Color(0xFFFF5252))
            else Triple("Transfert reçu", Icons.Default.CallReceived, Color(0xFF25D366))
        }
        "DP_AGENT" -> Triple("Dépôt auprès d'un agent", Icons.Default.AddCircle, Color(0xFF25D366))
        "WD" -> Triple("Retrait effectué", Icons.Default.RemoveCircle, Color(0xFFFF5252))
        "LR" -> Triple("Remboursement de prêt", Icons.Default.History, Color(0xFFFF5252))
        else -> Triple(tx.type ?: "Transaction", Icons.Default.Payment, if (isDebit) Color(0xFFFF5252) else Color(0xFF25D366))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = CashPayNavySurface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tx.createdAt ?: "Aujourd'hui",
                    color = CashPayGrey,
                    fontSize = 12.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                val prefix = if (isDebit) "-" else "+"
                Text(
                    text = "$prefix${tx.amount ?: 0.0} ${tx.currency ?: "USD"}",
                    color = iconColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (tx.status?.lowercase() == "success" || tx.status?.lowercase() == "completed") 
                        Color(0xFF25D366).copy(alpha = 0.2f) else Color(0xFFFFB300).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = (tx.status ?: "REUSSI").uppercase(),
                        color = if (tx.status?.lowercase() == "success" || tx.status?.lowercase() == "completed") 
                            Color(0xFF25D366) else Color(0xFFFFB300),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
