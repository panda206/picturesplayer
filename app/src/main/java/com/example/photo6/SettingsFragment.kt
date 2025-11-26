package com.example.photo6

import android.content.Intent
import android.os.Bundle
import android.util.Log  // 引入 Log 类
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment() {

    private lateinit var systemTitle: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_settings, container, false)

        // 初始化系统设置标题
        systemTitle = rootView.findViewById(R.id.system_title)

        // 添加控制台打印信息，查看此方法是否被调用
        Log.d("SettingsFragment", "onCreateView called")  // 打印当前方法被调用

        // 点击系统设置项时跳转到 SystemInfoActivity
        systemTitle.setOnClickListener {
            Log.d("SettingsFragment", "System title clicked")  // 打印点击事件
            try {
                val intent = Intent(activity, SystemInfoActivity::class.java)
                startActivity(intent)
                Log.d("SettingsFragment", "Intent started")  // 打印启动 activity 成功
            } catch (e: Exception) {
                Log.e("SettingsFragment", "Error starting activity", e)  // 打印启动 activity 失败
            }
        }

        return rootView
    }
}
