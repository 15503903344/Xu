package com.surexu.sesame.ui.miuix

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.surexu.sesame.R
import com.surexu.sesame.util.Log
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 桌宠对话窗：悬浮窗点击或首页「和我说话」进入。
 * 全屏透明主题，半透明遮罩 + 纯白拟态浮起卡片，复用 PetEngine 打字控制配置。
 */
class PetChatActivity : MiuixBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAppContent {
            PetChatScreen(
                context = this,
                userId = restoreSelectedAccount(),
                onDismiss = { finish() }
            )
        }
    }

    override fun onStop() {
        super.onStop()
    }

    /** 读取最近选中的账号（与首页一致），无则用默认账号。 */
    private fun restoreSelectedAccount(): String? {
        return try {
            val prefs = getSharedPreferences("sesame_ui_state", MODE_PRIVATE)
            val last = prefs.getString("last_selected_user_id", null)
            if (last.isNullOrEmpty()) null else last
        } catch (t: Throwable) {
            Log.printStackTrace(t)
            null
        }
    }
}

/** 对话消息：text 内容，isUser 是否用户发出的。 */
private data class PetChatMsg(val text: String, val isUser: Boolean)

/** 对话窗配色：与纯白拟态主题一致（电光蓝主色 + 量子青辅色）。 */
private val PetPrimary = Color(0xFF2E6BFF)
private val PetCyan = Color(0xFF0FA5A0)
private val PetHairline = Color(0xFFE9EFF8)

@Composable
private fun PetChatScreen(
    context: android.content.Context,
    userId: String?,
    onDismiss: () -> Unit
) {
    val messages = remember {
        mutableStateListOf(
            PetChatMsg("我是大肥鱼，直接跟我说要改哪个设置，例如：把蚂蚁庄园的自动收能量打开", false)
        )
    }
    var input by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var dragOffsetX by remember { mutableStateOf(0f) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val dm = LocalContext.current.resources.displayMetrics

    // 思考中的小圆点呼吸动画
    val dotTransition = rememberInfiniteTransition(label = "pet_thinking_dots")
    val dotAlpha by dotTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pet_thinking_dot_alpha"
    )

    Box(
        Modifier
            .fillMaxSize()
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .neuRaised(shape = RoundedCornerShape(30.dp), elevation = 14.dp)
                .padding(vertical = 18.dp, horizontal = 18.dp)
                .heightIn(max = 620.dp)
        ) {
            // 顶栏：鲸鱼娘头像 + 名字 + 在线状态，右侧关闭
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                        .shadow(4.dp, CircleShape, spotColor = Neu.shadow, ambientColor = Neu.shadow)
                ) {
                    Image(
                        painter = painterResource(R.drawable.pet_idle),
                        contentDescription = "大肥鱼头像",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "大肥鱼",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiuixTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(6.dp))
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(PetCyan)
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "在线 · 说句话就能改设置",
                        fontSize = 11.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
                Text(
                    text = "×",
                    fontSize = 22.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onDismiss() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            // 顶栏与消息区之间的渐变分隔线
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(listOf(Color.Transparent, PetHairline, Color.Transparent))
                    )
            )
            Spacer(Modifier.height(8.dp))

            // 消息区
            LazyColumn(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 380.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(msg)
                }
                if (loading) {
                    item { ThinkingBubble(dotAlpha) }
                }
            }
            Spacer(Modifier.height(10.dp))

            // 输入区：凹陷输入槽 + 圆形渐变发送
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .weight(1f)
                        .neuPressed(shape = RoundedCornerShape(24.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    TextField(
                        value = input,
                        onValueChange = { input = it },
                        label = "",
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading
                    )
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(PetPrimary, PetCyan)))
                        .clickable(enabled = !loading) {
                            val text = input.trim()
                            if (text.isEmpty() || loading) return@clickable
                            input = ""
                            messages.add(PetChatMsg(text, true))
                            loading = true
                            Thread {
                                val reply = PetEngine.handle(context.applicationContext, text, userId)
                                mainHandler.post {
                                    messages.add(PetChatMsg(reply, false))
                                    loading = false
                                }
                            }.start()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "➤",
                        fontSize = 20.sp,
                        color = Color.White,
                        modifier = Modifier.alpha(if (loading) 0.4f else 1f)
                    )
                }
            }
        }
    }
}

/** 单条消息气泡：宠物带头像在左（白底拟态），用户渐变气泡在右。 */
@Composable
private fun ChatBubble(msg: PetChatMsg) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!msg.isUser) {
            Image(
                painter = painterResource(R.drawable.pet_idle),
                contentDescription = "大肥鱼",
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, Color.White, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = msg.text,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = if (msg.isUser) Color.White else MiuixTheme.colorScheme.onSurface,
            modifier = Modifier
                .widthIn(max = 260.dp)
                .clip(
                    if (msg.isUser) RoundedCornerShape(18.dp, 6.dp, 18.dp, 18.dp)
                    else RoundedCornerShape(6.dp, 18.dp, 18.dp, 18.dp)
                )
                .background(
                    if (msg.isUser) Brush.linearGradient(listOf(PetPrimary, PetCyan))
                    else Brush.linearGradient(listOf(Neu.base, Neu.pressed))
                )
                .border(
                    width = 1.dp,
                    brush = if (msg.isUser) Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                    else Brush.linearGradient(listOf(Neu.highlight, Neu.hairline)),
                    shape = if (msg.isUser) RoundedCornerShape(18.dp, 6.dp, 18.dp, 18.dp)
                    else RoundedCornerShape(6.dp, 18.dp, 18.dp, 18.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

/** 思考中的气泡：三点呼吸动画。 */
@Composable
private fun ThinkingBubble(dotAlpha: Float) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.pet_thinking),
            contentDescription = "大肥鱼思考中",
            modifier = Modifier
                .padding(top = 2.dp)
                .size(30.dp)
                .clip(CircleShape)
                .border(1.5.dp, Color.White, CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "正在思考",
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp, 18.dp, 18.dp, 18.dp))
                .background(Brush.linearGradient(listOf(Neu.base, Neu.pressed)))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
        Spacer(Modifier.width(2.dp))
        Row {
            repeat(3) { i ->
                Text(
                    text = "●",
                    fontSize = 6.sp,
                    color = PetCyan,
                    modifier = Modifier
                        .padding(horizontal = 1.5.dp)
                        .alpha(if (i == 0) dotAlpha else if (i == 1) dotAlpha * 0.7f else dotAlpha * 0.4f)
                )
            }
        }
    }
}
