package com.poyankee.note.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.view.KeyEvent
import java.security.MessageDigest

/** Kalem tuşları: standart Android kalem tuş kodları + kullanıcının "öğrettiği" tuşlar. */
object PenKeys {
    /** "Tuşu öğret" açıkken bir sonraki tuş buraya iletilir. */
    @Volatile var learner: ((Int) -> Unit)? = null

    private val STANDARD = setOf(
        KeyEvent.KEYCODE_STYLUS_BUTTON_PRIMARY, KeyEvent.KEYCODE_STYLUS_BUTTON_SECONDARY,
        KeyEvent.KEYCODE_STYLUS_BUTTON_TERTIARY, KeyEvent.KEYCODE_STYLUS_BUTTON_TAIL,
    )

    /** Bu tuş kalem tuşu olarak işlenmeli mi? Geri, ses ve güç tuşlarına dokunulmaz (öğretilmedikçe). */
    fun isPenKey(code: Int, learned: Set<Int>): Boolean = code in learned || code in STANDARD

    fun canLearn(code: Int) = code != KeyEvent.KEYCODE_BACK && code != KeyEvent.KEYCODE_POWER && code != KeyEvent.KEYCODE_HOME
}

/** Defter kilidi: PIN düz metin olarak saklanmaz, defter kimliğiyle birlikte özeti alınır. */
object Lock {
    fun hash(id: String, pin: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest("note:$id:$pin".toByteArray()).joinToString("") { "%02x".format(it) }
    }
    fun check(id: String, pin: String, hash: String) = hash.isNotEmpty() && hash(id, pin) == hash
}

/** Seçilen klasöre (ör. Google Drive veya bir USB bellek) otomatik yedek. */
object AutoBackup {
    private const val DAY = 24L * 60 * 60 * 1000
    private const val KEEP = 5

    /** Gerekiyorsa yedek alır. Arka planda çağrılmalı. Yedek alındıysa true. */
    fun runIfDue(context: Context, store: Store, settings: Settings, force: Boolean = false): Boolean {
        if (!settings.autoBackup.value && !force) return false
        val tree = settings.backupTree.value.takeIf { it.isNotEmpty() }?.let { Uri.parse(it) } ?: return false
        if (!force && System.currentTimeMillis() - settings.lastBackup.value < DAY) return false
        val cr = context.contentResolver
        val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val name = "Note-otomatik-" + java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm", java.util.Locale.US).format(java.util.Date()) + ".zip"
        val doc = DocumentsContract.createDocument(cr, parent, "application/zip", name) ?: return false
        cr.openOutputStream(doc)?.use { store.backupTo(it) } ?: return false
        settings.lastBackup.value = System.currentTimeMillis()
        prune(context, tree)
        return true
    }

    /** En yeni [KEEP] otomatik yedek kalır. */
    private fun prune(context: Context, tree: Uri) {
        try {
            val cr = context.contentResolver
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
            val found = ArrayList<Pair<String, String>>() // (ad, belge kimliği)
            cr.query(children, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_DOCUMENT_ID), null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    val n = c.getString(0) ?: continue
                    if (n.startsWith("Note-otomatik-") && n.endsWith(".zip")) found += n to c.getString(1)
                }
            }
            found.sortedByDescending { it.first }.drop(KEEP).forEach { (_, id) ->
                try { DocumentsContract.deleteDocument(cr, DocumentsContract.buildDocumentUriUsingTree(tree, id)) } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }
}
