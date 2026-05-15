package com.notes.notesproxmlviews

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp.Companion.now
import com.google.firebase.firestore.DocumentReference

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_details)

        titleEditText = findViewById<EditText?>(R.id.notes_title_text)
        contentEditText = findViewById<EditText?>(R.id.notes_content_text)
        saveNoteBtn = findViewById<ImageButton?>(R.id.save_note_btn)
        pageTitleTextView = findViewById<TextView?>(R.id.page_title)
        deleteNoteTextViewBtn = findViewById<TextView?>(R.id.delete_note_text_view_btn)

        //receive data
        title = intent.getStringExtra("title")
        content = intent.getStringExtra("content")
        docId = intent.getStringExtra("docId")

        if (docId != null && !docId!!.isEmpty()) {
            isEditMode = true
        }

        titleEditText!!.setText(title)
        contentEditText!!.setText(content)
        if (isEditMode) {
            pageTitleTextView!!.text = getString(R.string.edit_your_note)
            deleteNoteTextViewBtn!!.visibility = View.VISIBLE
        }

        saveNoteBtn!!.setOnClickListener(View.OnClickListener { v: View? -> saveNote() })

        deleteNoteTextViewBtn!!.setOnClickListener(View.OnClickListener { v: View? -> deleteNoteFromFirebase() })
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