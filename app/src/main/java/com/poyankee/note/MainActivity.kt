package com.poyankee.note

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.IntentCompat
import com.poyankee.note.core.Background
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
import com.poyankee.note.ui.UI
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private lateinit var store: Store
    private lateinit var settings: Settings
    private val bg = Executors.newSingleThreadExecutor()

    private var notebooks by mutableStateOf<List<NotebookInfo>>(emptyList())
    private var folders by mutableStateOf<List<String>>(emptyList())
    private var folder by mutableStateOf<String?>(null)
    private var busy by mutableStateOf<String?>(null)
    private var editor by mutableStateOf<EditorController?>(null)
    private var showSettings by mutableStateOf(false)
    private var loading by mutableStateOf(false)

    private val openPdf = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) importPdf(uri) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.WHITE, android.graphics.Color.WHITE),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.WHITE, android.graphics.Color.WHITE),
        )
        super.onCreate(savedInstanceState)
        store = Store(applicationContext)
        settings = Settings.get(this)
        bg.execute { Exporter.init(applicationContext) }
        folders = store.folders()
        refresh()
        handleIntent(intent)

        setContent {
            NoteTheme {
                val ed = editor
                val presenting = ed?.presentation == true
                Box(Modifier.fillMaxSize().background(UI.Surface).let { if (presenting) it else it.safeDrawingPadding() }) {
                    when {
                        showSettings -> {
                            BackHandler { showSettings = false }
                            SettingsScreen(settings) { showSettings = false }
                        }
                        ed != null -> EditorScreen(ed, onClose = { closeEditor() }, onSettings = { showSettings = true })
                        loading -> Box(Modifier.fillMaxSize().background(UI.Bg), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        else -> LibraryScreen(
                            notebooks = notebooks,
                            folders = folders,
                            folder = folder,
                            busy = busy,
                            onSelectFolder = { folder = it },
                            onOpen = { open(it) },
                            onCreate = { create(it) },
                            onImportPdf = { openPdf.launch(arrayOf("application/pdf")) },
                            onRename = { id, name -> if (name.isNotBlank()) { store.rename(id, name); notebooks = notebooks.map { if (it.id == id) it.copy(name = name) else it } } },
                            onDelete = { id -> store.delete(id); notebooks = notebooks.filter { it.id != id } },
                            onMove = { id, f -> store.setFolder(id, f); notebooks = notebooks.map { if (it.id == id) it.copy(folder = f) else it } },
                            onCreateFolder = { name -> addFolder(name) },
                            onRenameFolder = { old, new -> renameFolder(old, new) },
                            onDeleteFolder = { name -> deleteFolder(name) },
                            onSettings = { showSettings = true },
                        )
                    }
                }
            }
        }
    }

    /* ---------- klasörler ---------- */

    private fun addFolder(name: String) {
        val n = name.trim()
        if (n.isEmpty() || n in folders) return
        folders = folders + n
        store.saveFolders(folders)
    }

    private fun renameFolder(old: String, new: String) {
        val n = new.trim()
        if (n.isEmpty() || n == old) return
        folders = folders.map { if (it == old) n else it }.distinct()
        store.saveFolders(folders)
        notebooks.filter { it.folder == old }.forEach { store.setFolder(it.id, n) }
        notebooks = notebooks.map { if (it.folder == old) it.copy(folder = n) else it }
        if (folder == old) folder = n
    }

    private fun deleteFolder(name: String) {
        folders = folders - name
        store.saveFolders(folders)
        notebooks.filter { it.folder == name }.forEach { store.setFolder(it.id, "") }
        notebooks = notebooks.map { if (it.folder == name) it.copy(folder = "") else it }
        if (folder == name) folder = null
    }

    /* ---------- defterler ---------- */

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
            val known = store.folders()
            runOnUiThread {
                notebooks = l
                // defterlerde geçen ama listede olmayan klasörleri ekle
                val extra = l.map { it.folder }.filter { it.isNotEmpty() && it !in known }.distinct()
                folders = known + extra
            }
        }
    }

    private fun importPdf(uri: Uri) {
        closeEditor()
        busy = "PDF açılıyor..."
        val f = folder ?: ""
        bg.execute {
            try {
                val nb = store.importPdf(applicationContext, uri, f)
                runOnUiThread { busy = null; editor = EditorController(this, store, settings, nb); refresh() }
            } catch (e: Exception) {
                runOnUiThread { busy = null; android.widget.Toast.makeText(this, e.message ?: "PDF açılamadı", android.widget.Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun create(name: String) {
        val title = name.ifBlank { "Adsız defter" }
        val t = runCatching { Template.valueOf(settings.newTemplate.value) }.getOrDefault(Template.GRID)
        val size = runCatching { PageSize.valueOf(settings.newSize.value) }.getOrDefault(PageSize.A4_PORTRAIT)
        val paper = Background.Paper(t, settings.newDark.value)
        val f = folder ?: ""
        bg.execute {
            val nb = store.create(title, paper, size, f)
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
        // kapak resmi arka planda yazılıyor; biraz sonra listeyi yenile
        refresh()
        bg.execute { Thread.sleep(800); runOnUiThread { refresh() } }
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
