package com.notes.notesproxmlviews

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.Query

class MainActivity : AppCompatActivity() {

    var addNoteBtn: FloatingActionButton? = null
    var recyclerView: RecyclerView? = null
    var menuBtn: ImageButton? = null

    // adapter que liga os dados do Firestore ao RecyclerView
    // é declarado aqui para poder ser acedido nos métodos onStart, onStop e onResume
    var noteAdapter: NoteAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // inicializa as views do layout
        addNoteBtn = findViewById(R.id.add_note_btn)
        recyclerView = findViewById(R.id.recyler_view)
        menuBtn = findViewById(R.id.menu_btn)

        // quando o utilizador clica no botão "+", abre o NoteDetailsActivity para criar uma nova nota
        addNoteBtn!!.setOnClickListener {
            startActivity(Intent(this@MainActivity, NoteDetailsActivity::class.java))
        }

        // quando o utilizador clica no botão do menu, mostra o menu de opções
        menuBtn!!.setOnClickListener { showMenu() }

        // configura o RecyclerView com os dados do Firestore
        // é chamado no onCreate para que a lista seja carregada quando a activity é criada
        setupRecyclerView()
    }

    fun setupRecyclerView() {
        // cria uma query ao Firestore para obter as notas do utilizador atual
        // orderBy("timestamp", DESCENDING) ordena as notas da mais recente para a mais antiga
        // ou seja, a última nota criada aparece sempre no topo da lista
        val query: Query = Utility.getCollectionReferenceForNotes()
            .orderBy("timestamp", Query.Direction.DESCENDING)

        // FirestoreRecyclerOptions define como o adapter vai carregar os dados do Firestore
        // setQuery: define a query a usar e o tipo de dados (Note::class.java)
        // o adapter vai converter automaticamente cada documento do Firestore num objeto Note
        val options = FirestoreRecyclerOptions.Builder<Note>()
            .setQuery(query, Note::class.java)
            .build()

        // LinearLayoutManager organiza os itens do RecyclerView numa lista vertical
        // é o layout manager mais simples e adequado para uma lista de notas
        recyclerView!!.layoutManager = LinearLayoutManager(this)

        // cria o NoteAdapter com as opções do Firestore e o contexto da activity
        // e liga-o ao RecyclerView para que os dados sejam mostrados na lista
        noteAdapter = NoteAdapter(options, this)
        recyclerView!!.adapter = noteAdapter
    }

    // onStart é chamado quando a activity fica visível para o utilizador
    // startListening() inicia a escuta de alterações no Firestore em tempo real
    // ou seja, se uma nota for adicionada, editada ou apagada noutro dispositivo,
    // a lista atualiza-se automaticamente sem precisar de recarregar a app
    override fun onStart() {
        super.onStart()
        noteAdapter!!.startListening()
    }

    // onStop é chamado quando a activity fica invisível (ex: o utilizador saiu da app)
    // stopListening() para a escuta de alterações no Firestore
    // isto é importante para evitar atualizações desnecessárias quando a app está em background
    // e para não desperdiçar recursos do dispositivo e da quota do Firestore
    override fun onStop() {
        super.onStop()
        noteAdapter!!.stopListening()
    }

    // onResume é chamado quando a activity volta ao primeiro plano
    // por exemplo, após o utilizador editar ou apagar uma nota e voltar à lista
    // notifyDataSetChanged() informa o adapter que os dados podem ter mudado
    // e que deve atualizar a lista para refletir as alterações mais recentes
    override fun onResume() {
        super.onResume()
        noteAdapter!!.notifyDataSetChanged()
    }

    fun showMenu() {
        // PopupMenu mostra um menu de opções junto ao botão do menu
        val popupMenu = android.widget.PopupMenu(this@MainActivity, menuBtn)
        popupMenu.show()
    }
}