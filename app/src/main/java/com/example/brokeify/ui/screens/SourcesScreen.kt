package com.example.brokeify.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.brokeify.model.ProviderStatus
import com.example.brokeify.viewmodel.BrokeifyViewModel
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class SixDigitCodeTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text.filter { it.isDigit() }.take(6)
        val out = StringBuilder()
        for (i in raw.indices) {
            out.append(raw[i])
            if (i == 2 && raw.length > 3) {
                out.append(" ")
            }
        }
        val transformed = out.toString()

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 3) return offset
                if (offset <= 6) return (offset + 1).coerceAtMost(transformed.length)
                return transformed.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 3) return offset
                if (offset <= 4) return 3
                if (offset <= 7) return (offset - 1).coerceAtMost(raw.length)
                return raw.length
            }
        }

        return TransformedText(AnnotatedString(transformed), offsetMapping)
    }
}

@Composable
fun SourcesScreen(
    viewModel: BrokeifyViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val config = uiState.gatewayConfig
    val isConnected = config.status == ProviderStatus.CONNECTED

    var serverUrlInput by remember(config.serverUrl) {
        mutableStateOf(
            if (config.serverUrl.isNotBlank()) config.serverUrl else "https://music.flockingreeds.farm"
        )
    }
    var sixDigitCodeInput by remember { mutableStateOf("") }
    val sixDigitFocusRequester = remember { FocusRequester() }
    var usernameInput by remember(config.username) { mutableStateOf(config.username) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showManualLogin by remember { mutableStateOf(false) }

    var testHealthResultText by remember { mutableStateOf<String?>(null) }
    var isTestingHealth by remember { mutableStateOf(false) }

    // ZXing Embedded fallback scanner launcher (works on all Android phones & emulators)
    val zxingScannerLauncher = rememberLauncherForActivityResult(
        contract = ScanContract()
    ) { result ->
        val contents = result.contents
        if (!contents.isNullOrBlank()) {
            viewModel.connectWithQrPayload(contents, serverUrlInput)
        }
    }

    fun launchZxingScanner() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Point camera at the Brokeify QR Code on your server")
            setBeepEnabled(false)
            setOrientationLocked(false)
        }
        zxingScannerLauncher.launch(options)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchZxingScanner()
        }
    }

    fun startQrCodeScanner() {
        try {
            val options = GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .enableAutoZoom()
                .build()
            val scanner = GmsBarcodeScanning.getClient(context, options)
            scanner.startScan()
                .addOnSuccessListener { barcode ->
                    val rawValue = barcode.rawValue
                    if (!rawValue.isNullOrBlank()) {
                        viewModel.connectWithQrPayload(rawValue, serverUrlInput)
                    }
                }
                .addOnFailureListener {
                    // Fallback to standard camera scanner if Google Play Services scanner is unavailable
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        launchZxingScanner()
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
        } catch (_: Exception) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                launchZxingScanner()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070A0F))
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("sources_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (uiState.isReRunningSetup || isConnected) {
            OutlinedButton(
                onClick = { viewModel.closeSetupWizard() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00FF66)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Return to Music Library",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        // Header matching Brokeify Docker Web UI ("Connect Android App")
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode2,
                    contentDescription = null,
                    tint = Color(0xFF00FF66),
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "CONNECT ANDROID APP",
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = Color(0xFFA855F7).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "6-DIGIT QUICK CONNECT",
                        color = Color(0xFFD8B4FE),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Surface(
                    color = Color(0xFF00FF66).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "CAMERA QR PAIRING",
                        color = Color(0xFF00FF66),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = "Link your phone using the 6-digit Quick Connect code or scan the QR code from your Brokeify Docker server.",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp
            )
        }

        // Active Connection Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    if (isConnected) Color(0xFF00FF66).copy(alpha = 0.5f) else Color(0xFF1E293B),
                    RoundedCornerShape(14.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D131C)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    when (config.status) {
                                        ProviderStatus.CONNECTED -> Color(0xFF00FF66)
                                        ProviderStatus.TESTING -> Color(0xFFF59E0B)
                                        ProviderStatus.ERROR -> Color(0xFFEF4444)
                                        ProviderStatus.DISCONNECTED -> Color(0xFF64748B)
                                    },
                                    shape = CircleShape
                                )
                        )
                        Column {
                            Text(
                                text = if (isConnected) config.name else "Brokeify Docker Server",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            if (config.serverUrl.isNotBlank()) {
                                Text(
                                    text = config.serverUrl,
                                    color = Color(0xFF00FF66),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Surface(
                        color = when (config.status) {
                            ProviderStatus.CONNECTED -> Color(0xFF00FF66).copy(alpha = 0.15f)
                            ProviderStatus.TESTING -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                            ProviderStatus.ERROR -> Color(0xFFEF4444).copy(alpha = 0.15f)
                            ProviderStatus.DISCONNECTED -> Color(0xFF1E293B)
                        },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = when (config.status) {
                                ProviderStatus.CONNECTED -> "CONNECTED"
                                ProviderStatus.TESTING -> "LINKING"
                                ProviderStatus.ERROR -> "ERROR"
                                ProviderStatus.DISCONNECTED -> "NOT LINKED"
                            },
                            color = when (config.status) {
                                ProviderStatus.CONNECTED -> Color(0xFF00FF66)
                                ProviderStatus.TESTING -> Color(0xFFF59E0B)
                                ProviderStatus.ERROR -> Color(0xFFEF4444)
                                ProviderStatus.DISCONNECTED -> Color(0xFF94A3B8)
                            },
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                if (config.lastMessage.isNotBlank()) {
                    Text(
                        text = config.lastMessage,
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (isConnected) {
                    HorizontalDivider(color = Color(0xFF1E293B))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "TRACKS", color = Color(0xFF64748B), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text(text = "${uiState.tracks.size}", color = Color(0xFF00FF66), fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Column {
                            Text(text = "ALBUMS", color = Color(0xFF64748B), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text(text = "${config.albumCount}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Column {
                            Text(text = "PLAYLISTS", color = Color(0xFF64748B), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text(text = "${uiState.playlists.size}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Column {
                            Text(text = "LIKED", color = Color(0xFF64748B), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text(text = "${uiState.likedTrackIds.size}", color = Color(0xFFEC4899), fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        if (config.latencyMs > 0) {
                            Column {
                                Text(text = "PING", color = Color(0xFF64748B), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                Text(text = "${config.latencyMs}ms", color = Color(0xFF38BDF8), fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.navigateTo("library") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = Color(0xFF070A0F), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Library", color = Color(0xFF070A0F), fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }

                        Button(
                            onClick = { viewModel.refreshLibrary() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            if (uiState.isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color(0xFF00FF66), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh Library", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }

                        Button(
                            onClick = { viewModel.disconnectGateway() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Unlink", color = Color(0xFFEF4444), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // Feedback Banner
        if (!uiState.pairingFeedbackMessage.isNullOrBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (uiState.isPairingError) Color(0xFFEF4444) else Color(0xFF00FF66),
                        RoundedCornerShape(12.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isPairingError) Color(0xFFEF4444).copy(alpha = 0.12f) else Color(0xFF00FF66).copy(alpha = 0.12f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = uiState.pairingFeedbackMessage!!,
                    color = if (uiState.isPairingError) Color(0xFFFCA5A5) else Color(0xFF00FF66),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        // Primary Card: 6-DIGIT QUICK CONNECT & CAMERA QR SCAN
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFA855F7).copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0E0B16)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "# 6-DIGIT QUICK CONNECT",
                        color = Color(0xFFD8B4FE),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Jellyfin-Style Pairing",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Server URL Input
                OutlinedTextField(
                    value = serverUrlInput,
                    onValueChange = { serverUrlInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gateway_url_input")
                        .testTag("jellyfin_url_input"),
                    label = { Text("Brokeify Server URL") },
                    placeholder = { Text("https://music.flockingreeds.farm or http://192.168.1.175:3487") },
                    leadingIcon = {
                        Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFF00FF66))
                    },
                    trailingIcon = {
                        if (serverUrlInput.isNotEmpty()) {
                            IconButton(onClick = { serverUrlInput = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear URL", tint = Color(0xFF94A3B8))
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00FF66),
                        unfocusedBorderColor = Color(0xFF2E1065),
                        focusedContainerColor = Color(0xFF070A0F),
                        unfocusedContainerColor = Color(0xFF070A0F),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color(0xFF00FF66),
                        unfocusedLabelColor = Color(0xFF94A3B8)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )

                // Quick URL Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { serverUrlInput = "https://music.flockingreeds.farm" },
                        label = {
                            Text(
                                "music.flockingreeds.farm",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFF00FF66)
                        ),
                        border = null
                    )
                    AssistChip(
                        onClick = { serverUrlInput = "http://192.168.1.175:3487" },
                        label = {
                            Text(
                                "LAN :3487",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFF94A3B8)
                        ),
                        border = null
                    )
                }

                // High-Contrast 6-Digit PIN Interactive Boxes (Clean 1-place input without confusing separate bar)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "ENTER 6-DIGIT CODE FROM BROKEIFY WEB UI",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )

                    BasicTextField(
                        value = sixDigitCodeInput,
                        onValueChange = { raw ->
                            val digits = raw.filter { it.isDigit() }.take(6)
                            sixDigitCodeInput = digits
                            if (digits.length == 6 && serverUrlInput.isNotBlank() && !uiState.isSyncing) {
                                viewModel.connectWithSixDigitCode(serverUrlInput, digits)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(sixDigitFocusRequester)
                            .testTag("quick_connect_code_input"),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (serverUrlInput.isNotBlank() && sixDigitCodeInput.length == 6 && !uiState.isSyncing) {
                                    viewModel.connectWithSixDigitCode(serverUrlInput, sixDigitCodeInput)
                                }
                            }
                        ),
                        decorationBox = {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { sixDigitFocusRequester.requestFocus() },
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (i in 0 until 6) {
                                    if (i == 3) {
                                        Spacer(modifier = Modifier.width(14.dp))
                                    }
                                    val char = sixDigitCodeInput.getOrNull(i)?.toString() ?: ""
                                    val isCurrentSlot = i == sixDigitCodeInput.length
                                    val isFilled = char.isNotEmpty()

                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 3.dp)
                                            .size(width = 46.dp, height = 56.dp)
                                            .background(
                                                color = Color(0xFF05070B),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .border(
                                                width = if (isCurrentSlot) 2.dp else 1.dp,
                                                color = when {
                                                    isCurrentSlot -> Color(0xFFA855F7)
                                                    isFilled -> Color(0xFF00FF66)
                                                    else -> Color(0xFF1E293B)
                                                },
                                                shape = RoundedCornerShape(10.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isFilled) char else "—",
                                            color = if (isFilled) Color(0xFF00FF66) else Color(0xFF475569),
                                            fontSize = if (isFilled) 24.sp else 16.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (sixDigitCodeInput.length == 6) "✓ 6 digits entered" else "Tap boxes to type code",
                            color = if (sixDigitCodeInput.length == 6) Color(0xFF00FF66) else Color(0xFF64748B),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        if (sixDigitCodeInput.isNotEmpty()) {
                            TextButton(
                                onClick = { sixDigitCodeInput = "" },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear", color = Color(0xFF94A3B8), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }

                // Link with 6-Digit Code Button
                Button(
                    onClick = {
                        viewModel.connectWithSixDigitCode(serverUrlInput, sixDigitCodeInput)
                    },
                    enabled = serverUrlInput.isNotBlank() &&
                        sixDigitCodeInput.length == 6 &&
                        !uiState.isSyncing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF9333EA),
                        disabledContainerColor = Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("quick_connect_submit_btn")
                ) {
                    if (uiState.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.Link, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (uiState.isSyncing) "Linking with Server..." else "Connect with 6-Digit Code",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                }

                HorizontalDivider(color = Color(0xFF1E293B))

                // Method 2: Camera QR Scan & Paste QR Payload Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { startQrCodeScanner() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("scan_qr_code_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan QR Code",
                            tint = Color(0xFF070A0F),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Scan QR Code",
                            color = Color(0xFF070A0F),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val clipText = clipboardManager.getText()?.text?.trim().orEmpty()
                            if (clipText.isNotBlank()) {
                                val digits = clipText.filter { it.isDigit() }
                                if (digits.length == 6 && clipText.length <= 10) {
                                    sixDigitCodeInput = digits
                                    viewModel.connectWithSixDigitCode(serverUrlInput, digits)
                                } else {
                                    viewModel.connectWithQrPayload(clipText, serverUrlInput)
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00FF66)),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = SolidColor(Color(0xFF00FF66).copy(alpha = 0.5f))
                        ),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("paste_qr_payload_btn")
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste Code or QR Payload", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Paste", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Reverse Quick Connect Card: Show a 6-Digit Code on This Phone to Authorize in Web UI
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D131C)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "OR SHOW 6-DIGIT CODE ON THIS PHONE",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Generate a 6-digit code on this phone and enter it in your Brokeify Web UI under 'Authorize 6-Digit Code Shown On Your Phone'.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )

                if (!uiState.generatedPhonePin.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF05070B), shape = RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF00FF66), RoundedCornerShape(12.dp))
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.generatedPhonePin!!,
                                color = Color(0xFF00FF66),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 30.sp,
                                letterSpacing = 4.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Waiting for Web UI authorization...",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.cancelPhonePinPolling() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel Phone Code", color = Color(0xFF94A3B8), fontFamily = FontFamily.Monospace)
                    }
                } else {
                    OutlinedButton(
                        onClick = { viewModel.generatePhoneSixDigitCode(serverUrlInput) },
                        enabled = serverUrlInput.isNotBlank() && !uiState.isPollingPhonePin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("generate_phone_pin_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD8B4FE)),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = SolidColor(Color(0xFFA855F7).copy(alpha = 0.5f))
                        )
                    ) {
                        Icon(Icons.Default.Smartphone, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate 6-Digit Code on Phone", fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Diagnostics & Optional Password/Token Login Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D131C)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quick Test Connection Button checking /api/health
                OutlinedButton(
                    onClick = {
                        isTestingHealth = true
                        testHealthResultText = null
                        viewModel.testGatewayHealth(serverUrlInput) { result ->
                            isTestingHealth = false
                            testHealthResultText = if (result.success) {
                                "✓ Verified Reachable: ${result.serverName} (${result.totalTracks} tracks ready, ${result.latencyMs}ms)"
                            } else {
                                "✗ Failed: ${result.errorMessage ?: "Cannot reach server"}"
                            }
                        }
                    },
                    enabled = serverUrlInput.isNotBlank() && !isTestingHealth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("test_connection_btn"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF38BDF8)
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = SolidColor(Color(0xFF38BDF8).copy(alpha = 0.5f))
                    )
                ) {
                    if (isTestingHealth) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF38BDF8), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Checking /api/health...", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    } else {
                        Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF38BDF8))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test Server URL (/api/health)", fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (testHealthResultText != null) {
                    Text(
                        text = testHealthResultText!!,
                        color = if (testHealthResultText!!.startsWith("✓")) Color(0xFF00FF66) else Color(0xFFEF4444),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showManualLogin = !showManualLogin }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FRIENDS & FAMILY / PASSWORD LOGIN",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = if (showManualLogin) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle Manual Login",
                        tint = Color(0xFF94A3B8)
                    )
                }

                if (showManualLogin) {
                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gateway_username_input")
                            .testTag("jellyfin_username_input"),
                        label = { Text("Username") },
                        placeholder = { Text("admin or Friends & Family username") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00FF66),
                            unfocusedBorderColor = Color(0xFF1E293B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color(0xFF00FF66),
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gateway_password_input")
                            .testTag("jellyfin_password_input"),
                        label = { Text("Password or API Token") },
                        placeholder = { Text("Password or brk_... API token") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = Color(0xFF94A3B8)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00FF66),
                            unfocusedBorderColor = Color(0xFF1E293B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color(0xFF00FF66),
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    Button(
                        onClick = {
                            viewModel.connectGateway(serverUrlInput, usernameInput, passwordInput)
                        },
                        enabled = serverUrlInput.isNotBlank() && !uiState.isSyncing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00FF66),
                            disabledContainerColor = Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("gateway_connect_btn")
                            .testTag("jellyfin_connect_btn")
                    ) {
                        Text(
                            text = "Sign In & Sync Library",
                            color = Color(0xFF070A0F),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
