package com.example.ui.screens.meetings

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.api.ApiClient
import com.example.ui.theme.CashPayNavyDark
import java.net.URI

/** Shared authenticated meeting UI: one admission/PIN and organizer flow for web and Android. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MeetingWebScreen(url: String, onClose: () -> Unit) {
    val context = LocalContext.current
    var view by remember { mutableStateOf<WebView?>(null) }
    var pending by remember { mutableStateOf<PermissionRequest?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val origin = remember { URI(ApiClient.BASE_URL) }
    fun trusted(uri: Uri) = uri.scheme == "https" && uri.host == origin.host && uri.port in listOf(-1, 443)
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val request = pending; pending = null
        if (request != null) {
            if (result.isNotEmpty() && result.values.all { it }) request.grant(request.resources) else request.deny()
        }
    }
    BackHandler { val current = view; if (current?.canGoBack() == true) current.goBack() else onClose() }
    DisposableEffect(Unit) {
        onDispose { pending?.deny(); pending = null; view?.apply { stopLoading(); loadUrl("about:blank"); destroy() }; view = null }
    }
    Column(Modifier.fillMaxSize()) {
        Surface(color = CashPayNavyDark) { Row(Modifier.fillMaxWidth().statusBarsPadding()) { TextButton(onClick = onClose) { Text("Fermer la réunion") } } }
        if (error != null) { Text(error!!); Button(onClick = { view?.reload(); error = null }) { Text("Réessayer") } }
        AndroidView(modifier = Modifier.fillMaxWidth().weight(1f), factory = {
            WebView(context).apply {
                view = this
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                settings.mediaPlaybackRequiresUserGesture = false
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(webView: WebView, request: WebResourceRequest): Boolean {
                        if (!trusted(request.url)) { error = "Ce lien sort de l’espace OnChat."; return true }
                        return false
                    }
                    override fun onReceivedError(webView: WebView, request: WebResourceRequest, failure: WebResourceError) {
                        if (request.isForMainFrame) error = "Connexion à OnChat impossible. Vérifiez votre réseau."
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onPermissionRequest(request: PermissionRequest) {
                        post {
                            val supported = setOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE, PermissionRequest.RESOURCE_VIDEO_CAPTURE)
                            if (!trusted(request.origin) || request.resources.any { it !in supported }) { request.deny(); return@post }
                            val required = request.resources.map { if (it == PermissionRequest.RESOURCE_VIDEO_CAPTURE) Manifest.permission.CAMERA else Manifest.permission.RECORD_AUDIO }
                            val missing = required.filter { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }
                            if (missing.isEmpty()) request.grant(request.resources)
                            else if (pending != null) request.deny()
                            else { pending = request; permissions.launch(missing.toTypedArray()) }
                        }
                    }
                    override fun onPermissionRequestCanceled(request: PermissionRequest) { if (pending === request) pending = null }
                }
                val token = ApiClient.authToken
                if (token.isNullOrBlank()) error = "Votre session a expiré. Reconnectez-vous."
                else CookieManager.getInstance().setCookie(ApiClient.BASE_URL, "cashpay-token=$token; Path=/; Secure; HttpOnly; SameSite=Lax") { accepted ->
                    post { if (accepted) loadUrl(url) else error = "Impossible d’ouvrir votre session OnChat." }
                }
            }
        })
    }
}
