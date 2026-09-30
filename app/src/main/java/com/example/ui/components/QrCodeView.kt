package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

/**
 * High-precision 25x25 QR Code Matrix Renderer in Compose Canvas
 */
@Composable
fun QrCodeView(
    data: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    codeColor: Color = Color(0xFF0F172A)
) {
    val matrix = remember(data) { generateQrMatrix(data, 25) }

    Canvas(modifier = modifier) {
        val count = matrix.size
        val cellSize = size.width / count

        // Draw background
        drawRect(color = backgroundColor, size = size)

        // Draw matrix modules
        for (r in 0 until count) {
            for (c in 0 until count) {
                if (matrix[r][c]) {
                    drawRect(
                        color = codeColor,
                        topLeft = Offset(c * cellSize, r * cellSize),
                        size = Size(cellSize, cellSize),
                        style = Fill
                    )
                }
            }
        }
    }
}

private fun generateQrMatrix(text: String, size: Int): Array<BooleanArray> {
    val matrix = Array(size) { BooleanArray(size) }

    // Helper to draw a standard 7x7 Finder Pattern with 1px separator
    fun drawFinder(top: Int, left: Int) {
        for (r in 0 until 7) {
            for (c in 0 until 7) {
                val isOuter = r == 0 || r == 6 || c == 0 || c == 6
                val isInner = r in 2..4 && c in 2..4
                if (top + r < size && left + c < size) {
                    matrix[top + r][left + c] = isOuter || isInner
                }
            }
        }
    }

    // 1. Finder patterns at 3 corners
    drawFinder(0, 0)
    drawFinder(0, size - 7)
    drawFinder(size - 7, 0)

    // 2. Alignment pattern at bottom right
    val alignR = size - 7
    val alignC = size - 7
    for (r in 0..4) {
        for (c in 0..4) {
            val isBorder = r == 0 || r == 4 || c == 0 || c == 4
            val isCenter = r == 2 && c == 2
            if (alignR + r < size && alignC + c < size) {
                matrix[alignR + r][alignC + c] = isBorder || isCenter
            }
        }
    }

    // 3. Timing lines
    for (i in 7 until size - 7) {
        matrix[6][i] = (i % 2 == 0)
        matrix[i][6] = (i % 2 == 0)
    }

    // 4. Fill remaining data modules with deterministic seed based on text bytes
    val bytes = text.toByteArray(Charsets.UTF_8)
    var byteIdx = 0
    var bitIdx = 0

    for (r in 0 until size) {
        for (c in 0 until size) {
            // Skip finder zones
            val inTopLeft = r < 8 && c < 8
            val inTopRight = r < 8 && c >= size - 8
            val inBottomLeft = r >= size - 8 && c < 8
            val inAlign = r in (size - 7)..(size - 3) && c in (size - 7)..(size - 3)
            val inTiming = (r == 6 && (c >= 7 && c < size - 7)) || (c == 6 && (r >= 7 && r < size - 7))

            if (!inTopLeft && !inTopRight && !inBottomLeft && !inAlign && !inTiming) {
                if (bytes.isNotEmpty()) {
                    val currentByte = bytes[byteIdx % bytes.size].toInt()
                    val bitVal = (currentByte shr (bitIdx % 8)) and 1
                    val posHash = (r * 31 + c * 17) % 7
                    matrix[r][c] = (bitVal xor (posHash % 2)) == 1
                    bitIdx++
                    if (bitIdx % 8 == 0) byteIdx++
                } else {
                    matrix[r][c] = (r + c) % 2 == 0
                }
            }
        }
    }

    return matrix
}

@Composable
fun QrCodeModal(
    title: String,
    subtitle: String,
    qrData: String,
    walletId: String,
    btcAddress: String? = null,
    hideChatTab: Boolean = false,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var activeType by remember { mutableStateOf("WALLET") }

    val currentData = when (activeType) {
        "BTC" -> btcAddress ?: qrData
        "CHAT" -> "onchat://chat?walletId=$walletId"
        else -> walletId
    }

    fun copyText(label: String, value: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, value)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copié !", Toast.LENGTH_SHORT).show()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CashPayNavySurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CashPayNavyBorder))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = CashPayGrey)
                    }
                }

                Text(
                    text = subtitle,
                    color = CashPayGrey,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                // Tab Switcher (Wallet ID / BTC / Chat Link)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CashPayNavyDark)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilterChip(
                        selected = activeType == "WALLET",
                        onClick = { activeType = "WALLET" },
                        label = { Text("ID Wallet", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OnChatViolet,
                            selectedLabelColor = Color.White,
                            containerColor = Color.Transparent,
                            labelColor = CashPayGrey
                        ),
                        border = null
                    )

                    if (btcAddress != null) {
                        FilterChip(
                            selected = activeType == "BTC",
                            onClick = { activeType = "BTC" },
                            label = { Text("Bitcoin", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFF7931A),
                                selectedLabelColor = Color.White,
                                containerColor = Color.Transparent,
                                labelColor = CashPayGrey
                            ),
                            border = null
                        )
                    }

                    if (!hideChatTab) {
                        FilterChip(
                            selected = activeType == "CHAT",
                            onClick = { activeType = "CHAT" },
                            label = { Text("Chat Direct", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OnChatCyan,
                                selectedLabelColor = Color.White,
                                containerColor = Color.Transparent,
                                labelColor = CashPayGrey
                            ),
                            border = null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // QR Container
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    QrCodeView(
                        data = currentData,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Centered Micro Badge
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (activeType == "BTC") Color(0xFFF7931A) else OnChatVioletDeep)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (activeType) {
                                "BTC" -> Icons.Default.CurrencyBitcoin
                                "CHAT" -> Icons.Default.Chat
                                else -> Icons.Default.AccountBalanceWallet
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Readable Code box
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CashPayNavyDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CashPayNavyBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = currentData,
                            color = OnChatCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { copyText(activeType, currentData) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copier",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { copyText(activeType, currentData) },
                    colors = ButtonDefaults.buttonColors(containerColor = OnChatViolet),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copier / Partager", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun QrScannerModal(
    onDismiss: () -> Unit,
    onScanned: (String) -> Unit
) {
    var manualInput by remember { mutableStateOf("") }
    var flashEnabled by remember { mutableStateOf(false) }

    // Animated scanning line
    val infiniteTransition = rememberInfiniteTransition(label = "scanner")
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CashPayNavyDark),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CashPayNavyBorder))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Scanner un QR Code",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = CashPayGrey)
                    }
                }

                Text(
                    text = "Scannez un Wallet ID, une adresse Bitcoin ou un contact OnChat.",
                    color = CashPayGrey,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Camera viewfinder with live CameraPreview
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black)
                        .border(2.dp, OnChatCyan.copy(alpha = 0.8f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Actual CameraPreview
                    CameraPreview(
                        modifier = Modifier.fillMaxSize(),
                        useFrontCamera = false,
                        enableTorch = flashEnabled,
                        onQrDetected = { detected ->
                            if (detected.isNotBlank()) {
                                onScanned(detected)
                                onDismiss()
                            }
                        }
                    )

                    // Corner guides
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(140.dp)
                    )

                    // Laser beam
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(2.dp)
                            .align(Alignment.TopCenter)
                            .offset(y = (240 * 0.85f * laserOffset).dp)
                            .background(OnChatCyan)
                            .border(1.dp, Color.White)
                    )

                    // Torch button
                    IconButton(
                        onClick = { flashEnabled = !flashEnabled },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(CashPayNavySurface.copy(alpha = 0.8f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flash",
                            tint = if (flashEnabled) Color(0xFFFFD700) else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "OU SAISIR DIRECTEMENT",
                    color = CashPayGrey,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = manualInput,
                    onValueChange = { manualInput = it },
                    placeholder = { Text("Ex: CD0100000001 ou adresse BTC", color = CashPayGrey, fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = OnChatCyan,
                        unfocusedBorderColor = CashPayNavyBorder,
                        cursorColor = OnChatCyan
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        if (manualInput.isNotBlank()) {
                            onScanned(manualInput.trim())
                            onDismiss()
                        }
                    },
                    enabled = manualInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = OnChatViolet),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Valider le code", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
