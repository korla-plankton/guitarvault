package com.guitarvault.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Legal") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Privacy Policy", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(PRIVACY_POLICY, style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(32.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(32.dp))

            Text("Terms of Service", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(TERMS_OF_SERVICE, style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(32.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(16.dp))

            Text("Open Source Licenses", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(OPEN_SOURCE_LICENSES, style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "GuitarVault v1.0.0\n© 2026 korla-plankton\nAll rights reserved.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private const val PRIVACY_POLICY = """
Last updated: July 12, 2026

GuitarVault stores all data locally on your device. No personal data is collected, transmitted, or shared with third parties.

DATA WE DO NOT COLLECT:
• No name, email, or account information
• No location tracking
• No analytics or advertising
• No data sold to third parties

DATA YOU ENTER:
• Guitar specifications, photos, valuations — stored in a local JSON file on your device only
• Photos captured via camera or pasted from clipboard — stored on your device only
• No cloud sync — data exists only on the device where created

PERMISSIONS:
• Camera: Only when you photograph a guitar
• Internet: ML Kit model download; opening web browser for spec searches
• Clipboard: Only when you tap "Paste Photo"
• Foreground Service: AI background removal processing only

ON-DEVICE AI:
Background removal runs entirely on your device via ML Kit. No images are sent to any server.

DATA RETENTION:
Data is retained until you uninstall the app. Uninstalling permanently deletes all data.

THIRD-PARTY SERVICES:
• Google Play Services (ML Kit model download)
• Google Play Store (app distribution)
• Web browser (when you tap spec search buttons)

CONTACT:
github.com/korla-plankton/guitarvault
"""

private const val TERMS_OF_SERVICE = """
Last updated: July 12, 2026

1. ACCEPTANCE: By using GuitarVault, you agree to these Terms.

2. LICENSE: The App is proprietary. You may install and use it on your personal device. You may not copy, redistribute, reverse engineer, or resell the App.

3. YOUR DATA: You own all data you enter. You are responsible for backups — the App stores data locally and data may be lost if the app is uninstalled or the device is lost.

4. AI PROCESSING: Background removal is on-device and optional. Results may be inaccurate. The developer is not responsible for AI output quality.

5. VALUATIONS: The App does not provide or verify valuations. Consult a qualified appraiser for professional valuations.

6. AS-IS BASIS: The App is provided "AS IS" without warranties of any kind. The developer is not liable for data loss, inaccurate information, or any damages arising from use of the App.

7. INTELLECTUAL PROPERTY: The App, its source code, design, and content are the proprietary property of the copyright holder.

8. TERMINATION: You may stop using the App at any time by uninstalling it. All local data will be deleted.
"""

private const val OPEN_SOURCE_LICENSES = """
GuitarVault is built with the following open-source libraries:

• Jetpack Compose — Apache 2.0
• CameraX — Apache 2.0
• ML Kit Subject Segmentation — Google
• kotlinx.serialization — Apache 2.0
• Coil — Apache 2.0
• Navigation Compose — Apache 2.0
• ExifInterface — Apache 2.0
"""
