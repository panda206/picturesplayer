package com.example.photo6

import android.app.Activity
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
import java.io.File
import java.io.FileOutputStream

class SystemInfoActivity : AppCompatActivity() {

    private lateinit var photoFileFolder: File
    private lateinit var playPicturesFolder: File

    private val REQUEST_CODE_PICK_IMAGE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_system_info)

        // 显示设备 IP 地址
        val ipAddressTextView: TextView = findViewById(R.id.ip_address)
        val ipAddress = getDeviceIpAddress()
        ipAddressTextView.text = "设备 IP 地址: $ipAddress"

        val customInfoText: TextView = findViewById(R.id.custom_info_text)
        customInfoText.text = "局域网内访问 $ipAddress:8000 上传文件"

        // 返回按钮
        val backButton: ImageButton = findViewById(R.id.back_button)
        backButton.setOnClickListener { onBackPressed() }

        // 上传按钮
        val uploadButton: Button = findViewById(R.id.upload_button)
        uploadButton.setOnClickListener { openGallery() }

        // 创建 PhotoFile 文件夹
        photoFileFolder = File(filesDir, "PhotoFile")
        if (!photoFileFolder.exists()) photoFileFolder.mkdirs()

        // 在 PhotoFile 内创建 Play pictures 文件夹
        playPicturesFolder = File(photoFileFolder, "Play pictures")
        if (!playPicturesFolder.exists()) playPicturesFolder.mkdirs()

        Toast.makeText(this, "Play pictures 文件夹已准备好", Toast.LENGTH_SHORT).show()
    }

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
