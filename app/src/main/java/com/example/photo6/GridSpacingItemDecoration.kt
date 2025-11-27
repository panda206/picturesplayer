package com.example.photo6

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Grid 等距装饰器：保证左右间距对称（即使列数变化也能保持两边与列间间距一致）
 *
 * @param spanCount 列数
 * @param spacing   间距 px（整型）
 */
class GridSpacingItemDecoration(
    private val spanCount: Int,
    private val spacing: Int
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view) // item 位置
        if (position == RecyclerView.NO_POSITION) {
            // 防御性判断
            outRect.set(0, 0, 0, 0)
            return
        }
        val column = position % spanCount

        // 计算左右：保持左右边距与内部间距等分对齐
        outRect.left = spacing - column * spacing / spanCount
        outRect.right = (column + 1) * spacing / spanCount

        // top spacing：第一行需要上间距，其他行只有 bottom/ top 可选
        if (position < spanCount) {
            outRect.top = spacing
        } else {
            outRect.top = 0
        }
        outRect.bottom = spacing
    }
}
