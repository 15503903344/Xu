package com.surexu.sesame.ui.miuix

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.surexu.sesame.R
import com.surexu.sesame.data.ConfigV2
import com.surexu.sesame.data.Model
import com.surexu.sesame.data.ModelConfig
import com.surexu.sesame.data.modelFieldExt.SelectOneModelField
import com.surexu.sesame.model.normal.answerAI.CustomAI
import com.surexu.sesame.util.Log
import com.surexu.sesame.util.StringUtil
import org.json.JSONArray
import org.json.JSONObject
import top.yukonga.miuix.kmp.basic.Text
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 首页桌宠展示位：一言下方 Q 版鲸鱼娘（同悬浮窗形态），会自己上下浮动 + 轻微摇摆，
 * 支持拖动移动位置；点击进入对话窗。控制入口（开启悬浮窗 / 放到桌面）在首页「设置-系统设置」。
 *
 * @param running 悬浮窗是否运行中：为 true 时首页不再绘制鲸鱼娘，避免与桌面悬浮窗重复。
 */
@Composable
fun PetHomeImage(running: Boolean = false) {
    if (running) return
    val context = LocalContext.current
    val transition = rememberInfiniteTransition(label = "pet_home")
    val floatY by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pet_float_y"
    )
    val sway by transition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pet_sway"
    )
    var dragOffset by remember { mutableStateOf(IntOffset.Zero) }

    Column(
        Modifier
            .fillMaxWidth()
            .offset { IntOffset(0, floatY.roundToInt()) + dragOffset }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { context.startActivity(Intent(context, PetChatActivity::class.java)) })
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    dragOffset += IntOffset(dragAmount.x.roundToInt(), dragAmount.y.roundToInt())
                }
            }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.pet_idle),
            contentDescription = "Q版鲸鱼娘桌宠",
            modifier = Modifier
                .size(132.dp)
                .graphicsLayer { rotationZ = sway }
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "鲸鱼娘桌宠 · 拖动我 · 点击和我说话",
            fontSize = 12.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
    }
}

/** 跳系统悬浮窗授权页。 */
internal fun openOverlaySettings(context: Context) {
    try {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:" + context.packageName)
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (t: Throwable) {
        Log.printStackTrace(t)
    }
}

/** 创建桌面快捷方式：点击图标直接拉起悬浮窗桌宠。 */
internal fun addToLauncher(context: Context) {
    try {
        val launcherIntent = Intent(context, PetLauncherActivity::class.java)
            .setAction(Intent.ACTION_MAIN)
        val shortcut = ShortcutInfoCompat.Builder(context, "pet_float")
            .setShortLabel("大肥鱼")
            .setLongLabel("大肥鱼桌宠")
            .setIcon(IconCompat.createWithBitmap(loadFishIcon(context)))
            .setIntent(launcherIntent)
            .build()
        if (ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)) {
            // 系统弹「已添加」提示，无需额外文案
        } else {
            // 部分桌面不支持 pin 快捷方式：退回发送 INSTALL_SHORTCUT 广播
            sendInstallShortcutBroadcast(context)
        }
    } catch (t: Throwable) {
        Log.printStackTrace(t)
    }
}

/** 兜底方案：传统 INSTALL_SHORTCUT 广播（老桌面兼容）。 */
private fun sendInstallShortcutBroadcast(context: Context) {
    val intent = Intent("com.android.launcher.action.INSTALL_SHORTCUT")
    intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "大肥鱼桌宠")
    intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, loadFishIcon(context))
    val launch = Intent(context, PetLauncherActivity::class.java)
        .setAction(Intent.ACTION_MAIN)
    intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launch)
    intent.putExtra("duplicate", false)
    context.sendBroadcast(intent)
}

/** 把鱼素材转成桌面图标 Bitmap。 */
private fun loadFishIcon(context: Context): Bitmap {
    val drawable = context.resources.getDrawable(R.drawable.pet_fish, null)
    val size = (96 * context.resources.displayMetrics.density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, size, size)
    drawable.draw(canvas)
    return bitmap
}

/**
 * 桌宠引擎：组装字段字典 -> 调自定义 AI -> 解析 JSON -> 写配置 -> 保存并广播重启。
 * 悬浮窗对话（PetChatActivity）与首页入口共用。
 */
object PetEngine {

    /**
     * 处理一条用户指令：子线程调用，返回要展示给用户的结果文本。
     */
    fun handle(context: Context, instruction: String, userId: String?): String {
        val ai = buildCustomAI()
        if (ai == null) {
            return "还没有配置 AI 连接，请先到「配置-其他-自定义AI」填写接口地址、模型名、令牌再试。"
        }
        val prompt = buildPrompt(buildFieldDict(), instruction)
        val raw = ai.getAnswerStr(prompt)
        if (raw.isNullOrBlank()) {
            return "AI 请求失败，请检查「自定义AI」配置是否可用。"
        }
        return try {
            val obj = JSONObject(raw)
            if (obj.has("err")) {
                "肥肥没听懂：" + obj.optString("err")
            } else {
                applyCommand(context, obj, userId)
            }
        } catch (e: Exception) {
            Log.err("PetEngine", "LLM 返回非 JSON:" + raw, e)
            "肥肥回答格式不对，请换种说法再试。"
        }
    }

    /** 从 AnswerAI 配置项构造 CustomAI；未填齐返回 null。 */
    private fun buildCustomAI(): CustomAI? {
        val fields = ConfigV2.INSTANCE.getModelFields("AnswerAI") ?: return null
        val url = (fields["customAIUrl"]?.value as? String).orEmpty()
        val model = (fields["customAIModel"]?.value as? String).orEmpty()
        val key = (fields["customAIKey"]?.value as? String).orEmpty()
        val maxTokens = (fields["customAIMaxTokens"]?.value as? Int) ?: 1024
        val ai = CustomAI(url, model, key, maxTokens)
        return if (ai.isConfigured()) ai else null
    }

    /** 生成全量字段字典 JSON 数组字符串。 */
    private fun buildFieldDict(): String {
        val arr = JSONArray()
        val configs = Model.getModelConfigMap()
        for (mc in configs.values) {
            val modelCode = mc.code
            for (f in mc.fields.values) {
                val type = f.type
                val obj = JSONObject()
                obj.put("m", modelCode)
                obj.put("f", f.code)
                obj.put("n", f.name)
                obj.put("t", type)
                obj.put("v", f.value?.toString() ?: "")
                if (f is SelectOneModelField) {
                    val opts = JSONArray()
                    val list = f.expandValue ?: emptyList()
                    for (item in list) {
                        opts.put(item.id + "|" + item.name)
                    }
                    obj.put("o", opts)
                }
                arr.put(obj)
            }
        }
        return arr.toString()
    }

    private fun buildPrompt(dict: String, instruction: String): String {
        return "你是Sure-Xu模块的桌宠大肥鱼「肥肥」。用户会用中文让你修改模块配置。\n" +
                "模块配置项字典（m=模块,f=字段,n=名称,t=类型,v=当前值,o=选项id|名称）：\n" +
                dict + "\n\n" +
                "用户指令：" + instruction + "\n\n" +
                "规则：\n" +
                "1. 把指令映射到唯一配置项，只输出一个 JSON 对象：{\"m\":\"模块\",\"f\":\"字段\",\"v\":值}\n" +
                "2. 类型对应：BOOLEAN→true/false；INTEGER/MULTIPLY_INTEGER→整数；STRING/TEXT/READ_TEXT/URL_TEXT→字符串；SELECT_ONE→选项id（从 o 中选）\n" +
                "3. 类型是 SELECT/SELECT_AND_COUNT/SELECT_AND_COUNT_ONE/CHOICE/LIST/EMPTY 的字段不要改，输出 {\"err\":\"简短中文说明\"}\n" +
                "4. 指令模糊、找不到对应项或拿不准时输出 {\"err\":\"简短中文说明\"}\n" +
                "只输出 JSON，不要任何多余文字。"
    }

    /** 执行配置修改并保存广播。 */
    private fun applyCommand(context: Context, obj: JSONObject, userId: String?): String {
        val modelCode = obj.optString("m")
        val fieldCode = obj.optString("f")
        if (!obj.has("v")) {
            return "AI 没给出要改成的值，请再试一次。"
        }
        val mc: ModelConfig = Model.getModelConfigMap()[modelCode] ?: return "找不到模块：$modelCode"
        val field = mc.getModelField(fieldCode) ?: return "找不到字段：$modelCode.$fieldCode"

        val newValue: Any? = when (field.type) {
            "BOOLEAN" -> obj.optBoolean("v")
            "INTEGER", "MULTIPLY_INTEGER" -> obj.optInt("v")
            "STRING", "TEXT", "READ_TEXT", "URL_TEXT" -> obj.optString("v")
            "SELECT_ONE" -> obj.optString("v")
            else -> return "字段「${field.name}」类型 ${field.type} 暂不支持语音修改。"
        }
        val oldValue = field.configValue
        field.setObjectValue(newValue)
        if (!ConfigV2.isModify(userId)) {
            return "没有检测到配置变化，请换一种说法。"
        }
        if (!ConfigV2.save(userId, true)) {
            return "配置保存失败，请稍后再试。"
        }
        sendRestart(context, userId)
        return "已修改「${field.name}」：$oldValue -> ${field.configValue}，稍等生效。"
    }

    private fun sendRestart(context: Context, userId: String?) {
        try {
            val intent = Intent("com.eg.android.AlipayGphone.sesame.restart")
            if (!StringUtil.isEmpty(userId)) {
                intent.putExtra("userId", userId)
            }
            context.sendBroadcast(intent)
        } catch (th: Throwable) {
            Log.printStackTrace(th)
        }
    }
}
