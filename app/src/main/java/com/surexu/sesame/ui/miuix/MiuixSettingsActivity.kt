package com.surexu.sesame.ui.miuix

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.surexu.sesame.data.ConfigPreload
import com.surexu.sesame.data.ConfigV2
import com.surexu.sesame.data.Model
import com.surexu.sesame.data.ModelField
import com.surexu.sesame.data.ModelGroup
import com.surexu.sesame.data.modelFieldExt.ChoiceModelField
import com.surexu.sesame.data.modelFieldExt.EmptyModelField
import com.surexu.sesame.data.modelFieldExt.IntegerModelField
import com.surexu.sesame.data.modelFieldExt.SelectAndCountModelField
import com.surexu.sesame.data.modelFieldExt.SelectAndCountOneModelField
import com.surexu.sesame.data.modelFieldExt.SelectModelField
import com.surexu.sesame.data.modelFieldExt.SelectOneModelField
import com.surexu.sesame.entity.IdAndName
import com.surexu.sesame.entity.KVNode
import com.surexu.sesame.entity.MemberBenefit
import com.surexu.sesame.util.Log
import com.surexu.sesame.util.StringUtil
import com.surexu.sesame.util.ToastUtil
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.BasicComponentColors
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

class MiuixSettingsActivity : MiuixBaseActivity() {

    companion object {
        const val EXTRA_USER_ID = "userId"
    }

    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userId = intent.getStringExtra(EXTRA_USER_ID)
        Model.initAllModel()
        ConfigPreload.prepare(userId)
        setAppContent {
            SettingsContent(this, userId)
        }
    }

    override fun onBackPressed() {
        save()
        super.onBackPressed()
    }

    /** 顶部返回按钮与系统返回统一入口：先保存再退出（与三级/四级保持一致）。 */
    fun saveAndFinish() {
        save()
        finish()
    }

    /**
     * 统一落盘入口（二级/三级/四级同款实现）：
     * 先用 isModify() 短路「无改动」的情况，确认有改动后走 force=true，
     * 避免 ConfigV2.save() 内部再重复做一次全量序列化比较。
     */
    fun save() {
        if (!ConfigV2.isModify(userId)) return
        if (ConfigV2.save(userId, true)) {
            ToastUtil.show(this, "保存成功！")
            sendRestartIfNeeded()
        }
    }

    private fun sendRestartIfNeeded() {
        // userId 为 null 表示「默认」账号，也要发广播（不带 extra 即可命中当前进程），
        // 否则默认账号下改配置保存后支付宝进程不重载，仍然不能即时生效。
        try {
            val intent = Intent("com.eg.android.AlipayGphone.sesame.restart")
            if (!StringUtil.isEmpty(userId)) {
                intent.putExtra("userId", userId)
            }
            sendBroadcast(intent)
        } catch (th: Throwable) {
            Log.printStackTrace(th)
        }
    }
}

@Composable
fun SettingsContent(activity: MiuixSettingsActivity, userId: String?) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        if (uri != null) {
            val file = ConfigPreload.getConfigFile(userId)
            try {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    file.inputStream().use { it.copyTo(os) }
                }
                ToastUtil.show(context, "导出成功！")
            } catch (e: Exception) {
                ToastUtil.show(context, "导出失败！")
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val file = ConfigPreload.getConfigFile(userId)
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { input.copyTo(it) }
                }
                // 导入后通知支付宝进程重载；userId 为空表示默认账号，也要发广播（不带 extra 即可命中当前进程）
                try {
                    val intent = Intent("com.eg.android.AlipayGphone.sesame.restart")
                    if (!StringUtil.isEmpty(userId)) {
                        intent.putExtra("userId", userId)
                    }
                    context.sendBroadcast(intent)
                } catch (th: Throwable) {
                    Log.printStackTrace(th)
                }
                Model.initAllModel()
                ConfigPreload.reload(userId)
                ToastUtil.show(context, "导入成功！")
            } catch (e: Exception) {
                ToastUtil.show(context, "导入失败！")
            }
        }
    }

    // ============ 二级:分组目录 ============
    Scaffold(
        topBar = {
            LogTopBar(
                title = "配置设置",
                onBack = { activity.saveAndFinish() },
                onImport = { importLauncher.launch("*/*") },
                onExport = { exportLauncher.launch("[" + (userId ?: "默认") + "]-config_v2.json") },
                onClear = { showDeleteDialog = true }
            )
        },
        containerColor = MiuixTheme.colorScheme.surface
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // ============ 配置分组目录 ============
            SmallTitle(text = "配置分组")
            CardList {
                ModelGroup.values().forEach { g ->
                    if (Model.getGroupModelConfig(g).isNotEmpty()) {
                        CardArrowPreference(title = g.getName(), onClick = {
                            activity.startActivity(
                                Intent(activity, MiuixGroupFieldsActivity::class.java).apply {
                                    putExtra(MiuixGroupFieldsActivity.EXTRA_USER_ID, userId)
                                    putExtra(MiuixGroupFieldsActivity.EXTRA_GROUP_CODE, g.name)
                                }
                            )
                        })
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            if (showDeleteDialog) {
                ConfirmDialog(
                    title = "警告",
                    text = "确认删除该配置？",
                    onConfirm = {
                        showDeleteDialog = false
                        if (ConfigPreload.getConfigFile(userId).let { com.surexu.sesame.util.FileUtil.deleteFile(it) }) {
                            ToastUtil.show(context, "配置删除成功")
                        }
                        activity.finish()
                    },
                    onDismiss = { showDeleteDialog = false }
                )
            }
        }
    }
}

/**
 * 配置字段编辑项（原地展开编辑）。
 *
 * 值为「只写内存」：任何变更只调用 setObjectValue() 落在 ConfigV2 单例上，不触发磁盘写入。
 * 统一落盘由所在页面的退出流程负责（MiuixGroupFieldsActivity.saveAndFinish() / onBackPressed()），
 * 避免每次拨开关、每次提交输入都做一次「全量序列化 + 写盘 + 备份检查」。
 */
@Composable
fun FieldItem(field: ModelField<*>, onFieldChanged: (() -> Unit)? = null) {
    // 用字段名唯一标识展开状态，避免 LazyColumn 复用导致错位
    val fieldKey = "${field.type}:${field.name}"
    var expanded by remember { mutableStateOf(false) }
    var expandedFieldKey by remember { mutableStateOf<String?>(null) }
    when {
        field.type == "BOOLEAN" -> {
            var checked by remember { mutableStateOf(field.value as? Boolean ?: false) }
            SwitchPreference(
                title = field.name ?: "",
                summary = field.description,
                checked = checked,
                onCheckedChange = {
                    checked = it
                    field.setObjectValue(it)
                    onFieldChanged?.invoke()
                }
            )
        }

        field.type in listOf("INTEGER", "MULTIPLY_INTEGER") -> {
            val context = LocalContext.current
            val imf = field as? IntegerModelField
            val maxLimit = imf?.maxLimit
            val lowerLimit = imf?.minLimit
            val current = (field as? IntegerModelField.MultiplyIntegerModelField)?.getConfigValue()?.toInt() ?: (field.value as? Int ?: 0)
            val limitHint = when {
                lowerLimit == null && maxLimit == null -> ""
                lowerLimit != null && lowerLimit < 0 -> "（-1 表示按最大额度）"
                lowerLimit != null && maxLimit != null -> "（${lowerLimit}~${maxLimit}）"
                maxLimit != null -> "（上限 ${maxLimit}）"
                else -> "（下限 ${lowerLimit}）"
            }
            if (expandedFieldKey != fieldKey) {
                expanded = false
                expandedFieldKey = null
            }
            ArrowPreference(
                title = field.name ?: "",
                summary = if (limitHint.isEmpty()) current.toString() else "$current$limitHint",
                onClick = {
                    expanded = !expanded
                    expandedFieldKey = fieldKey
                }
            )
            if (expanded) {
                val context2 = LocalContext.current
                var text by remember { mutableStateOf(current.toString()) }
                Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    TextField(
                        value = text,
                        onValueChange = { text = it },
                        label = "",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(text = "取消", onClick = { expanded = false; expandedFieldKey = null })
                        Spacer(Modifier.width(8.dp))
                        TextButton(text = "保存", onClick = {
                            val parsed = text.trim().toIntOrNull()
                            if (parsed == null) {
                                ToastUtil.show(context2, "请输入有效整数")
                            } else if (lowerLimit != null && parsed < lowerLimit) {
                                ToastUtil.show(context2, "最小值为 $lowerLimit")
                            } else if (maxLimit != null && parsed > maxLimit) {
                                ToastUtil.show(context2, "最大值为 $maxLimit")
                            } else {
                                field.setConfigValue(parsed.toString())
                                expanded = false
                                expandedFieldKey = null
                            }
                        })
                    }
                }
            }
        }

        field.type in listOf("STRING", "TEXT") -> {
            if (expandedFieldKey != fieldKey) {
                expanded = false
                expandedFieldKey = null
            }
            ArrowPreference(
                title = field.name ?: "",
                summary = field.configValue,
                onClick = {
                    expanded = !expanded
                    expandedFieldKey = fieldKey
                }
            )
            if (expanded) {
                var text by remember { mutableStateOf(field.configValue ?: "") }
                // 会员额外兑换：按名称实时过滤权益库候选，点击候选直接填入
                val isCustomBenefit = field.code == "memberPointExchangeCustom"
                val candidates = remember(text, isCustomBenefit) {
                    if (!isCustomBenefit || text.isBlank()) {
                        emptyList()
                    } else {
                        MemberBenefit.getList()
                            .filter { it.name.contains(text.trim(), ignoreCase = true) || it.id.contains(text.trim()) }
                            .take(10)
                    }
                }
                Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    TextField(
                        value = text,
                        onValueChange = { text = it },
                        label = "",
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isCustomBenefit && candidates.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        candidates.forEach { cand ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { text = cand.name }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = cand.name,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    color = MiuixTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(text = "取消", onClick = { expanded = false; expandedFieldKey = null })
                        Spacer(Modifier.width(8.dp))
                        TextButton(text = "保存", onClick = {
                            field.setObjectValue(text)
                            expanded = false
                            expandedFieldKey = null
                        })
                    }
                }
            }
        }

        field.type == "READ_TEXT" || field.type == "URL_TEXT" -> {
            ArrowPreference(title = field.name ?: "", summary = field.configValue)
        }

        field.type in listOf("SELECT", "SELECT_ONE", "SELECT_AND_COUNT", "SELECT_AND_COUNT_ONE") -> {} // 由 GroupFieldsPage 处理
        field.type == "EMPTY" -> {
            val emf = field as? EmptyModelField
            ArrowPreference(title = field.name ?: "", onClick = { emf?.clickRunner?.run() })
        }

        field.type == "LIST" -> {
            val list = (field.value as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
            if (expandedFieldKey != fieldKey) {
                expanded = false
                expandedFieldKey = null
            }
            ArrowPreference(
                title = field.name ?: "",
                summary = list.joinToString(","),
                onClick = {
                    expanded = !expanded
                    expandedFieldKey = fieldKey
                }
            )
            if (expanded) {
                var text by remember { mutableStateOf(list.joinToString("\n")) }
                Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    TextField(
                        value = text,
                        onValueChange = { text = it },
                        label = "",
                        singleLine = false,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(text = "取消", onClick = { expanded = false; expandedFieldKey = null })
                        Spacer(Modifier.width(8.dp))
                        TextButton(text = "保存", onClick = {
                            val newList = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
                            field.setObjectValue(newList)
                            expanded = false
                            expandedFieldKey = null
                        })
                    }
                }
            }
        }

        field.type == "CHOICE" -> {
            val cmf = field as? ChoiceModelField
            val choiceArray = cmf?.expandKey ?: emptyArray()
            val current = field.value as? Int ?: 0
            if (expandedFieldKey != fieldKey) {
                expanded = false
                expandedFieldKey = null
            }
            ArrowPreference(
                title = field.name ?: "",
                summary = choiceArray.getOrNull(current),
                onClick = {
                    expanded = !expanded
                    expandedFieldKey = fieldKey
                }
            )
            if (expanded) {
                var sel by remember { mutableStateOf(current) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .background(
                            color = MiuixTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    choiceArray.forEachIndexed { index, opt ->
                        RadioButtonPreference(
                            title = opt,
                            selected = sel == index,
                            titleColor = BasicComponentColors(
                                color = if (sel == index) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
                                disabledColor = MiuixTheme.colorScheme.disabledOnSurface
                            ),
                            onClick = { sel = index }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(text = "取消", onClick = { expanded = false; expandedFieldKey = null })
                        Spacer(Modifier.width(8.dp))
                        TextButton(text = "保存", onClick = {
                            field.setObjectValue(sel)
                            expanded = false
                            expandedFieldKey = null
                            onFieldChanged?.invoke()
                        })
                    }
                }
            }
        }

        else -> {
            Text(text = field.name ?: "", color = MiuixTheme.colorScheme.onBackground)
        }
    }
}
