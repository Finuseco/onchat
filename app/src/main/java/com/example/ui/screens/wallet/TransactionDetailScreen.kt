package com.example.ui.screens.wallet

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionDto
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    tx: TransactionDto,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Détails Transaction", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CashPayNavy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = CashPayNavyDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val isDebit = tx.direction?.lowercase() == "outgoing"
            val userTitle = when (tx.type?.uppercase()) {
                "TR" -> if (isDebit) "Transfert envoyé" else "Transfert reçu"
                "DP_AGENT" -> "Dépôt auprès d'un agent"
                "WD" -> "Retrait effectué"
                "LR" -> "Remboursement de prêt"
                "POS_SALE" -> "Achat / Paiement commerçant"
                "EXCHANGE" -> "Conversion de devise"
                else -> tx.type ?: "Transaction"
            }

            // Success Header
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF25D366).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF25D366),
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = userTitle,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
            
            Text(
                text = "${if (isDebit) "-" else "+"}${tx.resolvedAmount} ${tx.resolvedCurrency}",
                color = if (isDebit) Color(0xFFFF5252) else OnChatCyan,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Divider(color = CashPayNavyBorder, modifier = Modifier.padding(vertical = 16.dp))
            
            // Details Grid
            DetailRow(label = "Type Technique", value = tx.type ?: "TR")
            DetailRow(label = "Référence", value = tx.reference ?: "CP-TR-000000")
            DetailRow(label = "Date", value = tx.createdAt ?: "---")
            DetailRow(label = "Statut", value = tx.status?.uppercase() ?: "COMPLETED")
            
            // metadata specific fields
            if (tx.type?.uppercase() == "EXCHANGE") {
                val toAmount = tx.metadata?.get("toAmount")
                val toCurrency = tx.metadata?.get("toCurrency")
                val rate = tx.metadata?.get("rate")
                if (toAmount != null && toCurrency != null) {
                    DetailRow(label = "Montant Converti", value = "$toAmount $toCurrency")
                }
                if (rate != null) {
                    DetailRow(label = "Taux", value = "1 ${tx.resolvedCurrency} = $rate $toCurrency")
                }
            } else if (tx.type?.uppercase() == "POS_SALE") {
                val boutique = tx.metadata?.get("boutique") ?: tx.metadata?.get("vendeur")
                if (boutique != null) {
                    DetailRow(label = "Boutique", value = boutique.toString())
                }
                val referenceVente = tx.metadata?.get("reference_vente") ?: tx.metadata?.get("sale_ref")
                if (referenceVente != null) {
                    DetailRow(label = "Réf. Vente", value = referenceVente.toString())
                }
            }

            tx.otherUser?.let { p ->
                DetailRow(label = if (isDebit) "Bénéficiaire" else "Expéditeur", value = p.fullName ?: "Contact OnChat")
                DetailRow(label = "Wallet ID", value = p.walletId ?: "---")
            }
            
            DetailRow(label = "Frais", value = "${tx.resolvedFee} ${tx.resolvedCurrency}")
            DetailRow(label = "Total Débité", value = "${tx.resolvedAmount} ${tx.resolvedCurrency}")

            Spacer(modifier = Modifier.height(32.dp))
            
            // Receipt Actions
            Text(
                "Options du reçu",
                color = CashPayGrey,
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ReceiptButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.PictureAsPdf,
                    label = "PDF",
                    onClick = { generateReceipt(context, tx, "pdf") }
                )
                ReceiptButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Print,
                    label = "Imprimer",
                    onClick = { generateReceipt(context, tx, "print") }
                )
                ReceiptButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Share,
                    label = "Partager",
                    onClick = { generateReceipt(context, tx, "share") }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, OnChatCyan),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OnChatCyan)
            ) {
                Text("Retour à l'historique")
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = CashPayGrey, fontSize = 14.sp)
        Text(text = value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ReceiptButton(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        color = CashPayNavySurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CashPayNavyBorder),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = OnChatCyan, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun generateReceipt(context: Context, tx: TransactionDto, action: String) {
    val isDebit = tx.direction?.lowercase() == "outgoing"
    val userTitle = when (tx.type?.uppercase()) {
        "TR" -> if (isDebit) "Transfert envoyé" else "Transfert reçu"
        "DP_AGENT" -> "Dépôt auprès d'un agent"
        "WD" -> "Retrait effectué"
        "LR" -> "Remboursement de prêt"
        "POS_SALE" -> "Achat / Paiement commerçant"
        "EXCHANGE" -> "Conversion de devise"
        else -> tx.type ?: "Transaction"
    }

    val receiptText = """
        CASH PAY ALL - REÇU DE TRANSACTION
        ----------------------------------
        Type: $userTitle
        Référence: ${tx.reference}
        Date: ${tx.createdAt}
        Montant: ${if (isDebit) "-" else "+"}${tx.amount} ${tx.currency}
        Statut: ${tx.status?.uppercase() ?: "COMPLETED"}
        Frais: ${tx.fee ?: "0.00"} ${tx.currency}
        ----------------------------------
        Merci d'utiliser CashPay & OnChat.
    """.trimIndent()

    when(action) {
        "pdf", "print" -> {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
            val jobName = "Reçu_${tx.reference}"
            
            // Simplified print using a custom adapter or HTML
            val webView = android.webkit.WebView(context)
            val html = """
                <html>
                <body style="font-family: sans-serif; padding: 20px;">
                    <h2 style="color: #000E38;">CASH PAY ALL</h2>
                    <hr/>
                    <p><b>Type:</b> $userTitle</p>
                    <p><b>Référence:</b> ${tx.reference}</p>
                    <p><b>Date:</b> ${tx.createdAt}</p>
                    <p style="font-size: 20px;"><b>Montant: ${if (isDebit) "-" else "+"}${tx.amount} ${tx.currency}</b></p>
                    <p><b>Statut:</b> ${tx.status?.uppercase() ?: "COMPLETED"}</p>
                    <p><b>Frais:</b> ${tx.fee ?: "0.00"} ${tx.currency}</p>
                    <hr/>
                    <p>Merci d'utiliser CashPay & OnChat.</p>
                </body>
                </html>
            """.trimIndent()
            
            webView.webViewClient = object : android.webkit.WebViewClient() {
                override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                    val printAdapter = webView.createPrintDocumentAdapter(jobName)
                    printManager.print(jobName, printAdapter, android.print.PrintAttributes.Builder().build())
                }
            }
            webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
        }
        "share" -> {
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Reçu CashPay ${tx.reference}")
                putExtra(android.content.Intent.EXTRA_TEXT, receiptText)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Partager le reçu via"))
        }
    }
}
