package com.surexu.sesame.ui.miuix

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.surexu.sesame.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 对话消息：isUser = 老板发的，text = 内容 */
private data class PetChatMsg(val isUser: Boolean, val text: String)

/**
 * 首页桌宠展示位：一言下方 Q 版鲸鱼娘，会自己上下浮动 + 轻微摇摆，
 * 支持拖动移动位置；点击弹出对话窗（输入框 + 本地关键词应答）。
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
    var showTalk by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .offset { IntOffset(0, floatY.roundToInt()) + dragOffset }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    dragOffset += IntOffset(dragAmount.x.roundToInt(), dragAmount.y.roundToInt())
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { showTalk = true })
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

    if (showTalk) {
        PetChatDialog(onDismiss = { showTalk = false })
    }
}

/** 鲸鱼娘对话窗：消息列表 + 输入框 + 发送，本地关键词应答，可连续对话。 */
@Composable
private fun PetChatDialog(onDismiss: () -> Unit) {
    val messages = remember { mutableStateListOf<PetChatMsg>() }
    var input by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Box(
            Modifier
                .fillMaxWidth()
                .neuRaised(RoundedCornerShape(24.dp), 10.dp)
                .padding(20.dp)
        ) {
            Column {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "鲸鱼娘",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MiuixTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(text = "关闭", onClick = onDismiss)
                }
                Spacer(Modifier.height(10.dp))
                if (messages.isEmpty()) {
                    Text(
                        text = "老板好呀，我是鲸鱼娘～想让我跑点什么任务？比如「森林自动收能量」「农场喂鸡」～",
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                } else {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        messages.forEach { msg ->
                            ChatBubble(msg)
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SxTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f)
                    )
                    SxIconButton(
                        imageVector = Icons.Filled.Send,
                        contentDescription = "发送",
                        onClick = {
                            val text = input.trim()
                            if (text.isEmpty()) return@SxIconButton
                            messages.add(PetChatMsg(isUser = true, text = text))
                            input = ""
                            // 简短停顿模拟"思考"，再回复
                            scope.launch {
                                delay(400)
                                messages.add(PetChatMsg(isUser = false, text = petReply(text)))
                            }
                        }
                    )
                }
            }
        }
    }
}

/** 单条对话气泡：老板消息靠右高亮、鲸鱼娘消息靠左。 */
@Composable
private fun ChatBubble(msg: PetChatMsg) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            Modifier
                .neuRaised(
                    RoundedCornerShape(if (msg.isUser) 14.dp else 14.dp),
                    2.dp
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = msg.text,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = if (msg.isUser) MiuixTheme.colorScheme.primary
                    else MiuixTheme.colorScheme.onBackground
            )
        }
    }
}

/** 本地关键词应答：覆盖森林/农场/庄园/问候等，兜底引导。 */
private fun petReply(input: String): String = when {
    input.contains("森林") || input.contains("能量") || input.contains("收") || input.contains("偷") ->
        "森林的活我盯着呢～自动收能量、帮好友、浇水这些开关都在「配置-森林」里，想改哪项直接说！"
    input.contains("农场") || input.contains("小鸡") || input.contains("鸡") || input.contains("饲料") ->
        "农场的鸡我来喂！自动喂小鸡、收蛋、换饲料都在「配置-农场」里，随时可以喊我改～"
    input.contains("庄园") || input.contains("树") || input.contains("种") ->
        "庄园的树种上就等着收果子啦～自动收果子的开关在「配置-庄园」里，要调整跟我说！"
    input.contains("金豆") || input.contains("豆") ->
        "金豆记录我看过了，每天的产出都在日志里躺着，需要我汇总给你吗？"
    input.contains("你好") || input.contains("在吗") || input.contains("嗨") || input.contains("哈喽") ->
        "老板好呀～我是鲸鱼娘，今天想让我跑点什么任务？"
    input.contains("谢谢") || input.contains("辛苦") ->
        "不辛苦，为老板服务是我的荣幸～喝口茶歇一歇！"
    input.contains("配置") || input.contains("设置") || input.contains("开关") ->
        "设置页的所有开关和选项都能管，比如「把森林自动收能量打开」「农场每天喂几次鸡」，直接说就行～"
    input.contains("日志") ->
        "日志在底部导航第二个「日志」页，森林、金豆、农场、运行日志都能看，还能复制导出～"
    else ->
        "收到！这个我先记下来～目前我能陪你聊森林、农场、庄园的任务，或者帮你看配置开关，换个说法试试？"
}
