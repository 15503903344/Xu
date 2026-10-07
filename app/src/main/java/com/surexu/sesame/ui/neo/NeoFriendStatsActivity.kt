package com.surexu.sesame.ui.neo

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.surexu.sesame.R
import com.surexu.sesame.entity.FriendWatch
import com.surexu.sesame.util.LanguageUtil

/**
 * 「服务」→「好友统计」二级页：展示单向好友列表（头像 + 昵称与能量统计）。
 * 数据源与模块版一致：FriendWatch.getList()。
 */
class NeoFriendStatsActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageUtil.setLocal(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.neo_page_friend_stats)
        window.statusBarColor = getColor(R.color.neo_base)
        window.navigationBarColor = getColor(R.color.neo_base)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        findViewById<View>(R.id.neo_friend_stats_back_btn).setOnClickListener { finish() }

        renderList()
    }

    private fun renderList() {
        val container = findViewById<LinearLayout>(R.id.neo_friend_stats_list)
        container.removeAllViews()
        val marginPx = dp(12)
        val friends = FriendWatch.getList()

        if (friends.isEmpty()) {
            val empty = TextView(this).apply {
                text = "(空)"
                textSize = 14f
                setTextColor(getColor(R.color.neo_text_hint))
                gravity = android.view.Gravity.CENTER
                setPadding(0, dp(32), 0, dp(32))
            }
            container.addView(empty)
            return
        }

        friends.forEach { fw ->
            val item = layoutInflater.inflate(R.layout.neo_item_friend_stats, container, false)
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, 0, 0, marginPx)
            item.layoutParams = lp

            item.findViewById<NeoAsyncAvatarView>(R.id.neo_friend_stats_avatar).load(fw.avatar)
            item.findViewById<TextView>(R.id.neo_friend_stats_name).text = fw.name
            container.addView(item)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
