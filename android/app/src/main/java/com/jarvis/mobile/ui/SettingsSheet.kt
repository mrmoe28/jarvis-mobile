package com.jarvis.mobile.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.mobile.R
import com.jarvis.mobile.data.SavedLogin
import com.jarvis.mobile.ui.theme.JarvisBlue
import com.jarvis.mobile.ui.theme.JarvisPurple
import com.jarvis.mobile.ui.theme.Surface1
import com.jarvis.mobile.ui.theme.Surface2
import com.jarvis.mobile.ui.theme.Surface3
import com.jarvis.mobile.ui.theme.TextMuted
import com.jarvis.mobile.ui.theme.TextSecondary
import com.jarvis.mobile.viewmodel.ChatViewModel

@Composable
fun SettingsSheet(vm: ChatViewModel, onDismiss: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            "SETTINGS",
            color = TextMuted,
            fontSize = 10.sp,
            letterSpacing = 2.sp,
            fontFamily = FontFamily.Default
        )

        Spacer(Modifier.height(12.dp))

        // ── Tabs ──────────────────────────────────────────────────────────────
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor   = Surface1,
            contentColor     = JarvisBlue,
            indicator        = { positions ->
                androidx.compose.material3.TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(positions[selectedTab]),
                    color = JarvisBlue
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick  = { selectedTab = 0 },
                text     = { Text("General", fontSize = 13.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick  = { selectedTab = 1 },
                text     = { Text("Voice", fontSize = 13.sp) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick  = { selectedTab = 2 },
                text     = { Text("Logins", fontSize = 13.sp) }
            )
        }

        Spacer(Modifier.height(20.dp))

        when (selectedTab) {
            0 -> GeneralTab(vm, onDismiss)
            1 -> VoiceTab(vm)
            2 -> LoginsTab(vm)
        }
    }
}

// ── General tab ───────────────────────────────────────────────────────────────

@Composable
private fun GeneralTab(vm: ChatViewModel, onDismiss: () -> Unit) {
    val serverUrl     by vm.serverUrl.collectAsState()
    val ttsEnabled    by vm.ttsEnabled.collectAsState()
    val googleAccount by vm.googleAccount.collectAsState()
    val googleAuthUrl by vm.googleAuthUrl.collectAsState()
    var urlDraft      by remember(serverUrl) { mutableStateOf(serverUrl) }
    val context       = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
    ) {

    // ── Server URL ────────────────────────────────────────────────────────
    Text("Server URL", color = TextSecondary, fontSize = 13.sp)
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(
        value          = urlDraft,
        onValueChange  = { urlDraft = it },
        modifier       = Modifier.fillMaxWidth(),
        textStyle      = TextStyle(color = Color(0xFFE8EAED), fontSize = 13.sp, fontFamily = FontFamily.Monospace),
        placeholder    = { Text("http://192.168.x.x:3001", color = TextMuted, fontSize = 12.sp) },
        colors         = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = JarvisBlue,
            unfocusedBorderColor    = Surface3,
            cursorColor             = JarvisBlue,
            focusedContainerColor   = Surface1,
            unfocusedContainerColor = Surface1,
        ),
        shape      = RoundedCornerShape(10.dp),
        singleLine = true
    )
    Spacer(Modifier.height(6.dp))
    Text(
        "USB: http://localhost:3001 after running adb reverse tcp:3001 tcp:3001\nWiFi: use your PC's local IP address",
        color = TextMuted, fontSize = 10.sp, lineHeight = 14.sp
    )

    Spacer(Modifier.height(20.dp))

    // ── Google Account ────────────────────────────────────────────────────
    Text("Google Account", color = TextSecondary, fontSize = 13.sp)
    Spacer(Modifier.height(8.dp))
    if (googleAccount != null) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Connected", color = Color(0xFF34A853), fontSize = 13.sp)
                Text(googleAccount!!.email, color = TextMuted, fontSize = 10.sp)
            }
            OutlinedButton(
                onClick = { vm.refreshGoogleStatus() },
                border  = BorderStroke(1.dp, Surface3),
                shape   = RoundedCornerShape(8.dp)
            ) { Text("Refresh", color = TextMuted, fontSize = 12.sp) }
        }
    } else {
        val authUrl = googleAuthUrl ?: "${serverUrl.trimEnd('/')}/api/google/auth"
        Button(
            onClick  = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))) },
            modifier = Modifier.fillMaxWidth(),
            colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
            shape    = RoundedCornerShape(10.dp)
        ) { Text("Connect Google Account", fontSize = 14.sp) }
        Spacer(Modifier.height(6.dp))
        Text("Opens browser to authorize. Come back and tap Refresh after.", color = TextMuted, fontSize = 10.sp)
        Spacer(Modifier.height(6.dp))
        OutlinedButton(
            onClick  = { vm.refreshGoogleStatus() },
            modifier = Modifier.fillMaxWidth(),
            border   = BorderStroke(1.dp, Surface3),
            shape    = RoundedCornerShape(8.dp)
        ) { Text("Check connection", color = TextMuted, fontSize = 12.sp) }
    }

    Spacer(Modifier.height(24.dp))

    Button(
        onClick  = { if (urlDraft.isNotBlank()) vm.updateServerUrl(urlDraft.trim()); onDismiss() },
        modifier = Modifier.fillMaxWidth(),
        colors   = ButtonDefaults.buttonColors(containerColor = JarvisBlue),
        shape    = RoundedCornerShape(10.dp)
    ) { Text("Save", fontSize = 14.sp) }

    Spacer(Modifier.height(24.dp))

    } // end scrollable Column
}

// ── Voice tab ─────────────────────────────────────────────────────────────────

@Composable
private fun VoiceTab(vm: ChatViewModel) {
    val voices        by vm.voices.collectAsState()
    val selectedVoice by vm.selectedVoice.collectAsState()
    val ttsEnabled    by vm.ttsEnabled.collectAsState()

    // TTS toggle
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Text-to-Speech", color = TextSecondary, fontSize = 13.sp)
            Text("JARVIS speaks responses aloud", color = TextMuted, fontSize = 11.sp)
        }
        Switch(
            checked         = ttsEnabled,
            onCheckedChange = { vm.setTtsEnabled(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor   = androidx.compose.ui.graphics.Color.White,
                checkedTrackColor   = JarvisPurple,
                uncheckedThumbColor = androidx.compose.ui.graphics.Color(0xFF6B7280),
                uncheckedTrackColor = Surface3,
            )
        )
    }

    Spacer(Modifier.height(20.dp))

    if (voices.isEmpty()) {
        Text("No voices available � check that TTS is installed on your device.",
            color = TextMuted, fontSize = 11.sp, lineHeight = 16.sp
        )
    } else {
        Text("Select Voice", color = TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height((voices.size * 56).coerceAtMost(400).dp)
        ) {
            items(voices) { voice ->
                val isSelected = voice.name == selectedVoice
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(
                            if (isSelected) Surface2 else androidx.compose.ui.graphics.Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { vm.setSelectedVoice(voice.name) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (isSelected) "▶" else "  ",
                        color = JarvisBlue, fontSize = 10.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            voice.name,
                            color = if (isSelected) JarvisBlue else androidx.compose.ui.graphics.Color(0xFFE8EAED),
                            fontSize = 13.sp
                        )
                        Text("${voice.locale}  quality ${voice.quality}", color = TextMuted, fontSize = 10.sp)
                    }
                    IconButton(
                        onClick  = { vm.previewVoice(voice.name) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text("▷", color = if (isSelected) JarvisBlue else TextMuted, fontSize = 16.sp)
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(24.dp))
}

// ── Logins tab ────────────────────────────────────────────────────────────────

@Composable
private fun LoginsTab(vm: ChatViewModel) {
    val logins by vm.logins.collectAsState()
    var showForm    by remember { mutableStateOf(false) }
    var editTarget  by remember { mutableStateOf<SavedLogin?>(null) }

    // Add / Edit form
    if (showForm) {
        LoginForm(
            initial  = editTarget,
            onSave   = { login -> vm.saveLogin(login); showForm = false; editTarget = null },
            onCancel = { showForm = false; editTarget = null }
        )
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = Surface3)
        Spacer(Modifier.height(16.dp))
    }

    // Header row
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Saved Logins", color = TextSecondary, fontSize = 13.sp)
        if (!showForm) {
            Button(
                onClick = { editTarget = null; showForm = true },
                colors  = ButtonDefaults.buttonColors(containerColor = JarvisBlue),
                shape   = RoundedCornerShape(8.dp),
                modifier = Modifier.height(32.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) { Text("+ Add", fontSize = 12.sp) }
        }
    }

    Spacer(Modifier.height(10.dp))

    if (logins.isEmpty() && !showForm) {
        Text(
            "No saved logins yet. Tap '+ Add' to save credentials for a site.",
            color = TextMuted, fontSize = 11.sp, lineHeight = 16.sp
        )
    }

    logins.forEach { login ->
        LoginItem(
            login    = login,
            onEdit   = { editTarget = it; showForm = true },
            onDelete = { vm.deleteLogin(it.id) }
        )
    }

    Spacer(Modifier.height(24.dp))
}

@Composable
private fun LoginItem(login: SavedLogin, onEdit: (SavedLogin) -> Unit, onDelete: (SavedLogin) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Surface2, RoundedCornerShape(10.dp))
            .clickable { onEdit(login) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(login.siteName.ifBlank { login.siteUrl }, color = Color(0xFFE8EAED), fontSize = 13.sp)
            Text(login.siteUrl, color = TextMuted, fontSize = 10.sp)
            Text(login.username, color = TextMuted, fontSize = 10.sp)
        }
        IconButton(onClick = { onDelete(login) }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun LoginForm(
    initial: SavedLogin?,
    onSave: (SavedLogin) -> Unit,
    onCancel: () -> Unit
) {
    val base = initial ?: SavedLogin()
    var siteName  by remember(base.id) { mutableStateOf(base.siteName) }
    var siteUrl   by remember(base.id) { mutableStateOf(base.siteUrl) }
    var username  by remember(base.id) { mutableStateOf(base.username) }
    var password  by remember(base.id) { mutableStateOf(base.password) }
    var showPass  by remember { mutableStateOf(false) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor      = JarvisBlue,
        unfocusedBorderColor    = Surface3,
        cursorColor             = JarvisBlue,
        focusedContainerColor   = Surface1,
        unfocusedContainerColor = Surface1,
    )
    val fieldShape    = RoundedCornerShape(10.dp)
    val fieldTextStyle = TextStyle(color = Color(0xFFE8EAED), fontSize = 13.sp)

    Text(
        if (initial == null) "Add Login" else "Edit Login",
        color = TextSecondary, fontSize = 13.sp
    )
    Spacer(Modifier.height(10.dp))

    OutlinedTextField(
        value         = siteName,
        onValueChange = { siteName = it },
        modifier      = Modifier.fillMaxWidth(),
        label         = { Text("Site name", color = TextMuted, fontSize = 11.sp) },
        placeholder   = { Text("e.g. Solar Portal", color = TextMuted, fontSize = 12.sp) },
        textStyle     = fieldTextStyle,
        colors        = fieldColors,
        shape         = fieldShape,
        singleLine    = true
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value         = siteUrl,
        onValueChange = { siteUrl = it },
        modifier      = Modifier.fillMaxWidth(),
        label         = { Text("URL / domain", color = TextMuted, fontSize = 11.sp) },
        placeholder   = { Text("e.g. solar.lock28.com", color = TextMuted, fontSize = 12.sp) },
        textStyle     = fieldTextStyle,
        colors        = fieldColors,
        shape         = fieldShape,
        singleLine    = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value         = username,
        onValueChange = { username = it },
        modifier      = Modifier.fillMaxWidth(),
        label         = { Text("Username / email", color = TextMuted, fontSize = 11.sp) },
        textStyle     = fieldTextStyle,
        colors        = fieldColors,
        shape         = fieldShape,
        singleLine    = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value                  = password,
        onValueChange          = { password = it },
        modifier               = Modifier.fillMaxWidth(),
        label                  = { Text("Password", color = TextMuted, fontSize = 11.sp) },
        textStyle              = fieldTextStyle,
        colors                 = fieldColors,
        shape                  = fieldShape,
        singleLine             = true,
        visualTransformation   = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions        = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon           = {
            IconButton(onClick = { showPass = !showPass }) {
                Icon(
                    if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (showPass) "Hide" else "Show",
                    tint = TextMuted
                )
            }
        }
    )
    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick  = onCancel,
            modifier = Modifier.weight(1f),
            border   = BorderStroke(1.dp, Surface3),
            shape    = RoundedCornerShape(10.dp)
        ) { Text("Cancel", color = TextMuted, fontSize = 13.sp) }
        Button(
            onClick  = {
                if (siteUrl.isNotBlank() && username.isNotBlank()) {
                    onSave(base.copy(siteName = siteName, siteUrl = siteUrl, username = username, password = password))
                }
            },
            modifier = Modifier.weight(1f),
            colors   = ButtonDefaults.buttonColors(containerColor = JarvisBlue),
            shape    = RoundedCornerShape(10.dp)
        ) { Text("Save", fontSize = 13.sp) }
    }
}
