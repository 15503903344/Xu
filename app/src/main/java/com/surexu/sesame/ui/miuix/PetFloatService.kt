package com.surexu.sesame.ui.miuix

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import com.surexu.sesame.R
import com.surexu.sesame.util.Log
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 桌宠悬浮窗服务：Q 版鲸鱼娘挂到系统桌面上层。
 *
 * 行为：
 * - 多表情状态：闲置/思考/等待/睡觉/眨眼/生气/庆祝，按场景自动切换
 * - 一直自己动：上下浮动 + 左右摇摆 + 呼吸缩放（闲置动画，无限循环）
 * - 可拖动：长按移动位置；闲置 60 秒自动睡觉，触摸即醒
 * - 点击：眨眼反馈后弹出对话窗（PetChatActivity），复用 PetEngine 打字控制配置
 * - 通知栏常驻，可一键关闭桌宠
 */
class PetFloatService : Service() {

    companion object {
        private const val TAG = "PetFloatService"
        private const val CHANNEL_ID = "pet_float"
        private const val NOTIFICATION_ID = 0x5045
        private const val FLOAT_SIZE_DP = 92
        private const val PREFS_PET = "sesame_pet_state"
        private const val KEY_RUNNING = "float_running"

        const val STATE_IDLE = 0
        const val STATE_THINKING = 1
        const val STATE_WAITING = 2
        const val STATE_SLEEP = 3
        const val STATE_ANGRY = 4
        const val STATE_CELEBRATE = 5
        const val STATE_WINK = 6

        @Volatile
        private var runningService: PetFloatService? = null

        /** 是否用户主动关闭（主动关闭时不自动重启）。 */
        private var stoppedByUser = false

        /** 启动悬浮窗桌宠；无悬浮窗权限时返回 false 且不启动。 */
        @JvmStatic
        fun start(context: Context): Boolean {
            if (!Settings.canDrawOverlays(context)) return false
            val intent = Intent(context, PetFloatService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            return true
        }

        /** 是否已运行。 */
        @JvmStatic
        fun isRunning(context: Context): Boolean {
            return context.getSharedPreferences(PREFS_PET, Context.MODE_PRIVATE)
                .getBoolean(KEY_RUNNING, false)
        }

        /** 是否已授予悬浮窗权限。 */
        @JvmStatic
        fun canOverlay(context: Context): Boolean = Settings.canDrawOverlays(context)

        /** 切换桌宠表情状态（同进程直接生效）。 */
        @JvmStatic
        fun setState(context: Context, state: Int) {
            runningService?.applyState(state)
        }
    }

    private lateinit var windowManager: WindowManager
    private var floatView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var floatAnimator: android.animation.ValueAnimator? = null
    private var swayAnimator: android.animation.ValueAnimator? = null
    private var breathAnimator: android.animation.ValueAnimator? = null
    private var tempRunnable: Runnable? = null
    private var sleepRunnable: Runnable? = null

    private var currentState = STATE_IDLE

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        runningService = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "ACTION_STOP_PET") {
            stoppedByUser = true
            stopSelf()
            return START_NOT_STICKY
        }
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildNotification())
        if (floatView == null) {
            addFloatView()
        }
        return START_STICKY
    }

    @SuppressLint("InflateParams")
    private fun addFloatView() {
        val sizePx = (FLOAT_SIZE_DP * resources.displayMetrics.density).toInt()

        val imageView = ImageView(this)
        imageView.setImageResource(R.drawable.pet_idle)
        imageView.contentDescription = "大肥鱼桌宠"
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = (resources.displayMetrics.heightPixels * 0.22f).toInt()

        imageView.setOnTouchListener(FloatTouchListener(params))
        try {
            windowManager.addView(imageView, params)
            floatView = imageView
            layoutParams = params
            currentState = STATE_IDLE
            startAnimations(imageView)
            setRunning(true)
            resetIdleTimer()
            Log.i(TAG, "桌宠悬浮窗已添加")
        } catch (t: Throwable) {
            Log.printStackTrace(t)
            setRunning(false)
            stopSelf()
        }
    }

    /** 表情状态映射到素材图。 */
    private fun stateRes(state: Int): Int {
        return when (state) {
            STATE_THINKING -> R.drawable.pet_thinking
            STATE_WAITING -> R.drawable.pet_waiting
            STATE_SLEEP -> R.drawable.pet_sleep
            STATE_ANGRY -> R.drawable.pet_angry
            STATE_CELEBRATE -> R.drawable.pet_celebrate
            STATE_WINK -> R.drawable.pet_wink
            else -> R.drawable.pet_idle
        }
    }

    /** 切换表情状态：换图并匹配对应动画。 */
    private fun applyState(state: Int) {
        val view = floatView ?: return
        if (state == currentState) return
        val old = currentState
        currentState = state
        tempRunnable?.let { mainHandler.removeCallbacks(it) }
        tempRunnable = null

        mainHandler.post {
            if (floatView == null) return@post
            val iv = view as? ImageView ?: return@post
            iv.setImageResource(stateRes(state))
            when (state) {
                STATE_IDLE -> {
                    resetIdleTimer()
                    startAnimations(view)
                }
                STATE_THINKING, STATE_WAITING -> {
                    stopSleepTimer()
                    stopAnimations()
                    val a = android.animation.ObjectAnimator.ofFloat(view, "rotation", -6f, 6f, -6f)
                    a.duration = 900L
                    a.repeatCount = android.animation.ValueAnimator.INFINITE
                    a.repeatMode = android.animation.ValueAnimator.REVERSE
                    a.start()
                    swayAnimator = a
                }
                STATE_SLEEP -> {
                    stopAnimations()
                    val b = android.animation.ObjectAnimator.ofFloat(view, "scaleX", 0.96f, 1.0f)
                    b.duration = 2000L
                    b.repeatCount = android.animation.ValueAnimator.INFINITE
                    b.repeatMode = android.animation.ValueAnimator.REVERSE
                    b.start()
                    breathAnimator = b
                }
                STATE_ANGRY, STATE_CELEBRATE, STATE_WINK -> {
                    stopAnimations()
                    bounce(view)
                    tempRunnable = Runnable {
                        if (currentState == state) {
                            applyState(STATE_IDLE)
                        }
                    }
                    val delay = if (state == STATE_ANGRY) 1100L else if (state == STATE_WINK) 500L else 700L
                    mainHandler.postDelayed(tempRunnable!!, delay)
                }
            }
            if (old == STATE_SLEEP && state != STATE_SLEEP) {
                // 唤醒后保持浮动动画
            }
        }
    }

    /** 闲置动画：上下浮动 + 左右摇摆 + 呼吸缩放，无限循环，形成"自己动"的效果。 */
    private fun startAnimations(view: View) {
        stopAnimations()
        view.rotation = 0f
        view.translationY = 0f
        view.scaleX = 1f
        view.scaleY = 1f

        val floatAnimator = android.animation.ObjectAnimator.ofFloat(view, "translationY", 0f, -18f, 0f)
        floatAnimator.duration = 1600L
        floatAnimator.repeatCount = android.animation.ValueAnimator.INFINITE
        floatAnimator.repeatMode = android.animation.ValueAnimator.RESTART
        floatAnimator.start()
        this.floatAnimator = floatAnimator

        val swayAnimator = android.animation.ObjectAnimator.ofFloat(view, "rotation", 0f, 10f, -10f, 0f)
        swayAnimator.duration = 1400L
        swayAnimator.repeatCount = android.animation.ValueAnimator.INFINITE
        swayAnimator.repeatMode = android.animation.ValueAnimator.RESTART
        swayAnimator.start()
        this.swayAnimator = swayAnimator

        val breath = android.animation.ObjectAnimator.ofFloat(view, "scaleX", 0.96f, 1.0f)
        breath.duration = 2200L
        breath.repeatCount = android.animation.ValueAnimator.INFINITE
        breath.repeatMode = android.animation.ValueAnimator.REVERSE
        breath.start()
        this.breathAnimator = breath
    }

    /** 单次弹跳反馈（点击/庆祝/生气）。 */
    private fun bounce(view: View) {
        val sx = android.animation.ObjectAnimator.ofFloat(view, "scaleX", 1.0f, 1.15f, 1.0f)
        val sy = android.animation.ObjectAnimator.ofFloat(view, "scaleY", 1.0f, 1.15f, 1.0f)
        sx.duration = 240L
        sy.duration = 240L
        sx.start()
        sy.start()
    }

    /** 停止全部动画（触摸拖动时暂停）。 */
    private fun stopAnimations() {
        floatAnimator?.cancel()
        swayAnimator?.cancel()
        breathAnimator?.cancel()
        floatAnimator = null
        swayAnimator = null
        breathAnimator = null
    }

    /** 闲置计时：60 秒无互动自动睡觉。 */
    private fun resetIdleTimer() {
        stopSleepTimer()
        sleepRunnable = Runnable {
            if (currentState == STATE_IDLE) {
                applyState(STATE_SLEEP)
            }
        }
        mainHandler.postDelayed(sleepRunnable!!, 60_000L)
    }

    private fun stopSleepTimer() {
        sleepRunnable?.let { mainHandler.removeCallbacks(it) }
        sleepRunnable = null
    }

    /** 拖动 + 点击手势：位移小视为点击（眨眼并弹对话窗），位移大视为拖动。 */
    private inner class FloatTouchListener(private val params: WindowManager.LayoutParams) :
        View.OnTouchListener {

        private var downRawX = 0f
        private var downRawY = 0f
        private var downX = 0
        private var downY = 0
        private var moved = false
        private var downTime = 0L

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    // 动画偏移补偿：把当前 translationY 并入窗口位置，归零后再拖动，避免视觉跳动
                    params.y = params.y + v.translationY.toInt()
                    v.translationY = 0f
                    downX = params.x
                    downY = params.y
                    moved = false
                    downTime = System.currentTimeMillis()
                    stopAnimations()
                    if (currentState == STATE_SLEEP) {
                        applyState(STATE_IDLE)
                        startAnimations(v)
                    }
                    resetIdleTimer()
                    return true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    if (abs(dx) > 16f || abs(dy) > 16f) {
                        moved = true
                    }
                    if (moved) {
                        params.x = downX + dx.toInt()
                        params.y = downY + dy.toInt()
                        keepInScreen(params)
                        try {
                            windowManager.updateViewLayout(v, params)
                        } catch (t: Throwable) {
                            // 更新布局偶发异常不影响拖动，忽略
                        }
                    }
                    return true
                }

                MotionEvent.ACTION_UP -> {
                    val isTap = !moved && (System.currentTimeMillis() - downTime) < 600L
                    if (isTap) {
                        v.performClick()
                        applyState(STATE_WINK)
                        mainHandler.postDelayed({ openChat() }, 220L)
                    } else {
                        // 拖动结束恢复闲置动画
                        mainHandler.post { startAnimations(v) }
                    }
                    return true
                }
            }
            return false
        }
    }

    /** 打开对话窗（新 Activity，透明主题）。 */
    private fun openChat() {
        try {
            val intent = Intent(this, PetChatActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        } catch (t: Throwable) {
            Log.printStackTrace(t)
        }
    }

    /** 把悬浮窗限制在屏幕可视区域内。 */
    private fun keepInScreen(params: WindowManager.LayoutParams) {
        val dm = resources.displayMetrics
        val w = params.width
        val h = params.height
        params.x = max(0, min(params.x, dm.widthPixels - w))
        params.y = max(0, min(params.y, dm.heightPixels - h))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "桌宠悬浮窗",
                NotificationManager.IMPORTANCE_MIN
            )
            channel.description = "大肥鱼桌宠运行中"
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, PetFloatService::class.java).setAction("ACTION_STOP_PET")
        val stopPi = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openIntent = Intent(this, MiuixMainActivity::class.java)
        val openPi = PendingIntent.getActivity(
            this,
            1,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("大肥鱼桌宠运行中")
            .setContentText("点击鱼身聊天，长按拖动位置")
            .setContentIntent(openPi)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .addAction(0, "关闭桌宠", stopPi)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        runningService = null
        stopAnimations()
        stopSleepTimer()
        tempRunnable?.let { mainHandler.removeCallbacks(it) }
        tempRunnable = null
        floatView?.let { v ->
            try {
                windowManager.removeView(v)
            } catch (t: Throwable) {
                // 视图已移除则忽略
            }
        }
        floatView = null
        setRunning(false)
        Log.i(TAG, "桌宠悬浮窗已关闭")
    }

    private fun setRunning(running: Boolean) {
        getSharedPreferences(PREFS_PET, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_RUNNING, running)
            .apply()
    }
}
