package com.notes.notesproxmlviews

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions

/**
 * Adapter que liga os dados do Firestore ao RecyclerView.
 *
 * Herda do [FirestoreRecyclerAdapter] em vez do [RecyclerView.Adapter] normal.
 * A grande vantagem é que o FirestoreRecyclerAdapter sincroniza automaticamente
 * com o Firestore — quando os dados mudam na base de dados (ex: nova nota adicionada,
 * nota apagada), a lista atualiza-se automaticamente sem precisar de código extra.
 *
 * @param options As opções do FirestoreRecyclerAdapter (query + tipo de dados).
 * @param context O contexto da Activity, necessário para abrir a NoteDetailsActivity.
 */
class NoteAdapter(
    options: FirestoreRecyclerOptions<Note>,
    private val context: Context
) : FirestoreRecyclerAdapter<Note, NoteAdapter.NoteViewHolder>(options) {

    /**
     * O ViewHolder é um padrão de design do Android para otimizar o RecyclerView.
     * Em vez de chamar findViewById() para cada item da lista (o que é lento),
     * o ViewHolder guarda as referências às views uma única vez quando o item é criado
     * e depois reutiliza essas referências quando o item é reciclado para mostrar novos dados.
     */
    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.note_title_text_view)
        val contentTextView: TextView = itemView.findViewById(R.id.note_content_text_view)
        val dateTextView: TextView = itemView.findViewById(R.id.note_date_text_view)
        val noteImageView: ImageView = itemView.findViewById(R.id.note_image_view)
        val pinIconView: ImageView = itemView.findViewById(R.id.pin_icon_view)
        val reminderIconView: ImageView = itemView.findViewById(R.id.reminder_icon_view)
    }

    /**
     * Chamado pelo RecyclerView quando precisa de criar um novo item visual.
     * Infla (cria) o layout note_item.xml e devolve o ViewHolder com as referências às views.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.note_item, parent, false)
        return NoteViewHolder(view)
    }

    /**
     * Chamado quando o RecyclerView precisa de preencher um item com dados.
     *
     * @param holder O ViewHolder com as referências às views.
     * @param position A posição atual do item na lista.
     * @param note O objeto Note que contém os dados a exibir.
     */
    override fun onBindViewHolder(holder: NoteViewHolder, position: Int, note: Note) {
        // Título e conteúdo
        holder.titleTextView.text = note.title
        holder.contentTextView.text = note.content
        holder.dateTextView.text = Utility.timestampToString(note.timestamp)

        // Thumbnail da imagem
        val imageBase64 = note.imageBase64
        if (!imageBase64.isNullOrEmpty()) {
            val bitmap = ImageUtils.base64ToBitmap(imageBase64)
            if (bitmap != null) {
                holder.noteImageView.setImageBitmap(bitmap)
                holder.noteImageView.visibility = View.VISIBLE
            } else {
                holder.noteImageView.visibility = View.GONE
            }
        } else {
            holder.noteImageView.visibility = View.GONE
        }

        // Ícone de pin — visível e colorido quando fixada
        if (note.pinOrder == 1) {
            holder.pinIconView.visibility = View.VISIBLE
        } else {
            holder.pinIconView.visibility = View.GONE
        }

        // Ícone de lembrete (sino) — visível quando há lembrete no futuro
        val hasActiveReminder = note.reminderTimestamp > 0L &&
                note.reminderTimestamp > System.currentTimeMillis()
        holder.reminderIconView.visibility = if (hasActiveReminder) View.VISIBLE else View.GONE

        // Click para editar nota
        holder.itemView.setOnClickListener {
            val intent = Intent(context, NoteDetailsActivity::class.java).apply {
                putExtra("title", note.title)
                putExtra("content", note.content)
                putExtra("docId", snapshots.getSnapshot(position).id)
                putExtra("imageBase64", note.imageBase64)
                putExtra("pinOrder", note.pinOrder)
                putExtra("reminderTimestamp", note.reminderTimestamp)
            }
            context.startActivity(intent)
        }
    }

    /**
     * Apanha erros do Firestore (como a falta de um índice) e exibe-os na interface
     * para que o utilizador possa ver o link diretamente.
     */
    override fun onError(e: com.google.firebase.firestore.FirebaseFirestoreException) {
        super.onError(e)
        android.util.Log.e("NoteAdapter", "Firestore Error: ${e.message}")
        
        // Exibe um AlertDialog com o erro, que contém o link direto para criar o índice
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("⚠️ Índice do Firestore Necessário")
                .setMessage("Para ordenar as notas fixadas, é necessário criar um índice no Firestore.\n\nO Firebase gerou o seguinte link para o criares automaticamente:\n\n${e.message}")
                .setPositiveButton("OK", null)
                .show()
        }
    }
}