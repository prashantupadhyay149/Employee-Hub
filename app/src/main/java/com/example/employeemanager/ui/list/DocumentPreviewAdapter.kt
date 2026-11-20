package com.example.employeemanager.ui.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.employeemanager.R
import com.example.employeemanager.data.model.Document
import com.example.employeemanager.databinding.ItemDocumentPreviewBinding

class DocumentPreviewAdapter :
    ListAdapter<Document, DocumentPreviewAdapter.DocViewHolder>(DIFF_CALLBACK) {

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Document>() {
            override fun areItemsTheSame(a: Document, b: Document) =
                a.documentId == b.documentId
            override fun areContentsTheSame(a: Document, b: Document) = a == b
        }
    }

    inner class DocViewHolder(private val binding: ItemDocumentPreviewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(doc: Document) {
            binding.tvDocumentName.text = doc.documentName
            binding.tvDocumentTypeBadge.text = doc.documentType.uppercase()

            // Load thumbnail with Glide; fallback to placeholder icon
            if (doc.thumbnailUrl.isNotBlank() && doc.documentType.uppercase() != "PDF") {
                Glide.with(binding.ivDocumentThumbnail.context)
                    .load(doc.thumbnailUrl)
                    .placeholder(R.drawable.ic_description)
                    .error(R.drawable.ic_description)
                    .centerCrop()
                    .into(binding.ivDocumentThumbnail)
            } else {
                binding.ivDocumentThumbnail.setImageResource(R.drawable.ic_description)
                binding.ivDocumentThumbnail.scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
            }

            // Verified badge
            binding.ivVerifiedBadge.visibility =
                if (doc.isVerified) View.VISIBLE else View.GONE

            // Type badge tint
            val (tintColor, bgColor) = when (doc.documentType.uppercase()) {
                "PDF" -> Pair(
                    binding.root.context.getColor(R.color.badge_pdf),
                    binding.root.context.getColor(R.color.error_bg)
                )
                "JPG", "JPEG" -> Pair(
                    binding.root.context.getColor(R.color.badge_jpg),
                    binding.root.context.getColor(R.color.primary_container)
                )
                else -> Pair(
                    binding.root.context.getColor(R.color.text_secondary),
                    binding.root.context.getColor(R.color.surface_variant)
                )
            }
            binding.tvDocumentTypeBadge.setTextColor(tintColor)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocViewHolder {
        val binding = ItemDocumentPreviewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DocViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DocViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
