package com.poyankee.note

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.IntentCompat
import com.poyankee.note.core.PageSize
import com.poyankee.note.core.Template
import com.poyankee.note.data.NotebookInfo
import com.poyankee.note.data.Settings
import com.poyankee.note.data.Store
import com.poyankee.note.editor.EditorController
import com.poyankee.note.editor.Exporter
import com.poyankee.note.ui.EditorScreen
import com.poyankee.note.ui.LibraryScreen
import com.poyankee.note.ui.NoteTheme
import com.poyankee.note.ui.SettingsScreen
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private lateinit var store: Store
    private lateinit var settings: Settings
    private val bg = Executors.newSingleThreadExecutor()

    private var notebooks by mutableStateOf<List<NotebookInfo>>(emptyList())
    private var busy by mutableStateOf<String?>(null)
    private var editor by mutableStateOf<EditorController?>(null)
    private var showSettings by mutableStateOf(false)
    private var loading by mutableStateOf(false)

    private val openPdf = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) importPdf(uri) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = Store(applicationContext)
        settings = Settings.get(this)
        bg.execute { Exporter.init(applicationContext) }
        refresh()
        handleIntent(intent)

        setContent {
            NoteTheme {
                val ed = editor
                when {
                    showSettings -> {
                        BackHandler { showSettings = false }
                        SettingsScreen(settings) { showSettings = false }
                    }
                    ed != null -> EditorScreen(ed, onClose = { closeEditor() }, onSettings = { showSettings = true })
                    loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    else -> LibraryScreen(
                        notebooks = notebooks,
                        busy = busy,
                        settings = settings,
                        onOpen = { open(it) },
                        onCreate = { name, t, dark, size -> create(name, t, dark, size) },
                        onImportPdf = { openPdf.launch(arrayOf("application/pdf")) },
                        onRename = { id, name -> store.rename(id, name); bg.execute { store.flush(); runOnUiThread { refresh() } } },
                        onDelete = { id -> store.delete(id); notebooks = notebooks.filter { it.id != id } },
                        onSettings = { showSettings = true },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        val uri: Uri? = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            else -> null
        }
        if (uri != null) {
            intent.action = null
            importPdf(uri)
        }
    }

    private fun refresh() {
        bg.execute {
            val l = store.list()
            runOnUiThread { notebooks = l }
        }
    }

    private fun importPdf(uri: Uri) {
        closeEditor()
        busy = "PDF açılıyor..."
        bg.execute {
            try {
                val nb = store.importPdf(applicationContext, uri)
                runOnUiThread { busy = null; editor = EditorController(this, store, settings, nb); refresh() }
            } catch (e: Exception) {
                runOnUiThread { busy = null; android.widget.Toast.makeText(this, e.message ?: "PDF açılamadı", android.widget.Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun create(name: String, t: Template, dark: Boolean, size: PageSize) {
        bg.execute {
            val nb = store.create(name, t, dark, size)
            runOnUiThread { editor = EditorController(this, store, settings, nb); refresh() }
        }
    }

    private fun open(id: String) {
        loading = true
        bg.execute {
            try {
                val nb = store.load(id)
                runOnUiThread { loading = false; editor = EditorController(this, store, settings, nb) }
            } catch (e: Exception) {
                runOnUiThread {
                    loading = false
                    android.widget.Toast.makeText(this, "Defter açılamadı: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun closeEditor() {
        val ed = editor ?: return
        editor = null
        ed.close()
        refresh()
    }

    override fun onPause() {
        super.onPause()
        editor?.let { store.saveMeta(it.nb) }
        store.flush()
    }

    override fun onDestroy() {
        if (isFinishing) editor?.close()
        super.onDestroy()
    }
}
