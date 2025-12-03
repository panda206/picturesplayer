package com.example.photo6

import android.content.Context // 导入 Context
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.InputStreamReader // 用于读取文件
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class FileUploadServer(
    private val context: Context,
    private val serverPort: Int,
    private val uploadDir: File
) : NanoHTTPD(serverPort) {

    companion object {
        private const val TAG = "FileUploadServer"
    }

    init {
        if (!uploadDir.exists()) uploadDir.mkdirs()
    }

    override fun serve(session: IHTTPSession): Response {

        if (session.method == Method.GET) {
            val htmlTemplate = readAssetFile("upload_form.html")
            val html = htmlTemplate.replace("{{UPLOAD_DIR}}", uploadDir.absolutePath)
            return newFixedLengthResponse(Response.Status.OK, "text/html", html)
        }

        if (session.method == Method.POST) {
            try {
                val files = HashMap<String, String>()
                session.parseBody(files) // 这里仍然用于接收每块的临时文件路径

                // 获取客户端传来的分块信息
                val params = session.parameters
                val fileName = params["fileName"]?.firstOrNull() ?: "unknown"
                val chunkIndex = params["chunkIndex"]?.firstOrNull()?.toIntOrNull() ?: 0
                val totalChunks = params["totalChunks"]?.firstOrNull()?.toIntOrNull() ?: 1

                val tempFilePath = files["file"] ?: return newFixedLengthResponse(
                    Response.Status.BAD_REQUEST,
                    "text/plain",
                    "未找到上传文件"
                )

                val tempFile = File(tempFilePath)
                if (!tempFile.exists()) return newFixedLengthResponse(
                    Response.Status.BAD_REQUEST,
                    "text/plain",
                    "上传文件不存在"
                )

                // 临时文件夹存放分块
                val tempDir = File(uploadDir, "$fileName.temp")
                if (!tempDir.exists()) tempDir.mkdirs()

                val chunkFile = File(tempDir, "$chunkIndex.part")
                tempFile.copyTo(chunkFile, overwrite = true)
                tempFile.delete()

                // 检查是否所有块都上传完成
                val uploadedChunks = tempDir.listFiles()?.size ?: 0
                if (uploadedChunks == totalChunks) {
                    val finalFile = generateUniqueFile(fileName, uploadDir)
                    finalFile.outputStream().use { output ->
                        for (i in 0 until totalChunks) {
                            val partFile = File(tempDir, "$i.part")
                            partFile.inputStream().use { input ->
                                input.copyTo(output)
                            }
                        }
                    }
                    tempDir.deleteRecursively() // 删除临时分块
                    return newFixedLengthResponse(
                        Response.Status.OK,
                        "text/plain",
                        "文件上传完成: ${finalFile.name}"
                    )
                }

                return newFixedLengthResponse(
                    Response.Status.OK,
                    "text/plain",
                    "分块上传成功: $chunkIndex / $totalChunks"
                )

            } catch (e: Exception) {
                Log.e(TAG, "上传失败", e)
                return newFixedLengthResponse(
                    Response.Status.INTERNAL_ERROR,
                    "text/plain",
                    "上传失败: ${e.message}"
                )
            }
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404 Not Found")
    }

    private fun readAssetFile(fileName: String): String {
        return try {
            context.assets.open(fileName).use { input ->
                InputStreamReader(input).use { it.readText() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "读取文件失败: $fileName", e)
            "Error: Could not load HTML file."
        }
    }

    private fun generateUniqueFile(originalFileName: String, directory: File): File {
        val safeName = originalFileName.substringAfterLast("/").replace(Regex("[^0-9a-zA-Z._-]"), "_")
        val base = safeName.substringBeforeLast('.', safeName)
        val ext = safeName.substringAfterLast('.', "")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val newName = if (ext.isNotEmpty()) "${timestamp}_${base}.${ext}" else "${timestamp}_${base}"
        return File(directory, newName)
    }
}

