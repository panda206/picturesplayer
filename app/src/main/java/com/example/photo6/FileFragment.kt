package com.example.photo6

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import android.view.animation.TranslateAnimation
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import com.example.photo6.databinding.FragmentFileBinding
import com.example.photo6.GridSpacingItemDecoration

class FileFragment : Fragment() {

    private var _binding: FragmentFileBinding? = null
    private val binding get() = _binding!!

    private lateinit var btnDelete: Button
    private lateinit var fileRecyclerView: RecyclerView

    // 适配器数据源
    private val folderList = mutableListOf<File>()
    // 是否为选择模式 & 已选集合（MutableSet）
    private var isSelectionMode = false
    private val selectedFolders = mutableSetOf<File>()

    // 我们保留一个 adapter 引用，**只创建一次**，避免重复 new Adapter 导致状态不同步
    private lateinit var folderAdapter: FolderAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFileBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        btnDelete = view.findViewById(R.id.btn_delete)

        super.onViewCreated(view, savedInstanceState)

        // 1. RecyclerView 引用
        fileRecyclerView = view.findViewById(R.id.rv_files)

        // 2. 先准备数据（PhotoFile 下的文件夹）
        loadFolderList()

        // 3. 设置 LayoutManager（一定要先设置）

        val btnDelete = view.findViewById<View>(R.id.btn_delete)

        // --- 开始替换：动态计算列数 + 等距 ItemDecoration（跨手机/平板对称） ---
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
// 保持你之前的最小 item 宽度 dp （可根据需要调整）
        val minPhotoWidthPx = (133 * displayMetrics.density).toInt()

// 动态计算列数（至少 1 列）
        val spanCount = (screenWidth / minPhotoWidthPx).coerceAtLeast(1)

// 设置 GridLayoutManager
        fileRecyclerView.layoutManager = GridLayoutManager(requireContext(), spanCount)

// spacing（以 dp 为单位）
        val spacing = (12 * displayMetrics.density).toInt() // 你可以把 12 改成 8/10 等

// 给 RecyclerView 设置统一内边距（左右边距与 spacing 一致）
        fileRecyclerView.setPadding(spacing, spacing, spacing, spacing)
        fileRecyclerView.clipToPadding = false

// 先移除已存在的 ItemDecoration（如果你之前添加过），避免重复添加
        while (fileRecyclerView.itemDecorationCount > 0) {
            fileRecyclerView.removeItemDecorationAt(0)
        }
// 添加等距 ItemDecoration（下面会提供类 GridSpacingItemDecoration）
        fileRecyclerView.addItemDecoration(GridSpacingItemDecoration(spanCount, spacing))
// --- 替换结束 ---


        // 4. 创建 adapter（只创建一次），把 selectedFolders 的引用传进去
        folderAdapter = FolderAdapter(folderList, selectedFolders) { folder ->
            // 当 item 被点击时回调到这里，由 Fragment 决定行为（选择/打开）
            if (isSelectionMode) {
                // 切换选中状态
                if (selectedFolders.contains(folder)) selectedFolders.remove(folder)
                else selectedFolders.add(folder)

                // 打印当前已选（你之前需要看到的 Log）
                Log.d("FileFragmentLog", "当前已选择: ${selectedFolders.map { it.name }}")

                // 只刷新被点击项更高效（这里只用 notifyDataSetChanged 也可以）
                folderAdapter.notifyDataSetChanged()

                updateBottomActionUI()
            } else {
                // 普通模式：打开文件夹（保持你原有功能）
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, PhotoFolderFragment.newInstance(folder.absolutePath))
                    .addToBackStack(null)
                    .commit()
                Log.d("MSG", "点击了文件夹${folder.name}")
            }
        }

        fileRecyclerView.adapter = folderAdapter

        // 5. 选择按钮：只切换模式，不 new adapter
        val ivSelect = view.findViewById<ImageView>(R.id.iv_select)
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

            isSelectionMode = !isSelectionMode

            // 图标切换
            val newIcon = if (isSelectionMode) {
                R.drawable.ic_select  // 选择模式图标
            } else {
                R.drawable.ic_unselect // 普通模式图标
            }
            ivSelect.setImageResource(newIcon)
            // 如果进入选择模式，保持选中集合（也可以 clear() 视需求）
            if (!isSelectionMode) {
                // 退出选择模式时清除已选（如果你希望保留，则删除这行）
                selectedFolders.clear()
            }

            // 把状态传给 adapter 并刷新
            folderAdapter.isSelectionMode = isSelectionMode
            folderAdapter.notifyDataSetChanged()
            // 切换下方删除按钮
            updateBottomActionUI()
            val msg = if (isSelectionMode) "进入选择模式" else "退出选择模式"
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            Log.d("FileFragmentLog", msg)
        }
        //删除按钮逻辑功能
        btnDelete.setOnClickListener {
            if (selectedFolders.isEmpty()) return@setOnClickListener

            selectedFolders.forEach { folder ->
                folder.deleteRecursively()
                folderList.remove(folder)
            }

            selectedFolders.clear()

            folderAdapter.notifyDataSetChanged()
            updateBottomActionUI()

            Toast.makeText(requireContext(), "已删除文件夹", Toast.LENGTH_SHORT).show()
        }
        //新建文件夹按钮监听
        val ivNewFolder = view.findViewById<ImageView>(R.id.iv_new_folder)

        ivNewFolder.setOnClickListener {
            // 点击放大回弹动画
            ivNewFolder.animate()
                .scaleX(0.85f)
                .scaleY(0.85f)
                .setDuration(120)
                .withEndAction {
                    ivNewFolder.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(120)
                        .start()
                }
                .start()
            Log.d("FileFragmentLog", "新建文件夹")

            createNewFolder()

        }




    }
    //新建文件夹逻辑
    private fun createNewFolder() {
        val photoFileDir = File(requireContext().filesDir, "PhotoFile")
        if (!photoFileDir.exists()) photoFileDir.mkdir()

        var index = 1
        var newFolder: File
        do {
            val folderName = "新建文件夹$index"
            newFolder = File(photoFileDir, folderName)
            index++
        } while (newFolder.exists())

        if (newFolder.mkdir()) {
            folderList.add(newFolder)
            folderAdapter.notifyItemInserted(folderList.size - 1)
            fileRecyclerView.scrollToPosition(folderList.size - 1)

            // 让新建文件夹名称可编辑
            promptRename(newFolder)
        } else {
            Toast.makeText(requireContext(), "创建文件夹失败", Toast.LENGTH_SHORT).show()
        }
    }
    //文件夹重命名逻辑
    private fun promptRename(folder: File) {
        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.dialog_edit_folder, null)
        val editText = dialogView.findViewById<EditText>(R.id.et_folder_name)
        editText.setText(folder.name)
        editText.setSelection(folder.name.length)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("文件名")
            .setView(dialogView)
            .setPositiveButton("确定") { _, _ ->
                val folderName = editText.text.toString().trim()
                if (folderName.isNotEmpty()) {
                    renameFolder(folder, folderName)
                } else {
                    Toast.makeText(requireContext(), "文件夹名称不能为空", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .create()
        // 设置圆角背景
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)


        dialog.show()
    }
    //文件夹重命名逻辑
    private fun renameFolder(folder: File, newName: String) {
        val newFile = File(folder.parentFile, newName)
        if (!newFile.exists()) {
            folder.renameTo(newFile)
            loadFolderList()
            folderAdapter.notifyDataSetChanged()
        } else {
            Toast.makeText(requireContext(), "文件夹已存在", Toast.LENGTH_SHORT).show()
        }
    }




    //加载文件夹
    private fun loadFolderList() {
        val photoFileDir = File(requireContext().filesDir, "PhotoFile")
        if (!photoFileDir.exists()) photoFileDir.mkdir()
        folderList.clear()
        photoFileDir.listFiles()?.forEach {
            if (it.isDirectory) folderList.add(it)
        }
        Log.d("FileFragmentLog", "读取目录: ${photoFileDir.absolutePath}  共 ${folderList.size} 个文件夹")
    }
    /// 控制删除按钮显示 / 隐藏并加动画
    private fun updateBottomActionUI() {
        if (selectedFolders.isNotEmpty()) {
            if (btnDelete.visibility != View.VISIBLE) {
                btnDelete.apply {
                    // 重置状态
                    alpha = 0f
                    translationY = 100f * resources.displayMetrics.density
                    visibility = View.VISIBLE
                    animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(300)
                        .start()
                }
            }
        } else {
            if (btnDelete.visibility == View.VISIBLE) {
                btnDelete.animate()
                    .alpha(0f)
                    .translationY(100f * resources.displayMetrics.density) // 可选，往下滑
                    .setDuration(300)
                    .withEndAction {
                        btnDelete.visibility = View.GONE
                        // 重置状态，保证下一次显示动画能生效
                        btnDelete.alpha = 1f
                        btnDelete.translationY = 0f
                    }
                    .start()
            }
        }
    }




}
