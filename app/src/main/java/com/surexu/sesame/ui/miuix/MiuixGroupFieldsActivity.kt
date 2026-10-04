package com.surexu.sesame.ui.miuix

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.surexu.sesame.data.AppConfig
import com.surexu.sesame.data.ConfigPreload
import com.surexu.sesame.data.ConfigV2
import com.surexu.sesame.data.Model
import com.surexu.sesame.data.ModelConfig
import com.surexu.sesame.data.ModelField
import com.surexu.sesame.data.ModelGroup
import com.surexu.sesame.data.modelFieldExt.ChoiceModelField
import com.surexu.sesame.data.modelFieldExt.EmptyModelField
import com.surexu.sesame.data.modelFieldExt.IntegerModelField
import com.surexu.sesame.data.modelFieldExt.SelectAndCountModelField
import com.surexu.sesame.data.modelFieldExt.SelectAndCountOneModelField
import com.surexu.sesame.data.modelFieldExt.SelectModelField
import com.surexu.sesame.data.modelFieldExt.SelectOneModelField
import com.surexu.sesame.util.Log
import com.surexu.sesame.util.ToastUtil
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

/**
 * 配置字段页（三级）：显示某个分组下的所有配置字段。
 * 从 MiuixSettingsActivity 跳转进来，通过 Intent 传递 userId 和 groupCode。
 */
class MiuixGroupFieldsActivity : MiuixBaseActivity() {

    companion object {
        const val EXTRA_USER_ID = "userId"
        const val EXTRA_GROUP_CODE = "groupCode"
    }

    private var userId: String? = null
    internal var groupCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userId = intent.getStringExtra(EXTRA_USER_ID)
        groupCode = intent.getStringExtra(EXTRA_GROUP_CODE)
        // 本页会被底部配置 Tab 的分组列表直接拉起，必须自行完成「注册模型 + 预加载账号配置」，
        // 否则 Model.getGroupModelConfig(group) 返回空表 → 只有标题、正文全空白；
        // 且 ConfigV2 未按 userId 加载时字段值取的是上一个账号，退出保存会把错值写进本账号配置。
        // 这两步原先由已删除的旧齿轮入口页 MiuixSettingsActivity.onCreate 承担。
        Model.initAllModel()
        ConfigPreload.prepare(userId)
        setAppContent {
            groupCode?.let { code ->
                val group = ModelGroup.entries.find { it.name == code }
                if (group != null) {
                    GroupFieldsContent(activity = this, userId = userId, groupCode = code, group = group)
                } else {
                    top.yukonga.miuix.kmp.basic.Text("分组不存在: $code", color = MiuixTheme.colorScheme.error)
                }
            } ?: run {
                top.yukonga.miuix.kmp.basic.Text("缺少参数", color = MiuixTheme.colorScheme.error)
            }
        }
    }

    override fun onBackPressed() {
        save()
        super.onBackPressed()
    }

    /** 顶部返回按钮与系统返回统一入口：先保存再退出。 */
    fun saveAndFinish() {
        save()
        finish()
    }

    /**
     * 统一落盘入口：本页字段变更只写内存，只有真正退出时才调用这里写一次磁盘。
     * 是否提示/写盘只看「本进程字段级改动」（hasFieldChanges 基于页面打开时的快照），
     * 不能依赖 isModify()：它比较的是整份序列化文本，磁盘里任何格式差异都会判成"有改动"，
     * 导致没动任何开关返回也弹"保存成功"并给支付宝发重启广播（用户反馈的"返回即保存成功直接运行"）。
     */
    fun save() {
        if (!ConfigV2.hasFieldChanges()) return
        if (ConfigV2.save(userId, true)) {
            ToastUtil.show(this, "保存成功！")
            sendRestartIfNeeded()
        }
    }

    /** 保存成功后通知支付宝主进程重启模块，让新配置立即生效（与设置页同款）。 */
    private fun sendRestartIfNeeded() {
        // userId 为 null 表示「默认」账号，也要发广播（不带 extra 即可命中当前进程），
        // 否则默认账号下改配置保存后支付宝进程不重载，仍然不能即时生效。
        try {
            val intent = Intent("com.eg.android.AlipayGphone.sesame.restart")
            if (userId != null) {
                intent.putExtra("userId", userId)
            }
            sendBroadcast(intent)
        } catch (th: Throwable) {
            Log.printStackTrace(th)
        }
    }
}

/**
 * 字段页行类型：一个模型 = 一组字段卡（每字段一张独立卡片）；BASE 组的全局开关独立成小卡。
 */
private sealed interface GroupFieldsRow {
    val key: String

    data class Model(
        override val key: String,
        val mc: ModelConfig,
        val fields: List<ModelField<*>>
    ) : GroupFieldsRow

    data class AppConfigSwitch(
        override val key: String,
        val title: String
    ) : GroupFieldsRow
}

private val SELECT_TYPES = setOf("SELECT", "SELECT_ONE", "SELECT_AND_COUNT", "SELECT_AND_COUNT_ONE")

@Composable
fun GroupFieldsContent(activity: MiuixGroupFieldsActivity, userId: String?, groupCode: String, group: ModelGroup) {
    // 父字段开关/选项变化后，依赖其显示的子字段需重新计算可见性
    var depVersion by remember { mutableStateOf(0) }
    val rows = remember(group, depVersion) {
        val list = ArrayList<GroupFieldsRow>()
        Model.getGroupModelConfig(group).values.forEach { mc ->
            val fields = mc.fields.values.toList()
            if (fields.isEmpty()) return@forEach
            val visibleFields = fields.filter { f -> f.isVisible(mc) }
            if (visibleFields.isEmpty()) return@forEach
            list.add(GroupFieldsRow.Model(key = "model:${mc.getCode()}", mc = mc, fields = visibleFields))
            // 「开启状态栏禁删」「屏蔽部分弹窗」等全局开关跟随 BASE 组 debugMode 展示
            if (groupCode == "BASE" && visibleFields.any { it.code == "debugMode" }) {
                list.add(GroupFieldsRow.AppConfigSwitch(key = "appcfg:enableOnGoing", title = "开启状态栏禁删"))
                list.add(GroupFieldsRow.AppConfigSwitch(key = "appcfg:closeCaptchaDialog", title = "屏蔽部分弹窗"))
                list.add(GroupFieldsRow.AppConfigSwitch(key = "appcfg:showToast", title = "气泡提示"))
                list.add(GroupFieldsRow.AppConfigSwitch(key = "appcfg:toastOffsetY", title = "气泡纵向偏移"))
            }
        }
        list
    }

    Scaffold(
        topBar = {
            LogTopBar(
                title = group.getName(),
                onBack = { activity.saveAndFinish() },
                onExecute = {
                    try {
                        val intent = Intent("com.eg.android.AlipayGphone.sesame.execute")
                        intent.putExtra("group", group.getCode())
                        activity.sendBroadcast(intent)
                        ToastUtil.show(activity, "已发送执行请求：" + group.getName())
                    } catch (th: Throwable) {
                        Log.printStackTrace(th)
                        ToastUtil.show(activity, "执行失败: " + th.message)
                    }
                }
            )
        },
        containerColor = MiuixTheme.colorScheme.surface
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(rows, key = { it.key }) { row ->
                when (row) {
                    is GroupFieldsRow.Model -> ModelSection(
                        activity = activity,
                        userId = userId,
                        groupCode = groupCode,
                        mc = row.mc,
                        fields = row.fields,
                        onDependencyChanged = { depVersion++ }
                    )
                    is GroupFieldsRow.AppConfigSwitch -> AppConfigSwitchCard(activity = activity, row = row)
                }
            }
        }
    }
}

/** 模型区块：标题行（模型名 + 已开徽标，非卡片）+ 每字段一张独立卡片（一卡一功能）。 */
@Composable
private fun ModelSection(
    activity: MiuixGroupFieldsActivity,
    userId: String?,
    groupCode: String,
    mc: ModelConfig,
    fields: List<ModelField<*>>,
    onDependencyChanged: () -> Unit
) {
    val switches = fields.filter { it.type == "BOOLEAN" }
    val enabled = switches.count { (it.getValue() as? Boolean) == true }
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 模型区块标题（非卡片）：模型名 + 已开徽标
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = mc.name ?: "",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MiuixTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            SxBadge(text = "已开 $enabled/${switches.size}", tint = MiuixTheme.colorScheme.primary)
        }
        fields.forEach { field ->
            if (field.type in SELECT_TYPES) {
                ItemCard {
                    SxSelectRow(
                        name = field.name ?: "",
                        summary = null,
                        onClick = {
                            activity.startActivity(
                                Intent(activity, MiuixSelectionEditActivity::class.java).apply {
                                    putExtra(MiuixGroupFieldsActivity.EXTRA_USER_ID, userId)
                                    putExtra(MiuixGroupFieldsActivity.EXTRA_GROUP_CODE, groupCode)
                                    putExtra(MiuixSelectionEditActivity.EXTRA_FIELD_CODE, field.code)
                                    putExtra(MiuixSelectionEditActivity.EXTRA_MODEL_CODE, mc.getCode())
                                }
                            )
                        }
                    )
                }
            } else {
                // 一卡一功能：每字段一张独立卡片；内部内边距由 FieldItem/SxSettingRow 自持
                ItemCard(horizontalPadding = 0.dp, verticalPadding = 0.dp) {
                    FieldItem(field = field, onFieldChanged = onDependencyChanged)
                }
            }
        }
    }
}

/**
 * AppConfig 全局配置开关小卡（非 ConfigV2 字段）：
 * 逻辑读 AppConfig.INSTANCE，开关直接读写 AppConfig 并即时落盘 + 广播重载。
 */
@Composable
private fun AppConfigSwitchCard(activity: MiuixGroupFieldsActivity, row: GroupFieldsRow.AppConfigSwitch) {
    ItemCard(verticalPadding = 5.dp) {
        when (row.key) {
            "appcfg:enableOnGoing" -> {
                var checked by remember { mutableStateOf(AppConfig.INSTANCE.enableOnGoing ?: false) }
                SxSettingRow(
                    title = row.title,
                    trailing = {
                        SxSwitch(
                            checked = checked,
                            onCheckedChange = {
                                checked = it
                                AppConfig.INSTANCE.enableOnGoing = it
                                AppConfig.save()
                                activity.sendBroadcast(Intent("com.eg.android.AlipayGphone.sesame.reloadConfig"))
                            }
                        )
                    }
                )
            }
            "appcfg:closeCaptchaDialog" -> {
                var checked by remember { mutableStateOf(AppConfig.INSTANCE.closeCaptchaDialog ?: true) }
                SxSettingRow(
                    title = row.title,
                    trailing = {
                        SxSwitch(
                            checked = checked,
                            onCheckedChange = {
                                checked = it
                                AppConfig.INSTANCE.closeCaptchaDialog = it
                                AppConfig.save()
                                activity.sendBroadcast(Intent("com.eg.android.AlipayGphone.sesame.reloadConfig"))
                            }
                        )
                    }
                )
            }
            "appcfg:showToast" -> {
                var checked by remember { mutableStateOf(AppConfig.INSTANCE.showToast ?: true) }
                SxSettingRow(
                    title = row.title,
                    trailing = {
                        SxSwitch(
                            checked = checked,
                            onCheckedChange = {
                                checked = it
                                AppConfig.INSTANCE.showToast = it
                                AppConfig.save()
                                activity.sendBroadcast(Intent("com.eg.android.AlipayGphone.sesame.reloadConfig"))
                            }
                        )
                    }
                )
            }
            "appcfg:toastOffsetY" -> {
                var text by remember { mutableStateOf((AppConfig.INSTANCE.toastOffsetY ?: 0).toString()) }
                var expanded by remember { mutableStateOf(false) }
                Column {
                    SxSettingRow(
                        title = row.title,
                        summary = if (text.isEmpty()) "0 px（正数向下）" else "$text px（正数向下）",
                        onClick = { expanded = !expanded }
                    )
                    if (expanded) {
                        SxTextField(
                            value = text,
                            onValueChange = { newText ->
                                val filtered = newText.filterIndexed { index, c -> c.isDigit() || (c == '-' && index == 0) }
                                text = filtered
                                filtered.toIntOrNull()?.let { value ->
                                    AppConfig.INSTANCE.toastOffsetY = value
                                    AppConfig.save()
                                    activity.sendBroadcast(Intent("com.eg.android.AlipayGphone.sesame.reloadConfig"))
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}


