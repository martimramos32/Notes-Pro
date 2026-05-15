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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query

class MainActivity : AppCompatActivity() {

    var addNoteBtn: FloatingActionButton? = null
    var recyclerView: RecyclerView? = null
    var menuBtn: ImageButton? = null

    /**
     * Adapter que liga os dados do Firestore ao RecyclerView.
     * É declarado aqui para poder ser acedido nos métodos do ciclo de vida (onStart, onStop e onResume).
     */
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

    /**
     * Configura o RecyclerView conectando-o através da Query ao Firestore.
     * Organiza as visualizações num Layout Manager e atribui-lhes o adapter apropriado.
     */
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

    /**
     * Invocado quando a activity fica visível para o utilizador.
     * Inicia a escuta de alterações no Firestore em tempo real através do startListening().
     * Caso uma nota sofra alterações, a lista é atualizada automaticamente em todos os dispositivos.
     */
    override fun onStart() {
        super.onStart()
        noteAdapter!!.startListening()
    }

    /**
     * Invocado quando a activity fica invisível (ex: utilizador deitou a app para background).
     * O stopListening() interrompe a escuta de alterações, preservando os recursos do dispositivo e as quotas do Firestore.
     */
    override fun onStop() {
        super.onStop()
        noteAdapter!!.stopListening()
    }

    /**
     * Invocado quando a activity volta ao primeiro plano.
     * Garante que o adapter é notificado e refresca a interface visual com quaisquer dados atualizados pendentes.
     */
    override fun onResume() {
        super.onResume()
        noteAdapter!!.notifyDataSetChanged()
    }

    /**
     * Exibe o PopupMenu de propriedades extra ao pé do ícone/botão de menu superior.
     */
    fun showMenu() {
        val popupMenu = android.widget.PopupMenu(this@MainActivity, menuBtn)
        //Adiciona a opção "Logout" à lista do menu
        popupMenu.menu.add("Logout")
        popupMenu.show()
        popupMenu.setOnMenuItemClickListener { menuItem ->
            if (menuItem.title == "Logout") {
                // Termina a sessão do utilizador
                FirebaseAuth.getInstance().signOut()
                // Vai para o ecrã de início de sessão (LoginActivity)
                val intent = Intent(this@MainActivity, LoginActivity::class.java)
                startActivity(intent)
                finish()
                true
            } else {
                false
            }
        }
    }
}