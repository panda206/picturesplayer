package com.example.photo6

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog // 推荐使用 AppCompat 的 AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import android.widget.ImageView
import java.io.File
import android.widget.TextView // 确保导入
import android.graphics.Color
import android.widget.Button
import android.widget.LinearLayout // 确保导入

class PhotoFolderFragment : Fragment() {

    private lateinit var folder: File
    private val photoList = mutableListOf<File>()

    // 1. 新增：状态变量
    private var isSelectionMode = false // 是否处于选择模式
    private val selectedPhotos = mutableSetOf<File>() // 存放被选中的照片
    // 声明移动和删除按钮
    private lateinit var bottomActionBar: LinearLayout
    private lateinit var btnMove: Button
    private lateinit var btnDelete: Button
    // 保留 Adapter 的引用以便刷新
    private lateinit var photoAdapter: PhotoAdapter
    private lateinit var ivSelect: ImageView
    private lateinit var ivSelectAll: TextView // 全选按钮

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
        // 处理物理返回键：如果是选择模式，先退出选择模式
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isSelectionMode) {
                    exitSelectionMode()
                    requireActivity().onBackPressed()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressed()
                }
            }
        })
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

        // 绑定返回和选择按钮
        val ivBack: ImageView = view.findViewById(R.id.iv_back)
        ivSelect = view.findViewById(R.id.iv_select) // 初始化全局变量
        ivSelectAll = view.findViewById(R.id.iv_select_all) // 【新增】找到全选按钮

        //加载照片
        loadPhotos()

        // 设置 Adapter (注意：这里不需要传点击回调了，逻辑移到 Adapter 内部判断)
        photoAdapter = PhotoAdapter(photoList)

        // 【新增】全选按钮点击逻辑
        ivSelectAll.setOnClickListener {
            // 点击动画
            it.animate().scaleX(0.85f).scaleY(0.85f).setDuration(120).withEndAction {
                it.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
            }.start()

            handleSelectAllClick()
        }


        // ① 返回
        ivBack.setOnClickListener {
            // 点击放大回弹动画
            ivBack.animate()
                .scaleX(0.85f)
                .scaleY(0.85f)
                .setDuration(120)
                .withEndAction {
                    ivBack.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(120)
                        .start()
                }
                .start()
            //返回
            requireActivity().onBackPressedDispatcher.onBackPressed()

        }

        // ② 选择模式
        ivSelect.setOnClickListener {
            // 点击放大回弹动画
            ivSelect.animate()
                .scaleX(0.85f)
                .scaleY(0.85f)
                .setDuration(120)
                .withEndAction {
                    ivSelect.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(120)
                        .start()
                }
                .start()
            // 切换模式
            toggleSelectionMode()

        }

        // 【新增】底部栏引用
        bottomActionBar = view.findViewById(R.id.bottom_action_bar)
        btnMove = view.findViewById(R.id.btn_move)
        btnDelete = view.findViewById(R.id.btn_delete)

        // 【新增】底部按钮监听 (占位逻辑)
        btnMove.setOnClickListener {
            Toast.makeText(requireContext(), "移动 ${selectedPhotos.size} 张照片", Toast.LENGTH_SHORT).show()
            // TODO: 真正的移动逻辑

        }
        btnDelete.setOnClickListener {
            Toast.makeText(requireContext(), "删除 ${selectedPhotos.size} 张照片", Toast.LENGTH_SHORT).show()
            // TODO: 真正的删除逻辑
            showDeleteConfirmationDialog()
        }

        // 布局计算逻辑
        // 根据屏幕宽度和最小宽度自适应列数
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val minItemWidthPx = (120 * displayMetrics.density).toInt()

        // 自动列数，根据屏幕宽度计算
        val spanCount = (screenWidth / minItemWidthPx).coerceAtLeast(3)

        // 布局管理器
        recyclerView.layoutManager = GridLayoutManager(requireContext(), spanCount)

        // 设置 adapter
        recyclerView.adapter = photoAdapter

        // 计算间距 dp -> px
        val spacing = (1 * displayMetrics.density).toInt()

        // RecyclerView 内边距
        recyclerView.setPadding(0, 0, 0, 0)
        recyclerView.clipToPadding = false


        // 删除已有装饰
        //while (recyclerView.itemDecorationCount > 0) {
        //    recyclerView.removeItemDecorationAt(0)
        //}
        // 添加网格间距
        //recyclerView.addItemDecoration(GridSpacingItemDecoration(spanCount, spacing))
        // 【新增】初始化顶部 UI 状态
        updateTopBarUI()
        updateBottomActionUI() // 初始化时隐藏底部栏

    }
    // 【新增】全选/取消全选逻辑
    private fun handleSelectAllClick() {
        // 判断是否已经全选
        val isAllSelected = selectedPhotos.size == photoList.size

        if (isAllSelected) {
            // 如果已全选，则取消全选
            selectedPhotos.clear()
            Toast.makeText(requireContext(), "已取消全选", Toast.LENGTH_SHORT).show()
        } else {
            // 否则，执行全选
            selectedPhotos.clear()
            selectedPhotos.addAll(photoList)
            Toast.makeText(requireContext(), "已全选 ${photoList.size} 张照片", Toast.LENGTH_SHORT).show()
        }

        // 刷新 Adapter 和顶部 UI 状态
        photoAdapter.notifyDataSetChanged()
        updateTopBarUI()
        updateBottomActionUI() // 【关键】新增
    }

    // 【新增】控制底部栏显示/隐藏及动画
    private fun updateBottomActionUI() {
        // 只有在选择模式下 AND 选中数量大于 0 时才显示
        val show = isSelectionMode && selectedPhotos.isNotEmpty()

        if (show) {
            if (bottomActionBar.visibility != View.VISIBLE) {
                bottomActionBar.apply {
                    // 执行上滑动画
                    // 注意：由于 bottomActionBar 初始可能没有高度，需要先计算或使用已知高度
                    measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
                    val barHeight = measuredHeight.toFloat()

                    translationY = barHeight // 将底部栏推到屏幕外
                    visibility = View.VISIBLE
                    Log.d("PhotoFolderFragment", "显示按钮")
                    animate()
                        .translationY(0f) // 动画滑入到屏幕底部
                        .setDuration(300)
                        .start()
                }
            }

            // 选中数量更新文字 (可选)
            btnDelete.text = "删除 (${selectedPhotos.size})"
            btnMove.text = "移动 (${selectedPhotos.size})"

        } else {
            if (bottomActionBar.visibility == View.VISIBLE) {
                // 执行下滑动画
                // 确保动画知道高度
                val barHeight = bottomActionBar.height.toFloat()
                Log.d("PhotoFolderFragment", "隐藏按钮")
                bottomActionBar.animate()
                    .translationY(barHeight) // 动画下滑出屏幕
                    .setDuration(300)
                    .withEndAction {
                        bottomActionBar.visibility = View.GONE
                        bottomActionBar.translationY = 0f // 重置，确保下次动画能从正确位置开始
                    }
                    .start()
            }
        }
    }
    //删除照片确认函数

    private fun showDeleteConfirmationDialog() {
        if (selectedPhotos.isEmpty()) return

        val count = selectedPhotos.size

        // 【修改点】传入 R.style.RoundedAlertDialogStyle
        AlertDialog.Builder(requireContext(), R.style.RoundedAlertDialogStyle)
            .setTitle("确认删除")
            .setMessage("你确定要除这 $count 张照片吗？")
            .setPositiveButton("删除") { dialog, which ->
                deleteSelectedPhotos()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    //删除照片的执行函数
    private fun deleteSelectedPhotos() {
        if (selectedPhotos.isEmpty()) return

        val deletedCount = mutableListOf<File>()
        val failedCount = mutableListOf<File>()
        val context = requireContext()

        // 尝试删除文件
        selectedPhotos.forEach { file ->
            // 在 Android 10+ (API 29+) 上，对于非应用创建的文件，file.delete() 可能需要 MANAGE_EXTERNAL_STORAGE 权限，
            // 或者需要使用 MediaStore API 进行操作。这里使用基础的 File API。
            if (file.delete()) {
                deletedCount.add(file)
                // 提示：如果要保证文件删除后 MediaStore 立即更新，还需要调用 MediaStore API
            } else {
                failedCount.add(file)
            }
        }

        // 更新数据模型
        if (deletedCount.isNotEmpty()) {
            photoList.removeAll(deletedCount) // 从主列表中移除已删除的文件
            selectedPhotos.clear()           // 清空选中列表

            Toast.makeText(context, "成功删除 ${deletedCount.size} 张照片。", Toast.LENGTH_SHORT).show()
        }

        if (failedCount.isNotEmpty()) {
            Toast.makeText(context, "警告：${failedCount.size} 张照片删除失败，请检查应用权限。", Toast.LENGTH_LONG).show()
        }

        // 退出选择模式并刷新 UI (exitSelectionMode 会自动刷新 Adapter)
        exitSelectionMode()
    }
    // 【新增】统一管理顶部 UI 状态 (全选按钮的显示和图标)
    private fun updateTopBarUI() {
        // 定义颜色常量 (你可以根据你的 App 主题修改这些颜色值)
        val defaultTextColor = Color.parseColor("#333333") // 默认深灰色
        val highlightColor = Color.parseColor("#1A73E8") // 高亮蓝色

        if (isSelectionMode) {
            ivSelectAll.visibility = View.VISIBLE // 选择模式下显示全选按钮

            val isAllSelected = selectedPhotos.size == photoList.size

            // 1. 设置文本内容和高亮效果
            if (isAllSelected) {
                ivSelectAll.text = "取消全选" // 已全选时，提示用户“取消全选”
                ivSelectAll.setTextColor(highlightColor) // 高亮颜色
                // 可选：加粗显示
                ivSelectAll.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                ivSelectAll.text = "全选"
                ivSelectAll.setTextColor(defaultTextColor) // 默认颜色
                // 可选：取消加粗
                ivSelectAll.setTypeface(null, android.graphics.Typeface.NORMAL)
            }

            // 2. 切换模式按钮图标 (保持选中模式图标)
            ivSelect.setImageResource(R.drawable.ic_select)

        } else {
            ivSelectAll.visibility = View.GONE // 非选择模式下隐藏
            ivSelect.setImageResource(R.drawable.ic_unselect)
        }
    }
    // 切换选择模式逻辑
    private fun toggleSelectionMode() {
        isSelectionMode = !isSelectionMode

        if (isSelectionMode) {
            // 进入选择模式
            ivSelect.setImageResource(R.drawable.ic_select) // 变成“全选”或者“取消”图标，或者保持高亮状态
            Toast.makeText(requireContext(), "已进入选择模式", Toast.LENGTH_SHORT).show()
        } else {
            // 退出选择模式
            selectedPhotos.clear() // 清空已选
            //ivSelect.setImageResource(R.drawable.ic_unselect) // 变回原来的图标
        }
        // 【关键】刷新 UI
        updateTopBarUI() // 调用新增的方法来设置全选按钮的可见性和图标
        updateBottomActionUI() // 【关键】新增
        // 刷新整个列表以更新 UI 状态
        photoAdapter.notifyDataSetChanged()
    }

    // 强制退出选择模式
    private fun exitSelectionMode() {
        isSelectionMode = false
        selectedPhotos.clear()
        //ivSelect.setImageResource(R.drawable.ic_unselect)
        updateTopBarUI() // 调用新增的方法来设置全选按钮的可见性和图标
        updateBottomActionUI() // 【关键】新增
        photoAdapter.notifyDataSetChanged()
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

    // ---------------------------------------------------------
    // Inner Adapter Class (可以直接访问外部类的 isSelectionMode 和 selectedPhotos)
    // ---------------------------------------------------------
    inner class PhotoAdapter(
        private val photos: List<File>
    ) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

        inner class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imageView: ImageView = itemView.findViewById(R.id.iv_file_icon)
            // 新增的 View
            val maskView: View = itemView.findViewById(R.id.v_mask)
            val checkMark: ImageView = itemView.findViewById(R.id.iv_check_mark)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_photo_thumbnail, parent, false)
            return PhotoViewHolder(view)
        }

        override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
            val file = photos[position]

            // 1. 加载图片
            Glide.with(holder.imageView.context)
                .load(file)
                .centerCrop()
                .into(holder.imageView)

            // 2. 判断当前是否在选择模式，控制 UI 显示
            if (isSelectionMode) {
                // 如果是选中状态，显示遮罩和对勾
                val isSelected = selectedPhotos.contains(file)
                holder.maskView.visibility = if (isSelected) View.VISIBLE else View.GONE
                holder.checkMark.visibility = if (isSelected) View.VISIBLE else View.GONE

                // 可选：给未选中状态也加一个圆圈框（类似相册），目前先做选中才显示
                // 如果你想让未选中的也显示一个空圈，可以在 XML 里加一个默认显示的空圈 ImageView
            } else {
                // 普通模式：全部隐藏
                holder.maskView.visibility = View.GONE
                holder.checkMark.visibility = View.GONE
            }

            // 3. 点击事件处理
            holder.itemView.setOnClickListener {
                if (isSelectionMode) {
                    // --- 选择模式下：切换选中状态 ---
                    if (selectedPhotos.contains(file)) {
                        selectedPhotos.remove(file)
                    } else {
                        selectedPhotos.add(file)
                    }
                    updateTopBarUI()
                    updateBottomActionUI() // 【关键】新增
                    // 仅刷新当前 Item，性能更好
                    notifyItemChanged(position)

                    Log.d("PhotoSelection", "当前已选: ${selectedPhotos.size} 张")
                } else {
                    // --- 普通模式下：查看大图 ---
                    Toast.makeText(requireContext(), "查看图片: ${file.name}", Toast.LENGTH_SHORT).show()
                    // 这里可以跳转到大图预览 Fragment
                }
            }

            // 4. 长按事件（可选）：普通模式下长按也可以直接进入选择模式并选中当前项
            holder.itemView.setOnLongClickListener {
                if (!isSelectionMode) {
                    isSelectionMode = true
                    selectedPhotos.add(file)
                    ivSelect.setImageResource(R.drawable.ic_select) // 更新顶部按钮图标
                    notifyDataSetChanged() // 刷新所有以显示模式变化
                    return@setOnLongClickListener true
                }
                false
            }
        }

        override fun getItemCount(): Int = photos.size

    }
}
