package com.huanchengfly.tieba.post.utils

import com.huanchengfly.tieba.post.BuildConfig
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

/**
 * 「检查更新」：读 GitHub Releases 的 latest（PLAN §21.5）。
 *
 * 不引新依赖：直接用 `HttpURLConnection` + `org.json`。调用方负责切到 IO 线程，
 * 失败（无网 / 限流 / 解析失败）一律返回 `Result.failure`，由 UI 决定提示还是静默。
 */
object UpdateChecker {

    private const val RELEASES_API =
        "https://api.github.com/repos/kingjoungyun/tiebaliquid/releases/latest"

    data class Release(
        /** 服务端 tag，例如 `v1.9.0`。 */
        val tagName: String,
        /** 发布页地址（用户点「确定」时打开）。 */
        val htmlUrl: String,
        /** 纯版本号（去掉 `v` 前缀），例如 `1.9.0`。 */
        val version: String,
    )

    fun currentVersion(): String = BuildConfig.VERSION_NAME

    /** 请求 latest release；失败返回 failure（不抛异常）。 */
    fun fetchLatest(): Result<Release> = runCatching {
        val connection = (URL(RELEASES_API).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "TiebaLite/${currentVersion()}")
        }
        try {
            if (connection.responseCode != 200) {
                error("HTTP ${connection.responseCode}")
            }
            val body = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8")).use { it.readText() }
            val json = JSONObject(body)
            val tag = json.optString("tag_name").takeIf { it.isNotBlank() } ?: error("no tag_name")
            val htmlUrl = json.optString("html_url").takeIf { it.isNotBlank() } ?: error("no html_url")
            Release(
                tagName = tag,
                htmlUrl = htmlUrl,
                version = tag.removePrefix("v").removePrefix("V").trim(),
            )
        } finally {
            connection.disconnect()
        }
    }

    /** `remote` 是否比 `current` 新（按 `.` 分段数字比较；无法解析时按字符串不等处理）。 */
    fun isNewer(remote: String, current: String = currentVersion()): Boolean {
        val r = parse(remote)
        val c = parse(current)
        if (r == null || c == null) {
            return remote.isNotBlank() && remote != current
        }
        val size = maxOf(r.size, c.size)
        for (i in 0 until size) {
            val rv = r.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (rv != cv) return rv > cv
        }
        return false
    }

    private fun parse(version: String): List<Int>? {
        val parts = version.removePrefix("v").removePrefix("V").split('.')
        if (parts.isEmpty()) return null
        return parts.map { part ->
            part.takeWhile { it.isDigit() }.toIntOrNull() ?: return null
        }
    }
}
