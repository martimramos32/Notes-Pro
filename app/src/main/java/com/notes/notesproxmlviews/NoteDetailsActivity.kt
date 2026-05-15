package com.notes.notesproxmlviews

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.material.button.MaterialButton
import com.google.firebase.Timestamp.Companion.now
import com.google.firebase.firestore.DocumentReference
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class NoteDetailsActivity : AppCompatActivity() {
    var titleEditText: EditText? = null
    var contentEditText: EditText? = null
    var saveNoteBtn: ImageButton? = null
    var pageTitleTextView: TextView? = null
    var title: String? = null
    var content: String? = null
    var docId: String? = null
    var isEditMode: Boolean = false
    var deleteNoteTextViewBtn: TextView? = null

    // Campos de imagem
    var noteImagePreview: ImageView? = null
    var addImageBtn: MaterialButton? = null
    var removeImageBtn: MaterialButton? = null
    var currentImageBase64: String? = null
    private var cameraImageUri: Uri? = null

    // Campos de pin
    var pinNoteBtn: ImageButton? = null
    var isPinned: Boolean = false

    // Campos de lembrete
    var setReminderBtn: MaterialButton? = null
    var reminderDateTextView: TextView? = null
    var cancelReminderBtn: ImageButton? = null
    var reminderTimestampMs: Long = 0L // 0 = sem lembrete

    // Launchers
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var galleryPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_details)

        // Views básicas
        titleEditText = findViewById(R.id.notes_title_text)
        contentEditText = findViewById(R.id.notes_content_text)
        saveNoteBtn = findViewById(R.id.save_note_btn)
        pageTitleTextView = findViewById(R.id.page_title)
        deleteNoteTextViewBtn = findViewById(R.id.delete_note_text_view_btn)

        // Views de imagem
        noteImagePreview = findViewById(R.id.note_image_preview)
        addImageBtn = findViewById(R.id.add_image_btn)
        removeImageBtn = findViewById(R.id.remove_image_btn)

        // Views de pin
        pinNoteBtn = findViewById(R.id.pin_note_btn)

        // Views de lembrete
        setReminderBtn = findViewById(R.id.set_reminder_btn)
        reminderDateTextView = findViewById(R.id.reminder_date_text_view)
        cancelReminderBtn = findViewById(R.id.cancel_reminder_btn)

        registerLaunchers()

        // Receber dados do Intent
        title = intent.getStringExtra("title")
        content = intent.getStringExtra("content")
        docId = intent.getStringExtra("docId")
        val imageBase64 = intent.getStringExtra("imageBase64")
        isPinned = intent.getIntExtra("pinOrder", 0) == 1
        reminderTimestampMs = intent.getLongExtra("reminderTimestamp", 0L)

        if (docId != null && !docId!!.isEmpty()) {
            isEditMode = true
        }

        if (isEditMode) {
            pageTitleTextView!!.text = getString(R.string.edit_your_note)
            deleteNoteTextViewBtn!!.visibility = View.VISIBLE
            
            // Se o título estiver vazio, significa que fomos abertos pela Notificação (apenas docId)
            if (title.isNullOrEmpty()) {
                loadNoteFromFirebase()
            } else {
                // Fomos abertos pelo RecyclerView (Adapter passou todos os dados)
                titleEditText!!.setText(title)
                contentEditText!!.setText(content)

                // Restaurar imagem
                if (!imageBase64.isNullOrEmpty()) {
                    currentImageBase64 = imageBase64
                    showImagePreview(imageBase64)
                }

                // Restaurar estado do pin
                updatePinButton()

                // Restaurar lembrete
                if (reminderTimestampMs > 0L && reminderTimestampMs > System.currentTimeMillis()) {
                    showReminderText(reminderTimestampMs)
                } else if (reminderTimestampMs > 0L) {
                    reminderTimestampMs = 0L
                }
            }
        }

        // Listeners
        saveNoteBtn!!.setOnClickListener { saveNote() }
        deleteNoteTextViewBtn!!.setOnClickListener { deleteNoteFromFirebase() }
        addImageBtn!!.setOnClickListener { showImageSourceDialog() }
        removeImageBtn!!.setOnClickListener { removeImage() }
        pinNoteBtn!!.setOnClickListener { togglePin() }
        setReminderBtn!!.setOnClickListener { showReminderPicker() }
        cancelReminderBtn!!.setOnClickListener { clearReminder() }
    }

    // ─── CARREGAR DO FIRESTORE (VIA NOTIFICAÇÃO) ──────────────────────────────
    
    private fun loadNoteFromFirebase() {
        val documentReference = Utility.getCollectionReferenceForNotes().document(docId.toString())
        documentReference.get().addOnSuccessListener { documentSnapshot ->
            val note = documentSnapshot.toObject(Note::class.java)
            if (note != null) {
                // Preencher campos
                titleEditText!!.setText(note.title)
                contentEditText!!.setText(note.content)
                
                // Restaurar imagem
                if (!note.imageBase64.isNullOrEmpty()) {
                    currentImageBase64 = note.imageBase64
                    showImagePreview(note.imageBase64!!)
                }
                
                // Restaurar pin
                isPinned = note.pinOrder == 1
                updatePinButton()
                
                // Restaurar lembrete (só se estiver no futuro, caso contrário limpa)
                reminderTimestampMs = note.reminderTimestamp
                if (reminderTimestampMs > 0L && reminderTimestampMs > System.currentTimeMillis()) {
                    showReminderText(reminderTimestampMs)
                } else if (reminderTimestampMs > 0L) {
                    reminderTimestampMs = 0L
                }
            } else {
                Utility.showToast(this, "Failed to load note data")
                finish()
            }
        }.addOnFailureListener {
            Utility.showToast(this, "Failed to connect to server")
            finish()
        }
    }

    // ─── PIN ──────────────────────────────────────────────────────────────────

    /**
     * Alterna o estado de pin e atualiza o ícone.
     */
    private fun togglePin() {
        isPinned = !isPinned
        updatePinButton()
        Utility.showToast(
            this,
            if (isPinned) "Note pinned to top 📌" else "Note unpinned"
        )
    }

    /**
     * Atualiza o ícone e a tint do botão de pin conforme o estado atual.
     */
    private fun updatePinButton() {
        if (isPinned) {
            pinNoteBtn!!.setImageResource(R.drawable.ic_push_pin_filled)
        } else {
            pinNoteBtn!!.setImageResource(R.drawable.ic_push_pin_outline)
        }
    }

    // ─── LEMBRETE ─────────────────────────────────────────────────────────────

    /**
     * Mostra o DatePickerDialog seguido do TimePickerDialog.
     * Se Android 13+, pede permissão de notificação primeiro.
     */
    private fun showReminderPicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        launchDatePicker()
    }

    private fun launchDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                launchTimePicker(year, month, day)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            // Não permitir datas no passado
            datePicker.minDate = System.currentTimeMillis()
        }.show()
    }

    private fun launchTimePicker(year: Int, month: Int, day: Int) {
        val cal = Calendar.getInstance()
        TimePickerDialog(
            this,
            { _, hour, minute ->
                val chosen = Calendar.getInstance().apply {
                    set(year, month, day, hour, minute, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (chosen.timeInMillis <= System.currentTimeMillis()) {
                    Utility.showToast(this, "Please choose a future time")
                    return@TimePickerDialog
                }
                reminderTimestampMs = chosen.timeInMillis
                showReminderText(reminderTimestampMs)
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            true
        ).show()
    }

    /**
     * Mostra a data/hora do lembrete no TextView e o botão de cancelar.
     */
    private fun showReminderText(ms: Long) {
        val fmt = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault())
        reminderDateTextView!!.text = getString(R.string.reminder_set, fmt.format(ms))
        reminderDateTextView!!.visibility = View.VISIBLE
        cancelReminderBtn!!.visibility = View.VISIBLE
    }

    /**
     * Limpa o lembrete da UI (não cancela o AlarmManager ainda — só ao guardar).
     */
    private fun clearReminder() {
        reminderTimestampMs = 0L
        reminderDateTextView!!.visibility = View.GONE
        cancelReminderBtn!!.visibility = View.GONE
    }

    // ─── IMAGEM ───────────────────────────────────────────────────────────────

    private fun registerLaunchers() {
        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) processSelectedImage(uri)
        }
        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && cameraImageUri != null) processSelectedImage(cameraImageUri!!)
        }
        cameraPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) launchCamera()
            else Utility.showToast(this, "Camera permission is required to take photos")
        }
        galleryPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) galleryLauncher.launch("image/*")
            else Utility.showToast(this, "Storage permission is required to select images")
        }
        notificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) launchDatePicker()
            else Utility.showToast(this, "Notification permission is required for reminders")
        }
    }

    private fun showImageSourceDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.choose_image_source))
            .setItems(arrayOf(getString(R.string.gallery), getString(R.string.camera))) { _, which ->
                when (which) {
                    0 -> openGallery()
                    1 -> openCamera()
                }
            }
            .show()
    }

    private fun openGallery() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED)
            galleryLauncher.launch("image/*")
        else
            galleryPermissionLauncher.launch(permission)
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            launchCamera()
        else
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun launchCamera() {
        val imageFile = File(cacheDir, "camera_image_${System.currentTimeMillis()}.jpg")
        cameraImageUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", imageFile)
        cameraLauncher.launch(cameraImageUri!!)
    }

    private fun processSelectedImage(uri: Uri) {
        val bitmap = ImageUtils.decodeSampledBitmap(uri, contentResolver)
        if (bitmap != null) {
            currentImageBase64 = ImageUtils.bitmapToBase64(bitmap)
            showImagePreview(currentImageBase64!!)
            bitmap.recycle()
        } else {
            Utility.showToast(this, "Failed to load image")
        }
    }

    private fun showImagePreview(base64: String) {
        val bitmap = ImageUtils.base64ToBitmap(base64)
        if (bitmap != null) {
            noteImagePreview!!.setImageBitmap(bitmap)
            noteImagePreview!!.visibility = View.VISIBLE
            removeImageBtn!!.visibility = View.VISIBLE
        }
    }

    private fun removeImage() {
        currentImageBase64 = null
        noteImagePreview!!.setImageBitmap(null)
        noteImagePreview!!.visibility = View.GONE
        removeImageBtn!!.visibility = View.GONE
    }

    // ─── GUARDAR / APAGAR ─────────────────────────────────────────────────────

    fun saveNote() {
        val noteTitle = titleEditText!!.getText().toString()
        val noteContent = contentEditText!!.getText().toString()
        if (noteTitle.isEmpty()) {
            titleEditText!!.error = "Title is required"
            return
        }

        val note = Note()
        note.setTitle(noteTitle)
        note.setContent(noteContent)
        note.setTimestamp(now())
        note.setImageBase64(currentImageBase64)
        note.setPinOrder(if (isPinned) 1 else 0)
        note.setReminderTimestamp(reminderTimestampMs)

        saveNoteToFirebase(note, noteTitle)
    }

    fun saveNoteToFirebase(note: Note, noteTitle: String) {
        val documentReference: DocumentReference = if (isEditMode)
            Utility.getCollectionReferenceForNotes().document(docId.toString())
        else
            Utility.getCollectionReferenceForNotes().document()

        val targetDocId = documentReference.id

        documentReference.set(note).addOnCompleteListener(object : OnCompleteListener<Void?> {
            override fun onComplete(task: Task<Void?>) {
                if (task.isSuccessful) {
                    // Gerir AlarmManager conforme o lembrete
                    if (reminderTimestampMs > 0L) {
                        ReminderScheduler.scheduleReminder(
                            this@NoteDetailsActivity,
                            targetDocId,
                            noteTitle,
                            reminderTimestampMs
                        )
                    } else {
                        // Se havia lembrete e foi cancelado, garantir que o alarme é removido
                        ReminderScheduler.cancelReminder(this@NoteDetailsActivity, targetDocId)
                    }
                    Utility.showToast(this@NoteDetailsActivity, "Note saved successfully")
                    finish()
                } else {
                    Utility.showToast(this@NoteDetailsActivity, "Failed while saving note")
                }
            }
        })
    }

    fun deleteNoteFromFirebase() {
        val documentReference: DocumentReference =
            Utility.getCollectionReferenceForNotes().document(docId.toString())
        documentReference.delete().addOnCompleteListener(object : OnCompleteListener<Void?> {
            override fun onComplete(task: Task<Void?>) {
                if (task.isSuccessful) {
                    // Cancelar lembrete ao apagar nota
                    ReminderScheduler.cancelReminder(this@NoteDetailsActivity, docId!!)
                    Utility.showToast(this@NoteDetailsActivity, "Note deleted successfully")
                    finish()
                } else {
                    Utility.showToast(this@NoteDetailsActivity, "Failed while deleting note")
                }
            }
        })
    }
}