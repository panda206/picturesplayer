package com.example.photo6

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import fi.iki.elonen.NanoHTTPD // 导入 NanoHTTPD 类
import java.io.File
import java.io.FileOutputStream
import java.io.IOException // 导入 IOException 类，用于服务器启动错误处理
import android.util.Log

class SystemInfoActivity : AppCompatActivity() {

    // --- 文件夹路径 ---
    private lateinit var photoFileFolder: File
    private lateinit var playPicturesFolder: File

    // --- 原有功能常量：用于本地相册选择 ---
    private val REQUEST_CODE_PICK_IMAGE = 101

    // --- 新增功能变量：服务器控制 ---
    private val SERVER_PORT = 8000
    private var fileUploadServer: FileUploadServer? = null
    private var isServerRunning = false

    // --- 视图变量 ---
    private lateinit var ipAddressTextView: TextView
    private lateinit var customInfoText: TextView
    private lateinit var uploadButton: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_system_info)

        // --- 视图初始化 ---
        ipAddressTextView = findViewById(R.id.ip_address)
        customInfoText = findViewById(R.id.custom_info_text)
        uploadButton = findViewById(R.id.upload_button)
        val backButton: ImageButton = findViewById(R.id.back_button)

        // --- 按钮和事件设置 ---

        // 1. 返回按钮：点击返回上一级，并确保停止服务器
        backButton.setOnClickListener {
            stopServer()
            onBackPressed()
        }

        // 2. 上传按钮：保留原有功能，打开系统相册进行本地上传
        uploadButton.setOnClickListener { openGallery() }

        // 3. customInfoText：新增功能，点击启动或停止网络服务器
        updateServerDisplay(getDeviceIpAddress()) // 初始化 IP 地址和服务器状态显示
        customInfoText.setOnClickListener {
            if (isServerRunning) {
                stopServer()
            } else {
                startServer()
            }
        }

        // --- 文件夹路径初始化 ---
        photoFileFolder = File(filesDir, "PhotoFile")
        playPicturesFolder = File(photoFileFolder, "Play pictures")

        Toast.makeText(this, "Play pictures 文件夹已准备好", Toast.LENGTH_SHORT).show()
    }

    /** Activity 销毁时，确保停止服务器 */
    override fun onDestroy() {
        super.onDestroy()
        stopServer()
    }

    // ----------------------------------------------------------------------
    // --- 新增功能：网络服务器控制逻辑 ---
    // ----------------------------------------------------------------------

    /** 更新 IP 地址显示和服务器状态 */
    private fun updateServerDisplay(ip: String) {
        ipAddressTextView.text = "设备 IP 地址: $ip"
        if (isServerRunning) {
            customInfoText.text = "服务运行中: $ip:$SERVER_PORT (点击停止)"
        } else {
            customInfoText.text = "局域网内访问 $ip:$SERVER_PORT 上传文件 (点击启动)"
        }
    }

    /** 启动服务器 */
    private fun startServer() {
        if (fileUploadServer == null) {
            // 确保上传目录存在
            if (!playPicturesFolder.exists()) {
                playPicturesFolder.mkdirs()
            }

            try {
                // 实例化并启动服务器
                // 🌟 关键修改：传入 this (即 Context), 然后是 SERVER_PORT, 最后是 playPicturesFolder
                fileUploadServer = FileUploadServer(this, SERVER_PORT, playPicturesFolder)
                fileUploadServer?.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)

                isServerRunning = true

                // 更新 UI
                updateServerDisplay(getDeviceIpAddress())
                Toast.makeText(this, "文件上传服务已启动", Toast.LENGTH_SHORT).show()

            } catch (e: IOException) {
                Log.e("SystemInfoActivity", "Could not start server", e)
                isServerRunning = false
                Toast.makeText(this, "端口 $SERVER_PORT 被占用或权限不足", Toast.LENGTH_LONG).show()
                updateServerDisplay(getDeviceIpAddress())
            }
        }
    }

    /** 停止服务器 */
    private fun stopServer() {
        fileUploadServer?.stop()
        fileUploadServer = null
        isServerRunning = false
        updateServerDisplay(getDeviceIpAddress())
        Toast.makeText(this, "文件上传服务已停止", Toast.LENGTH_SHORT).show()
    }

    // ----------------------------------------------------------------------
    // --- 原有功能：本地相册上传及辅助函数 (全部保留) ---
    // ----------------------------------------------------------------------

    /** 获取设备 IP 地址 */
    private fun getDeviceIpAddress(): String {
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        val wifiInfo: WifiInfo = wifiManager.connectionInfo
        val ipAddress = wifiInfo.ipAddress
        return String.format(
            "%d.%d.%d.%d",
            ipAddress and 0xFF,
            ipAddress shr 8 and 0xFF,
            ipAddress shr 16 and 0xFF,
            ipAddress shr 24 and 0xFF
        )
    }

    /** 打开系统相册（多选） */
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                // 单选图片
                data.data?.let { saveImageToAppFolder(it) }

                // 多选图片
                data.clipData?.let { clipData ->
                    for (i in 0 until clipData.itemCount) {
                        saveImageToAppFolder(clipData.getItemAt(i).uri)
                    }
                }
            }
        }
    }

    /** 保存图片到 Play pictures 文件夹，保留原文件名，处理同名 */
    private fun saveImageToAppFolder(uri: Uri) {
        try {
            // 获取原始文件名
            var fileName = "uploaded_image.jpg"
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) {
                    fileName = cursor.getString(nameIndex)
                }
            }

            // 生成唯一文件
            val outputFile = generateUniqueFile(fileName, playPicturesFolder)

            val inputStream = contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(outputFile)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()

            Toast.makeText(this, "${outputFile.name} 上传成功", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "上传失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /** 生成不重复文件，放在指定文件夹 */
    private fun generateUniqueFile(fileName: String, folder: File): File {
        var newFile = File(folder, fileName)
        if (!newFile.exists()) return newFile

        val dotIndex = fileName.lastIndexOf('.')
        val name = if (dotIndex != -1) fileName.substring(0, dotIndex) else fileName
        val ext = if (dotIndex != -1) fileName.substring(dotIndex) else ""

        var index = 1
        while (newFile.exists()) {
            newFile = File(folder, "$name($index)$ext")
            index++
        }
        return newFile
    }
}