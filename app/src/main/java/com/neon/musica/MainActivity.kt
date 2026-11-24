package com.neon.musica

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neon.musica.ui.PlayerScreen
import com.neon.musica.ui.theme.DeepBg
import com.neon.musica.ui.theme.NeonTheme
import com.neon.musica.ui.theme.TextPrimary
import com.neon.musica.ui.theme.TextSecondary
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

private const val CRASH_FILE = "crash.txt"
private var crashLoggerInstalled = false

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installCrashLogger()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            NeonTheme {
                var crash by remember { mutableStateOf(readCrash()) }
                val text = crash
                if (text != null) {
                    CrashScreen(text = text) {
                        clearCrash()
                        crash = null
                    }
                } else {
                    AppRoot()
                }
            }
        }
    }

    /** Si la app se cierra por un error, lo guarda para mostrarlo la próxima vez. */
    private fun installCrashLogger() {
        if (crashLoggerInstalled) return
        crashLoggerInstalled = true
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                File(filesDir, CRASH_FILE).writeText(
                    "Hilo: ${thread.name}\n" +
                        "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) ${Build.MODEL}\n\n" +
                        sw.toString()
                )
            } catch (_: Throwable) {
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun readCrash(): String? {
        val file = File(filesDir, CRASH_FILE)
        return if (file.exists()) file.readText() else null
    }

    private fun clearCrash() {
        File(filesDir, CRASH_FILE).delete()
    }
}

@Composable
private fun CrashScreen(text: String, onContinue: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    Column(
        Modifier
            .fillMaxSize()
            .background(DeepBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Text(
            "La app se cerró por un error",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Toca «Copiar error» y pégalo en el chat con Claude.",
            color = TextSecondary,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { clipboard.setText(AnnotatedString(text)) }) {
                Text("Copiar error")
            }
            Button(onClick = onContinue) {
                Text("Continuar")
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = text,
            color = TextPrimary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        )
    }
}

@Composable
private fun AppRoot(vm: PlayerViewModel = viewModel()) {
    val context = LocalContext.current

    val permission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
    }

    // Permiso de notificaciones (Android 13+), necesario para ver los controles de música.
    val notificationLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(permission)
    }

    LaunchedEffect(granted) {
        if (granted) {
            vm.load()
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    PlayerScreen(
        vm = vm,
        hasPermission = granted,
        onRequestPermission = { launcher.launch(permission) }
    )
}
