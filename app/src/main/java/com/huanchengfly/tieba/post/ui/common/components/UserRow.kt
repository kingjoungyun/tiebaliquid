package com.huanchengfly.tieba.post.ui.common.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar

/**
 * 「用户行」（T5 批次 2 抽出，批次 3 的关注列表复用同一份）。
 *
 * 版式：40dp 圆头像 + 名字（可传 `AnnotatedString` 以保留等级/昵称样式）+ 一行简介
 * + 右侧槽位（关注按钮 / chevron / 计数等，由调用方给）。
 *
 * 行本身不包卡片：调用方决定是放进 [GlassListCard]（列表形态）还是直接铺在页面里。
 *
 * @param name 名字；用 `AnnotatedString` 是为了兼容 `StringUtil.getUsernameAnnotatedString`
 * @param onClick 整行点击（为空时不可点）
 * @param trailing 右侧槽位（关注按钮等）
 */
@Composable
fun UserRow(
    name: AnnotatedString,
    modifier: Modifier = Modifier,
    portraitUrl: String? = null,
    description: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Avatar(
            data = portraitUrl.orEmpty(),
            size = 40.dp,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = name,
                fontSize = 15.sp,
                color = AppTokens.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = AppTokens.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing?.invoke(this)
    }
}
