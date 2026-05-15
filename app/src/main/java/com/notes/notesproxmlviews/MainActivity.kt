package com.notes.notesproxmlviews

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
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

        // Criar o canal de notificações (obrigatório Android 8+, inócuo em versões anteriores)
        createNotificationChannel()

        // Inicializar views do layout
        addNoteBtn = findViewById(R.id.add_note_btn)
        recyclerView = findViewById(R.id.recyler_view)
        menuBtn = findViewById(R.id.menu_btn)

        // Quando o utilizador clica no botão "+", abre o NoteDetailsActivity para criar uma nova nota
        addNoteBtn!!.setOnClickListener {
            startActivity(Intent(this@MainActivity, NoteDetailsActivity::class.java))
        }

        // Quando o utilizador clica no botão do menu, mostra o menu de opções
        menuBtn!!.setOnClickListener { showMenu() }

        // Migrar notas antigas que não têm o campo pinOrder (criadas antes da funcionalidade Pin)
        // Esta migração só precisa de correr uma vez — usa SharedPreferences como flag de controlo
        migrateExistingNotes()

        // Configura o RecyclerView com os dados do Firestore
        setupRecyclerView()
    }

    /**
     * Migração única de dados: adiciona os campos pinOrder=0 e reminderTimestamp=0
     * a todos os documentos do Firestore que ainda não os têm.
     *
     * PORQUÊ É NECESSÁRIO:
     * O Firestore, ao fazer .orderBy("pinOrder"), exclui automaticamente todos os documentos
     * que não têm esse campo — mesmo que o valor padrão no Java seja 0.
     * As notas criadas antes de adicionarmos o campo "pinOrder" ficam "invisíveis" para a query.
     *
     * COMO FUNCIONA:
     * - Vai buscar todos os documentos da coleção de notas do utilizador
     * - Para cada documento sem "pinOrder", faz um update a adicionar pinOrder=0 e reminderTimestamp=0
     * - Usa SharedPreferences para garantir que esta migração só corre uma única vez
     */
    private fun migrateExistingNotes() {
        val prefs = getSharedPreferences("app_migration", MODE_PRIVATE)
        val migrationDone = prefs.getBoolean("pinOrder_migration_done", false)

        // Se a migração já foi feita anteriormente, não precisamos de correr de novo
        if (migrationDone) return

        Utility.getCollectionReferenceForNotes().get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    // Sem notas para migrar — marcar como concluído
                    prefs.edit().putBoolean("pinOrder_migration_done", true).apply()
                    return@addOnSuccessListener
                }

                var pendingUpdates = snapshot.documents.size
                var completedUpdates = 0

                for (document in snapshot.documents) {
                    // Verificar se o campo pinOrder já existe no documento do Firestore
                    // (é diferente do valor default do Java — pode não existir de todo)
                    if (!document.contains("pinOrder")) {
                        // Adicionar os campos em falta sem apagar os restantes dados da nota
                        document.reference.update(
                            "pinOrder", 0,
                            "reminderTimestamp", 0L
                        ).addOnCompleteListener {
                            completedUpdates++
                            // Quando todos os updates estiverem completos, marcar migração como feita
                            if (completedUpdates >= pendingUpdates) {
                                prefs.edit().putBoolean("pinOrder_migration_done", true).apply()
                                android.util.Log.d("Migration", "pinOrder migration complete: $completedUpdates documents updated")
                            }
                        }
                    } else {
                        // Documento já tem o campo — contar como "concluído"
                        completedUpdates++
                        if (completedUpdates >= pendingUpdates) {
                            prefs.edit().putBoolean("pinOrder_migration_done", true).apply()
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("Migration", "Failed to migrate notes: ${e.message}")
            }
    }


    /**
     * Cria o Notification Channel para os lembretes.
     * Obrigatório em Android 8.0 (API 26)+. Em versões anteriores não tem efeito.
     * Deve ser chamado o mais cedo possível (onCreate), pois pode ser chamado várias
     * vezes sem problemas — o sistema ignora criações duplicadas.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = getString(R.string.reminder_notification_channel_id)
            val channelName = getString(R.string.reminder_notification_channel_name)
            val channelDesc = getString(R.string.reminder_notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDesc
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Configura o RecyclerView conectando-o através da Query ao Firestore.
     * Ordena primeiro pelas notas fixadas (pinOrder DESC) e depois pela mais recente (timestamp DESC).
     *
     * NOTA: Esta query com dois campos requer um índice composto no Firestore.
     * Ao correr a app pela primeira vez, o Logcat mostrará um link direto para criar
     * o índice automaticamente na consola Firebase — basta clicar nesse link.
     */
    fun setupRecyclerView() {
        val query: Query = Utility.getCollectionReferenceForNotes()
            .orderBy("pinOrder", Query.Direction.DESCENDING)
            .orderBy("timestamp", Query.Direction.DESCENDING)

        val options = FirestoreRecyclerOptions.Builder<Note>()
            .setQuery(query, Note::class.java)
            .build()

        recyclerView!!.layoutManager = LinearLayoutManager(this)
        noteAdapter = NoteAdapter(options, this)
        recyclerView!!.adapter = noteAdapter
    }

    /**
     * Invocado quando a activity fica visível para o utilizador.
     * Inicia a escuta de alterações no Firestore em tempo real através do startListening().
     */
    override fun onStart() {
        super.onStart()
        noteAdapter!!.startListening()
    }

    /**
     * Invocado quando a activity fica invisível.
     * O stopListening() interrompe a escuta, preservando recursos e quotas do Firestore.
     */
    override fun onStop() {
        super.onStop()
        noteAdapter!!.stopListening()
    }

    /**
     * Invocado quando a activity volta ao primeiro plano.
     * Garante que a lista é refrescada com dados atualizados.
     */
    override fun onResume() {
        super.onResume()
        noteAdapter!!.notifyDataSetChanged()
    }

    /**
     * Exibe o PopupMenu de opções ao pé do botão de menu.
     */
    fun showMenu() {
        val popupMenu = android.widget.PopupMenu(this@MainActivity, menuBtn)
        popupMenu.menu.add("Logout")
        popupMenu.show()
        popupMenu.setOnMenuItemClickListener { menuItem ->
            if (menuItem.title == "Logout") {
                FirebaseAuth.getInstance().signOut()
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