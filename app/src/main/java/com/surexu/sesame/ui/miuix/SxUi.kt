package com.surexu.sesame.ui.miuix

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 分组入口网格卡：顶部分组色渐变条 + 图标 + 名称/描述 + 已启用计数。 */
@Composable
fun SxGroupGridCard(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    count: Int,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .neuRaised(RoundedCornerShape(20.dp), 4.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Box(
            Modifier
                .size(40.dp)
                .neuPressed(RoundedCornerShape(13.dp), base = tint.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = tint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MiuixTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(8.dp))
        SxBadge(text = "已开 $count 项", tint = tint)
    }
}


/** 字段跳转行（SELECT 类）：名称 + 右箭头，点按进选择编辑页。 */
@Composable
fun SxSelectRow(name: String, summary: String?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 15.sp,
                color = MiuixTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!summary.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = summary,
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
            modifier = Modifier.size(18.dp)
        )
    }
}

/** 发丝分隔线：卡片内字段行之间的细线。 */
@Composable
fun SxDivider(startPadding: Dp = 16.dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = startPadding)
            .height(1.dp)
            .background(MiuixTheme.colorScheme.dividerLine)
    )
}

/** 圆角计数徽标。 */
@Composable
fun SxBadge(text: String, tint: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(tint.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = tint,
            fontWeight = FontWeight.Medium
        )
    }
}

/** 凹陷搜索槽：圆角输入框 + 搜索图标 + 一键清空。 */
@Composable
fun SxSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "搜索",
) {
    Row(
        Modifier
            .fillMaxWidth()
            .neuPressed(RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = placeholder,
                    fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.onBackground
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(MiuixTheme.colorScheme.primary),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (query.isNotEmpty()) {
            Text(
                text = "×",
                fontSize = 16.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier
                    .clickable { onQueryChange("") }
                    .padding(4.dp)
            )
        }
    }
}

/** 选项行卡片：凸起默认态 / 选中态主色描边，尾部自绘选择标记（圆点=单选，方勾=多选）。 */
@Composable
fun SxSelectableCard(
    title: String,
    checked: Boolean,
    single: Boolean,
    avatar: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .then(
                if (checked) {
                    Modifier
                        .neuRaised(shape, 3.dp)
                        .border(1.dp, MiuixTheme.colorScheme.primary, shape)
                } else {
                    Modifier.neuRaised(shape, 3.dp)
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        avatar?.invoke()
        if (avatar != null) {
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = title,
            fontSize = 15.sp,
            color = MiuixTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        trailing?.invoke()
        Spacer(Modifier.width(10.dp))
        SxCheckMark(checked = checked, single = single)
    }
}

/** 自绘选择标记：圆形单选点 / 方形多选勾。 */
@Composable
fun SxCheckMark(checked: Boolean, single: Boolean) {
    if (single) {
        Box(
            Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (checked) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(MiuixTheme.colorScheme.onPrimary))
            }
        }
    } else {
        Box(
            Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (checked) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Text(text = "✓", fontSize = 13.sp, color = MiuixTheme.colorScheme.onPrimary)
            }
        }
    }
}

/** 数量步进器：− / 数值 / ＋，替代上游 Slider。 */
@Composable
fun SxCountStepper(
    value: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    min: Int = 1,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SxStepButton(text = "−", enabled = value > min, onClick = onMinus)
        Text(
            text = value.toString(),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MiuixTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        SxStepButton(text = "＋", enabled = true, onClick = onPlus)
    }
}

@Composable
private fun SxStepButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(26.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (enabled) MiuixTheme.colorScheme.primary.copy(alpha = 0.12f) else MiuixTheme.colorScheme.surfaceContainerHighest
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            color = if (enabled) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantActions
        )
    }
}

/** 自绘拟态开关：凸起轨道 + 圆形滑块，开=主色、关=凹陷浅灰，带滑动动画。 */
@Composable
fun SxSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    val trackColor = if (checked) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.surfaceContainerHighest
    }
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 3.dp,
        label = "sxSwitchThumb"
    )
    Box(
        Modifier
            .size(width = 48.dp, height = 28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(trackColor)
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(3.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .offset(x = thumbOffset)
                .size(22.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(if (checked) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.surface)
                .shadow(2.dp, RoundedCornerShape(11.dp))
        )
    }
}

/** 通用设置行：标题 + 副标题，右侧尾部组件或箭头，点击展开/跳转。 */
@Composable
fun SxSettingRow(
    title: String,
    summary: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                color = MiuixTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!summary.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = summary,
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** 自研凹陷输入槽：与 SxSearchBar 同一设计语言，替代上游 TextField。 */
@Composable
fun SxTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "",
    singleLine: Boolean = true,
    maxLines: Int = 1,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
            Spacer(Modifier.height(4.dp))
        }
        Row(
            Modifier
                .fillMaxWidth()
                .neuPressed(RoundedCornerShape(6.dp), base = Color(0xFFF2F4F7))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MiuixTheme.colorScheme.onBackground
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(MiuixTheme.colorScheme.primary),
                singleLine = singleLine,
                maxLines = maxLines,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** 自研拟态图标按钮：纯白凸起圆角方块 + 居中图标，替代上游 miuix IconButton。 */
@Composable
fun SxIconButton(
    imageVector: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MiuixTheme.colorScheme.onBackground,
    iconSize: Dp = 20.dp,
) {
    Box(
        modifier
            .size(40.dp)
            .neuRaised(RoundedCornerShape(12.dp), 2.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector, contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}
