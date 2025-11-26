package com.example.photo6

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

/**
 * FolderAdapter：支持动态切换选择模式（isSelectionMode 可变）
 * selectedFolders 使用 MutableSet<File> 引用，这样 Fragment 修改集合时 Adapter 能感知到。
 */
class FolderAdapter(
    private val folders: List<File>,
    // 传入可变集合引用（Fragment 持有同一份 selectedFolders）
    private val selectedFolders: MutableSet<File>,
    private val onItemClick: (File) -> Unit
) : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {

    // 选择模式由外部或 Fragment 控制：可变属性
    var isSelectionMode: Boolean = false

    inner class FolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivIcon: ImageView = itemView.findViewById(R.id.iv_file_icon)
        val tvName: TextView = itemView.findViewById(R.id.tv_file_name)
        val vSelectionBg: View? = itemView.findViewById(R.id.v_selection_bg) // 背景 View（可能为 null）
        val ivSelection: ImageView? = itemView.findViewById(R.id.iv_selection) // 勾选图标（可能为 null）
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_folder, parent, false)
        return FolderViewHolder(view)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        val folder = folders[position]
        holder.tvName.text = folder.name

        // --- 根据当前选择模式与是否被选中来更新 UI ---
        if (isSelectionMode) {
            // 显示覆盖层（背景 + 勾选图标）

            holder.ivSelection?.visibility = View.VISIBLE
            Log.d("FolderAdapter", "选择模式")
            if (selectedFolders.contains(folder)) {
                // 已选中：显示已选的图标或样式
                holder.ivSelection?.setImageResource(R.drawable.ic_checked) // 你需要准备 ic_checked
                // v_selection_bg 的样式已由 XML drawable 控制（圆角、颜色）
                Log.d("FolderAdapter", "已选择")
                holder.vSelectionBg?.visibility = View.VISIBLE
            } else {
                // 未选中：显示未选图标或半透明状态（可用 ic_unchecked 或透明图）
                holder.ivSelection?.setImageResource(R.drawable.ic_unchecked) // 可选：未选图标
                Log.d("FolderAdapter", "未选择")
                holder.vSelectionBg?.visibility = View.GONE
            }
        } else {
            // 普通模式：隐藏选择覆盖
            holder.vSelectionBg?.visibility = View.GONE
            holder.ivSelection?.visibility = View.GONE
        }

        // 点击行为由 Fragment 提供的回调处理（会在 Fragment 内决定是选中还是打开）
        holder.itemView.setOnClickListener {
            onItemClick(folder)
        }
    }

    override fun getItemCount(): Int = folders.size
}
