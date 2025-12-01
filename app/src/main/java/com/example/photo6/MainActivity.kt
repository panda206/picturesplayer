package com.example.photo6

import android.os.Bundle
import android.view.View // 导入 View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView // 导入 FragmentContainerView
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        // 【关键】获取 Fragment 容器的引用
        val fragmentContainer: FragmentContainerView = findViewById(R.id.fragment_container)

        // 【核心修正】在 BottomNavigationView 布局完成后，获取其高度并设置给 Fragment 容器
        bottomNavigationView.post {
            val bottomNavHeight = bottomNavigationView.height

            // 获取 Fragment 容器当前的 Padding
            val currentPaddingLeft = fragmentContainer.paddingLeft
            val currentPaddingTop = fragmentContainer.paddingTop
            val currentPaddingRight = fragmentContainer.paddingRight

            // 设置新的底部 Padding，其高度等于底部导航栏的高度
            fragmentContainer.setPadding(
                currentPaddingLeft,
                currentPaddingTop,
                currentPaddingRight,
                bottomNavHeight
            )
        }

        // 默认加载首页
        if (savedInstanceState == null) {
            openFragment(HomeFragment())
        }

        // 设置底部导航栏点击事件
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    openFragment(HomeFragment())
                    true
                }
                R.id.nav_search -> {
                    openFragment(FileFragment())
                    true
                }
                R.id.nav_profile -> {
                    openFragment(SettingsFragment())
                    true
                }
                else -> false
            }
        }
    }

    // 切换 Fragment 的方法 (保持不变)
    private fun openFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}