package com.notes.notesproxmlviews

import android.Manifest
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

    // Novos campos para imagem
    var noteImagePreview: ImageView? = null
    var addImageBtn: MaterialButton? = null
    var removeImageBtn: MaterialButton? = null
    var currentImageBase64: String? = null

    // URI temporário para a foto tirada com a câmara
    private var cameraImageUri: Uri? = null

    // Launchers para selecionar imagem
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var galleryPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_details)

        titleEditText = findViewById<EditText?>(R.id.notes_title_text)
        contentEditText = findViewById<EditText?>(R.id.notes_content_text)
        saveNoteBtn = findViewById<ImageButton?>(R.id.save_note_btn)
        pageTitleTextView = findViewById<TextView?>(R.id.page_title)
        deleteNoteTextViewBtn = findViewById<TextView?>(R.id.delete_note_text_view_btn)

        // Inicializar views de imagem
        noteImagePreview = findViewById(R.id.note_image_preview)
        addImageBtn = findViewById(R.id.add_image_btn)
        removeImageBtn = findViewById(R.id.remove_image_btn)

        // Registar launchers
        registerLaunchers()

        //receive data
        title = intent.getStringExtra("title")
        content = intent.getStringExtra("content")
        docId = intent.getStringExtra("docId")
        val imageBase64 = intent.getStringExtra("imageBase64")

        if (docId != null && !docId!!.isEmpty()) {
            isEditMode = true
        }

        titleEditText!!.setText(title)
        contentEditText!!.setText(content)
        if (isEditMode) {
            pageTitleTextView!!.text = getString(R.string.edit_your_note)
            deleteNoteTextViewBtn!!.visibility = View.VISIBLE
        }

        // Se a nota já tem imagem, mostrar o preview
        if (!imageBase64.isNullOrEmpty()) {
            currentImageBase64 = imageBase64
            showImagePreview(imageBase64)
        }

        saveNoteBtn!!.setOnClickListener(View.OnClickListener { v: View? -> saveNote() })

        deleteNoteTextViewBtn!!.setOnClickListener(View.OnClickListener { v: View? -> deleteNoteFromFirebase() })

        // Botão "Adicionar Imagem" — mostra diálogo para escolher galeria ou câmara
        addImageBtn!!.setOnClickListener { showImageSourceDialog() }

        // Botão "Remover Imagem" — remove a imagem da nota
        removeImageBtn!!.setOnClickListener { removeImage() }
    }

    /**
     * Regista os ActivityResultLaunchers para galeria, câmara e permissões.
     * Deve ser chamado no onCreate() antes de qualquer interação.
     */
    private fun registerLaunchers() {
        // Launcher para selecionar imagem da galeria
        galleryLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                processSelectedImage(uri)
            }
        }

        // Launcher para tirar foto com a câmara
        cameraLauncher = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success: Boolean ->
            if (success && cameraImageUri != null) {
                processSelectedImage(cameraImageUri!!)
            }
        }

        // Launcher para permissão de câmara
        cameraPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                launchCamera()
            } else {
                Utility.showToast(this, "Camera permission is required to take photos")
            }
        }

        // Launcher para permissão de galeria
        galleryPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                galleryLauncher.launch("image/*")
            } else {
                Utility.showToast(this, "Storage permission is required to select images")
            }
        }
    }

    /**
     * Mostra um diálogo para o utilizador escolher entre galeria e câmara.
     */
    private fun showImageSourceDialog() {
        val options = arrayOf(
            getString(R.string.gallery),
            getString(R.string.camera)
        )

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.choose_image_source))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openGallery()
                    1 -> openCamera()
                }
            }
            .show()
    }

    /**
     * Abre a galeria para selecionar uma imagem.
     * Verifica permissões conforme a versão do Android.
     */
    private fun openGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+: READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                == PackageManager.PERMISSION_GRANTED
            ) {
                galleryLauncher.launch("image/*")
            } else {
                galleryPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            // Android 12 e abaixo: READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                galleryLauncher.launch("image/*")
            } else {
                galleryPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    /**
     * Abre a câmara para tirar uma foto.
     * Verifica permissão de câmara primeiro.
     */
    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            launchCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    /**
     * Cria um ficheiro temporário e lança a câmara.
     * O URI do ficheiro é passado via FileProvider para compatibilidade com Android 7+.
     */
    private fun launchCamera() {
        val imageFile = File(cacheDir, "camera_image_${System.currentTimeMillis()}.jpg")
        cameraImageUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            imageFile
        )
        cameraLauncher.launch(cameraImageUri!!)
    }

    /**
     * Processa a imagem selecionada (da galeria ou câmara):
     * 1. Carrega o bitmap de forma eficiente
     * 2. Comprime e converte para Base64
     * 3. Mostra o preview
     */
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

    /**
     * Mostra o preview da imagem e o botão de remover.
     */
    private fun showImagePreview(base64: String) {
        val bitmap = ImageUtils.base64ToBitmap(base64)
        if (bitmap != null) {
            noteImagePreview!!.setImageBitmap(bitmap)
            noteImagePreview!!.visibility = View.VISIBLE
            removeImageBtn!!.visibility = View.VISIBLE
        }
    }

    /**
     * Remove a imagem da nota (limpa o preview e o Base64).
     */
    private fun removeImage() {
        currentImageBase64 = null
        noteImagePreview!!.setImageBitmap(null)
        noteImagePreview!!.visibility = View.GONE
        removeImageBtn!!.visibility = View.GONE
    }

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

        saveNoteToFirebase(note)
    }

    fun saveNoteToFirebase(note: Note) {
        val documentReference: DocumentReference
        if (isEditMode) {
            //update the note
            documentReference = Utility.getCollectionReferenceForNotes().document(docId.toString())
        } else {
            //create new note
            documentReference = Utility.getCollectionReferenceForNotes().document()
        }



        documentReference.set(note).addOnCompleteListener(object : OnCompleteListener<Void?> {
            override fun onComplete(task: Task<Void?>) {
                if (task.isSuccessful) {
                    //note is added
                    Utility.showToast(this@NoteDetailsActivity, "Note added successfully")
                    finish()
                } else {
                    Utility.showToast(this@NoteDetailsActivity, "Failed while adding note")
                }
            }
        })
    }

    fun deleteNoteFromFirebase() {
        val documentReference: DocumentReference = Utility.getCollectionReferenceForNotes().document(
            docId.toString()
        )
        documentReference.delete().addOnCompleteListener(object : OnCompleteListener<Void?> {
            override fun onComplete(task: Task<Void?>) {
                if (task.isSuccessful) {
                    //note is deleted
                    Utility.showToast(this@NoteDetailsActivity, "Note deleted successfully")
                    finish()
                } else {
                    Utility.showToast(this@NoteDetailsActivity, "Failed while deleting note")
                }
            }
        })
    }
}