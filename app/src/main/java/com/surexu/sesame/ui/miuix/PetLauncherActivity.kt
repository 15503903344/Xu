package com.surexu.sesame.ui.miuix

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import com.surexu.sesame.util.Log

/**
 * 桌面快捷方式中转：用户点击桌面上的「大肥鱼」图标时进入。
 * - 已有悬浮窗权限 → 直接拉起悬浮窗服务并立即关闭本页（无感）
 * - 无权限 → 跳系统悬浮窗授权页，授权后用户再次点击图标即可
 */
class PetLauncherActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            if (PetFloatService.canOverlay(this)) {
                PetFloatService.start(this)
                finish()
                return
            }
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:$packageName")
            )
            startActivity(intent)
            finish()
        } catch (t: Throwable) {
            Log.printStackTrace(t)
            finish()
        }
    }
}
