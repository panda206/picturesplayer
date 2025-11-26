package com.example.photo6

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)

        // 默认加载首页
        if (savedInstanceState == null) {
            openFragment(HomeFragment())
        }

        // 设置底部导航栏点击事件
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    openFragment(HomeFragment())  // 点击首页时加载 HomeFragment
                    true
                }
                R.id.nav_search -> {
                    openFragment(FileFragment())  // 点击文件时加载 FileFragment
                    true
                }
                R.id.nav_profile -> {
                    openFragment(SettingsFragment())  // 点击设置时加载 SettingsFragment
                    true
                }
                else -> false
            }
        }
    }

    // 切换 Fragment 的方法
    private fun openFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)  // fragment_container 是 Fragment 容器的 ID
            .commit()
    }
}
