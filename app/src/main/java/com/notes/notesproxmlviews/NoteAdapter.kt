package com.notes.notesproxmlviews

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions

// Adapter que liga os dados do Firestore ao RecyclerView
// Herda do FirestoreRecyclerAdapter em vez do RecyclerView.Adapter normal
// A grande vantagem é que o FirestoreRecyclerAdapter sincroniza automaticamente
// com o Firestore — quando os dados mudam na base de dados (ex: nova nota adicionada,
// nota apagada), a lista atualiza-se automaticamente sem precisar de código extra
//
// Recebe:
// - options: as opções do FirestoreRecyclerAdapter (query + tipo de dados)
// - context: o contexto da Activity, necessário para abrir o NoteDetailsActivity
class NoteAdapter(
    options: FirestoreRecyclerOptions<Note>, // opções que definem que dados carregar do Firestore
    private val context: Context             // contexto da Activity para navegação
) : FirestoreRecyclerAdapter<Note, NoteAdapter.NoteViewHolder>(options) {

    // O ViewHolder é um padrão de design do Android para otimizar o RecyclerView
    // Em vez de chamar findViewById() para cada item da lista (o que é lento),
    // o ViewHolder guarda as referências às views uma única vez quando o item é criado
    // e depois reutiliza essas referências quando o item é reciclado para mostrar novos dados
    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // referências às views do layout note_item.xml
        val titleTextView: TextView = itemView.findViewById(R.id.note_title_text_view)     // título da nota
        val contentTextView: TextView = itemView.findViewById(R.id.note_content_text_view) // conteúdo da nota
        val dateTextView: TextView = itemView.findViewById(R.id.note_date_text_view)       // data da nota
    }

    // onCreateViewHolder é chamado pelo RecyclerView quando precisa de criar um novo item visual
    // isto acontece quando a lista é carregada pela primeira vez ou quando o utilizador faz scroll
    // e aparecem novos itens que ainda não foram criados
    // infla (cria) o layout note_item.xml e cria um ViewHolder com as referências às suas views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        // LayoutInflater converte o ficheiro XML note_item.xml numa View Android
        // parent é o RecyclerView que vai conter este item
        // false significa que não adicionamos o item ao parent imediatamente
        val view = LayoutInflater.from(parent.context).inflate(R.layout.note_item, parent, false)
        return NoteViewHolder(view)
    }

    // onBindViewHolder é chamado quando o RecyclerView precisa de preencher um item com dados
    // isto acontece quando um item fica visível no ecrã (ex: ao fazer scroll)
    // recebe o ViewHolder (com as referências às views) e o objeto Note com os dados
    override fun onBindViewHolder(holder: NoteViewHolder, position: Int, note: Note) {
        // preenche o título da nota na TextView correspondente
        holder.titleTextView.text = note.title

        // preenche o conteúdo da nota na TextView correspondente
        holder.contentTextView.text = note.content

        // converte o Timestamp do Firestore para uma string formatada como "MM/DD/YYYY"
        // ex: Timestamp(2024, 1, 15) -> "01/15/2024"
        holder.dateTextView.text = Utility.timestampToString(note.timestamp)

        // define o comportamento quando o utilizador clica numa nota
        holder.itemView.setOnClickListener {
            // cria um Intent para abrir o NoteDetailsActivity em modo de edição
            val intent = Intent(context, NoteDetailsActivity::class.java)

            // passa o título e conteúdo da nota para pré-preencher os campos de edição
            intent.putExtra("title", note.title)
            intent.putExtra("content", note.content)

            // obtém o ID do documento do Firestore para este item da lista
            // este ID é necessário para saber qual documento atualizar ou apagar no Firestore
            // snapshots é uma propriedade do FirestoreRecyclerAdapter que contém
            // os documentos do Firestore correspondentes a cada posição da lista
            intent.putExtra("docId", snapshots.getSnapshot(position).id)

            // abre o NoteDetailsActivity com os dados da nota selecionada
            context.startActivity(intent)
        }
    }
}