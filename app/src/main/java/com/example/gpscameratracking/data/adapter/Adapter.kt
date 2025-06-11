package com.example.gpscameratracking.data.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.gpscameratracking.databinding.LayoutItemBinding

class Adapter(private val context: Context, private var list: List<String>) :
    RecyclerView.Adapter<Adapter.ViewHolder>() {
    private var selectedMode = false
    private val selectedItems = mutableSetOf<Int>()
    var onSelectionChanged: ((count: Int) -> Unit)? = null
    inner class ViewHolder(val binding: LayoutItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = LayoutItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(newUrls: List<String>) {
        list = newUrls
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return list.size
    }
    fun clearSelection() {
        selectedItems.clear()
        selectedMode = false
        notifyDataSetChanged()
    }
    fun getSelectedUrls(): List<String> {
        return selectedItems.map { position -> list[position] }
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.binding.lottieLoading.visibility = android.view.View.VISIBLE

        Glide.with(context).load(item)
            .centerCrop()
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    holder.binding.lottieLoading.visibility = android.view.View.GONE
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    holder.binding.lottieLoading.visibility = android.view.View.GONE
                    return false
                }

            })
            .into(holder.binding.imgPhoto)
        val isSelected = selectedItems.contains(position)
        holder.binding.checkbox.visibility = if (selectedMode) android.view.View.VISIBLE else android.view.View.GONE
        holder.binding.overlay.visibility = if (isSelected) android.view.View.VISIBLE else android.view.View.GONE
        holder.binding.checkbox.isChecked = isSelected

        holder.itemView.setOnLongClickListener {
            if(!selectedMode) {
                selectedMode = true
                selectedItems.add(position)
                notifyDataSetChanged()
                onSelectionChanged?.invoke(selectedItems.size)
            }
            true
        }

        holder.itemView.setOnClickListener {
            if (selectedMode) {
                if (selectedItems.contains(position)) {
                    selectedItems.remove(position)
                } else {
                    selectedItems.add(position)
                }
                notifyItemChanged(position)
                onSelectionChanged?.invoke(selectedItems.size)

                if(selectedItems.isEmpty()) {
                    selectedMode = false
                    notifyDataSetChanged()
                }
            }
        }
    }
}