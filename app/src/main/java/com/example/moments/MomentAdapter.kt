package com.example.moments

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Locale

class MomentAdapter(private val mainActivity: MainActivity) :
    RecyclerView.Adapter<MomentAdapter.ViewHolderMoment>() {

    var momentList = mutableListOf<Moment>()

    inner class ViewHolderMoment(view: View) : RecyclerView.ViewHolder(view) {
        internal var title = view.findViewById<View>(R.id.viewTitle) as TextView
        internal var contents = view.findViewById<View>(R.id.viewContents) as TextView
      internal  var image = view.findViewById<View>(R.id.imageViewPreview) as ImageView
        internal    var address = view.findViewById<View>(R.id.viewAddress) as TextView
        internal var date = view.findViewById<View>(R.id.viewDate) as TextView // Ajoutez cette ligne

        init {
            view.isClickable = true
            view.setOnClickListener {
                mainActivity.showMoment(layoutPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderMoment {
        return ViewHolderMoment(LayoutInflater.from(parent.context).inflate(R.layout.moment_preview, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolderMoment, position: Int) {
        val moment = momentList[position]

        holder.title.text = moment.title
        holder.contents.text = if (moment.contents.length < 15) moment.contents
        else moment.contents.substring(0, 15) + "..."
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        holder.date.text = dateFormat.format(moment.date)
        holder.date.visibility = View.VISIBLE
        moment.address?.let { address ->
            holder.address.text = address
            holder.address.visibility = View.VISIBLE
        } ?: run {
            holder.address.visibility = View.GONE
        }
        moment.photoPath?.let { uriString ->
            if (uriString.isNotEmpty()) {
                val uri = Uri.parse(uriString)
                Glide.with(holder.itemView.context)
                    .load(uri)
                    .into(holder.image)
                holder.image.visibility = View.VISIBLE
            } else {
                holder.image.visibility = View.GONE
            }
        } ?: run {
            holder.image.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = momentList.size
}