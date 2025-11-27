package com.example.photo6

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import android.widget.ImageView
import java.io.File

class PhotoFolderFragment : Fragment() {

    private lateinit var folder: File
    private val photoList = mutableListOf<File>()

    companion object {
        private const val ARG_FOLDER_PATH = "folder_path"

        fun newInstance(folderPath: String): PhotoFolderFragment {
            val fragment = PhotoFolderFragment()
            val bundle = Bundle()
            bundle.putString(ARG_FOLDER_PATH, folderPath)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val path = arguments?.getString(ARG_FOLDER_PATH)
        if (path != null) {
            folder = File(path)
        } else {
            throw IllegalArgumentException("Folder path required")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_photo_folder, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView: RecyclerView = view.findViewById(R.id.rv_photos)

        loadPhotos()

        // 根据屏幕宽度和最小宽度自适应列数
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val minItemWidthPx = (120 * displayMetrics.density).toInt()

// 自动列数，根据屏幕宽度计算
        val spanCount = (screenWidth / minItemWidthPx).coerceAtLeast(2)

// 布局管理器
        recyclerView.layoutManager = GridLayoutManager(requireContext(), spanCount)

// 设置 adapter
        recyclerView.adapter = PhotoAdapter(photoList) { file ->
            Toast.makeText(requireContext(), "点击图片: ${file.name}", Toast.LENGTH_SHORT).show()
            Log.d("PhotoFolder", "点击图片${file.name}")
        }

// 计算间距 dp -> px
        val spacing = (1 * displayMetrics.density).toInt()

// RecyclerView 内边距
        recyclerView.setPadding(0, 0, 0, 0)
        recyclerView.clipToPadding = false


// 删除已有装饰
 //       while (recyclerView.itemDecorationCount > 0) {
  //          recyclerView.removeItemDecorationAt(0)
 //       }
// 添加网格间距
 //       recyclerView.addItemDecoration(GridSpacingItemDecoration(spanCount, spacing))

    }

    private fun loadPhotos() {
        photoList.clear()
        folder.listFiles()?.forEach {
            val ext = it.extension.lowercase()
            if (it.isFile && ext in listOf("jpg","jpeg","png","webp","gif")) {
                photoList.add(it)
            }
        }
    }

    inner class PhotoAdapter(
        private val photos: List<File>,
        private val onItemClick: (File) -> Unit
    ) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

        inner class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imageView: ImageView = itemView.findViewById(R.id.iv_file_icon)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_photo_thumbnail, parent, false)
            return PhotoViewHolder(view)
        }

        override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
            val file = photos[position]
            Glide.with(holder.imageView.context)
                .load(file)
                .centerCrop()
                .into(holder.imageView)

            holder.imageView.setOnClickListener { onItemClick(file) }
        }

        override fun getItemCount(): Int = photos.size
    }
}
