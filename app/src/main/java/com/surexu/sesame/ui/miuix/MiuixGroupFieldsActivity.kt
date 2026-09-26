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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.CheckboxPreference
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
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
     * 先用 isModify() 判断是否有改动（无改动直接短路，不写盘、不提示），
     * 确认有改动后走 force=true，避免 ConfigV2.save() 内部再做一次全量序列化比较。
     */
    fun save() {
        // userId 为 null 表示「默认」账号：isModify/save 都会落到默认配置文件，
        // 不能直接 return，否则默认账号下改完配置退出等于没保存，且没有任何提示。
        if (!ConfigV2.isModify(userId)) return
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
 * 扁平化后的列表行：把「模型标题」和「字段」都提升为 LazyColumn 的独立 item，
 * 让虚拟化真正下沉到字段级。
 *
 * 原先每个 ModelConfig 是一个 item、内部用 fields.forEach 组合全部字段，
 * 导致 Forest 组（77 个字段）一旦进入视口就要一次性组合、measure、layout 所有字段。
 */
private sealed interface GroupFieldsRow {
    val key: String

    data class Header(override val key: String, val title: String) : GroupFieldsRow

    data class Field(
        override val key: String,
        val modelCode: String,
        val field: ModelField<*>,
        val first: Boolean,
        val last: Boolean
    ) : GroupFieldsRow
}

@Composable
fun GroupFieldsContent(activity: MiuixGroupFieldsActivity, userId: String?, groupCode: String, group: ModelGroup) {
    // 父字段开关/选项变化后，依赖其显示的子字段需重新计算可见性，
    // 用 depVersion 作为 remember 键触发扁平行列表重建。
    var depVersion by remember { mutableStateOf(0) }
    // 字段对象由 ConfigV2 单例持有，引用稳定；仅当分组或依赖版本变化时才重建。
    val rows = remember(group, depVersion) {
        val list = ArrayList<GroupFieldsRow>()
        Model.getGroupModelConfig(group).values.forEach { mc ->
            val fields = mc.fields.values.toList()
            if (fields.isEmpty()) return@forEach
            list.add(GroupFieldsRow.Header(key = "header:${mc.getCode()}", title = mc.name ?: ""))
            // 过滤：依赖父字段但父未激活的子字段
            val visibleFields = fields.filter { f ->
                f.isVisible(mc)
            }
            visibleFields.forEachIndexed { index, field ->
                list.add(
                    GroupFieldsRow.Field(
                        key = "field:${mc.getCode()}:${field.code}",
                        modelCode = mc.getCode(),
                        field = field,
                        first = index == 0,
                        last = index == visibleFields.lastIndex
                    )
                )
            }
        }
        list
    }

    Scaffold(
        topBar = {
            LogTopBar(
                title = group.getName(),
                onBack = { activity.saveAndFinish() }
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(rows, key = { it.key }) { row ->
                when (row) {
                    is GroupFieldsRow.Header -> SmallTitle(text = row.title)
                    is GroupFieldsRow.Field -> GroupFieldRow(
                        activity = activity,
                        userId = userId,
                        groupCode = groupCode,
                        row = row,
                        onDependencyChanged = { depVersion++ }
                    )
                }
            }
        }
    }
}

/**
 * 单个字段行：每个字段独立一张纯白拟态卡片（一个功能一张卡片）。
 * 展开的编辑区与字段行同卡展示，LazyColumn 仍可逐字段复用/回收。
 */
@Composable
private fun GroupFieldRow(
    activity: MiuixGroupFieldsActivity,
    userId: String?,
    groupCode: String,
    row: GroupFieldsRow.Field,
    onDependencyChanged: () -> Unit
) {
    val field = row.field
    val isSelect = field.type in listOf("SELECT", "SELECT_ONE", "SELECT_AND_COUNT", "SELECT_AND_COUNT_ONE")
    ItemCard(verticalPadding = if (isSelect) 8.dp else 5.dp) {
        if (isSelect) {
            ArrowPreference(
                title = field.name ?: "",
                onClick = {
                    activity.startActivity(
                        Intent(activity, MiuixSelectionEditActivity::class.java).apply {
                            putExtra(MiuixGroupFieldsActivity.EXTRA_USER_ID, userId)
                            putExtra(MiuixGroupFieldsActivity.EXTRA_GROUP_CODE, groupCode)
                            putExtra(MiuixSelectionEditActivity.EXTRA_FIELD_CODE, field.code)
                            putExtra(MiuixSelectionEditActivity.EXTRA_MODEL_CODE, row.modelCode)
                        }
                    )
                }
            )
        } else {
            // 只写内存，落盘统一在 saveAndFinish() / onBackPressed() 完成
            FieldItem(field = field, onFieldChanged = onDependencyChanged)
        }
    }
}


