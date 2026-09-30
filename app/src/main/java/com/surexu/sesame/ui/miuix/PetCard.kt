package com.surexu.sesame.ui.miuix

import android.content.Context
import android.content.Intent
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
import com.surexu.sesame.R
import com.surexu.sesame.data.ConfigV2
import com.surexu.sesame.data.Model
import com.surexu.sesame.data.ModelConfig
import com.surexu.sesame.data.modelFieldExt.SelectOneModelField
import com.surexu.sesame.model.normal.answerAI.CustomAI
import com.surexu.sesame.util.FileUtil
import com.surexu.sesame.util.Log
import com.surexu.sesame.util.Statistics
import com.surexu.sesame.util.StringUtil
import org.json.JSONArray
import org.json.JSONObject
import top.yukonga.miuix.kmp.basic.Text
import java.io.File
import java.io.RandomAccessFile
import java.util.Calendar
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 首页桌宠展示位：一言下方 Q 版鲸鱼娘，会自己上下浮动 + 轻微摇摆，
 * 支持拖动移动位置；点击进入对话窗。
 */
@Composable
fun PetHomeImage() {
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

/**
 * 桌宠引擎：组装字段字典 -> 调自定义 AI -> 解析 JSON -> 写配置 -> 保存并广播重启。
 * 首页对话窗（PetChatActivity）入口共用。
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
        val prompt = buildPrompt(buildFieldDict(), buildTodaySummary(context), instruction)
        val raw = ai.getAnswerStr(prompt)
        if (raw.isNullOrBlank()) {
            return "AI 请求失败，请检查「自定义AI」配置是否可用。"
        }
        val clean = stripCodeFence(raw)
        return try {
            val obj = JSONObject(clean)
            when {
                obj.has("err") -> "肥肥没听懂：" + obj.optString("err")
                obj.has("m") && obj.has("f") -> applyCommand(context, obj, userId)
                // JSON 但既非指令也非 err：按文本展示兜底
                else -> clean
            }
        } catch (e: Exception) {
            // 非 JSON：视为普通闲聊/建议回复，直接展示
            clean
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

    private fun buildPrompt(dict: String, summary: String, instruction: String): String {
        return "你是Sure-Xu模块的桌宠大肥鱼「肥肥」，性格活泼可爱，用中文回复用户。\n" +
                "模块配置项字典（m=模块,f=字段,n=名称,t=类型,v=当前值,o=选项id|名称）：\n" +
                dict + "\n\n" +
                "今日运行摘要（用户问今天做了哪些任务、收了多少能量时，结合它回答）：\n" +
                summary + "\n\n" +
                "用户指令：" + instruction + "\n\n" +
                "输出规则（三选一，只输出一个结果，不要多余文字）：\n" +
                "1. 如果用户是在要求修改某个配置项：只输出一个 JSON 对象 {\"m\":\"模块\",\"f\":\"字段\",\"v\":值}\n" +
                "   类型对应：BOOLEAN→true/false；INTEGER/MULTIPLY_INTEGER→整数；STRING/TEXT/READ_TEXT/URL_TEXT→字符串；SELECT_ONE→选项id（从 o 中选）\n" +
                "2. 如果用户想改的是 SELECT/SELECT_AND_COUNT/SELECT_AND_COUNT_ONE/CHOICE/LIST/EMPTY 类型字段，或指令模糊找不到对应项：输出 {\"err\":\"简短中文说明\"}\n" +
                "3. 其他情况（闲聊、问候、问设置建议、问今日任务/能量统计、问模块功能等）：直接用中文自然语言回答，不要输出 JSON；\n" +
                "   回答今日任务/能量时结合今日运行摘要总结，数据缺失就如实说明。"
    }

    /** 组装今日运行摘要：能量统计 + 今日运行日志尾部（供 LLM 回答总结类问题）。 */
    private fun buildTodaySummary(context: Context): String {
        val sb = StringBuilder()
        try {
            Statistics.load()
            Statistics.updateDay(Calendar.getInstance())
            sb.append("【能量统计·今日】收 ")
                .append(Statistics.getData(Statistics.TimeType.DAY, Statistics.DataType.COLLECTED))
                .append("，帮收 ")
                .append(Statistics.getData(Statistics.TimeType.DAY, Statistics.DataType.HELPED))
                .append("，浇水 ")
                .append(Statistics.getData(Statistics.TimeType.DAY, Statistics.DataType.WATERED))
                .append("，被浇 ")
                .append(Statistics.getData(Statistics.TimeType.DAY, Statistics.DataType.WATEREDCOUNT))
                .append("，浇树 ")
                .append(Statistics.getData(Statistics.TimeType.DAY, Statistics.DataType.WATERINGCOUNT))
                .append(" 次\n")
        } catch (t: Throwable) {
            Log.printStackTrace(t)
            sb.append("【能量统计·今日】暂不可用\n")
        }
        try {
            val logTail = readLogTail(FileUtil.getRuntimeLogFile(), 256L * 1024L)
            val lines = logTail.lineSequence().toList().takeLast(120)
            if (lines.isNotEmpty()) {
                sb.append("【今日运行日志·末尾摘要】\n")
                lines.forEach { sb.append(it).append('\n') }
            } else {
                sb.append("【今日运行日志】暂无记录\n")
            }
        } catch (t: Throwable) {
            Log.printStackTrace(t)
            sb.append("【今日运行日志】读取失败\n")
        }
        return sb.toString()
    }

    /** 从文件尾部读取文本（避免大日志全量加载），实现与日志查看器一致。 */
    private fun readLogTail(file: File, maxBytes: Long): String {
        val length = file.length()
        if (length <= 0L) return ""
        val start = maxOf(0L, length - maxBytes)
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(start)
            val bytes = ByteArray((length - start).toInt())
            raf.readFully(bytes)
            var text = String(bytes, Charsets.UTF_8)
            if (start > 0L) {
                val idx = text.indexOf('\n')
                text = if (idx >= 0) text.substring(idx + 1) else ""
            }
            return text
        }
    }

    /** 去掉 LLM 回复外层可能的 ```json ``` 代码块围栏。 */
    private fun stripCodeFence(raw: String): String {
        val trimmed = raw.trim()
        val fence = Regex("^```(?:json|JSON)?\\s*\\n?|\\n?```\\s*$")
        return fence.replace(trimmed, "").trim()
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
