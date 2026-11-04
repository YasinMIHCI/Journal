package com.yasinmihci.journal.ui.session

import android.Manifest
import android.content.pm.PackageManager
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yasinmihci.journal.JournalApplication
import com.yasinmihci.journal.R
import com.yasinmihci.journal.data.repository.HabitRepository
import com.yasinmihci.journal.data.repository.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SessionViewModel(
    private val habits: HabitRepository,
    private val sessions: SessionRepository,
    private val appScope: CoroutineScope,
) : ViewModel() {
    suspend fun habitName(id: Long): String = habits.get(id)?.name.orEmpty()

    /** Enregistre la session ; le temps est retiré du restant de la semaine. */
    fun save(habitId: Long, startMs: Long, seconds: Long) {
        appScope.launch { sessions.add(habitId, startMs, seconds) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as JournalApplication).container
                SessionViewModel(c.habitRepository, c.sessionRepository, c.appScope)
            }
        }
    }
}

/**
 * Session d'activité : caméra frontale (pour se voir) + chrono avec pause.
 * L'état du chrono survit à une rotation et à l'arrêt du process (rememberSaveable).
 */
@Composable
fun SessionScreen(
    habitId: Long,
    onDone: () -> Unit,
    viewModel: SessionViewModel = viewModel(factory = SessionViewModel.Factory),
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    LaunchedEffect(habitId) { name = viewModel.habitName(habitId) }

    // Chrono : temps cumulé avant la pause en cours + début de la période en cours (0 = en pause).
    var startedAtMs by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var accumulatedMs by rememberSaveable { mutableLongStateOf(0L) }
    var runningSinceMs by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(250)
        }
    }
    val elapsedMs = accumulatedMs + if (runningSinceMs > 0) (nowMs - runningSinceMs).coerceAtLeast(0) else 0
    var confirmQuit by remember { mutableStateOf(false) }

    KeepScreenOn()
    BackHandler { confirmQuit = true }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        FrontCamera(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(color = Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(name, color = Color.White, style = MaterialTheme.typography.titleLarge)
                    Text(formatChrono(elapsedMs), color = Color.White, style = MaterialTheme.typography.displayLarge)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
                OutlinedButton(onClick = {
                    if (runningSinceMs > 0) {
                        accumulatedMs += System.currentTimeMillis() - runningSinceMs
                        runningSinceMs = 0
                    } else {
                        runningSinceMs = System.currentTimeMillis()
                    }
                }) {
                    Text(
                        stringResource(if (runningSinceMs > 0) R.string.session_pause else R.string.session_resume),
                        color = Color.White,
                    )
                }
                Button(onClick = {
                    viewModel.save(habitId, startedAtMs, elapsedMs / 1000)
                    onDone()
                }) { Text(stringResource(R.string.session_finish)) }
            }
        }
    }

    if (confirmQuit) {
        AlertDialog(
            onDismissRequest = { confirmQuit = false },
            title = { Text(stringResource(R.string.session_quit_title)) },
            confirmButton = { TextButton(onClick = onDone) { Text(stringResource(R.string.session_quit)) } },
            dismissButton = { TextButton(onClick = { confirmQuit = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

/** 0:05:07 ou 05:07. */
internal fun formatChrono(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

@Composable
private fun KeepScreenOn() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? android.app.Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
}

/** Aperçu caméra frontale (CameraX). Demande la permission si besoin ; sinon fond noir + message. */
@Composable
private fun FrontCamera(modifier: Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }

    var failed by remember { mutableStateOf(false) }
    if (!granted || failed) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.session_no_camera), color = Color.White)
        }
        return
    }
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    DisposableEffect(lifecycleOwner) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val provider = future.get()
                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(previewView.surfaceProvider)
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview)
            } catch (e: Exception) {
                // Pas de caméra frontale, caméra occupée… : le chrono reste utilisable.
                failed = true
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            if (future.isDone) runCatching { future.get().unbindAll() }
        }
    }
    AndroidView(factory = { previewView }, modifier = modifier)
}
