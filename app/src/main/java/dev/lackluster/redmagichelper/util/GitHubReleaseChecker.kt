package dev.lackluster.redmagichelper.util

import android.content.Context
import android.net.Uri
import android.util.Xml
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class GitHubRelease(
    val tagName: String,
    val title: String,
    val releaseUrl: String,
    val apkUrl: String?,
    val versionName: String?,
    val versionCode: Long?
) {
    val downloadUrl: String
        get() = apkUrl ?: releaseUrl
}

internal sealed interface GitHubUpdateResult {
    data class Available(val release: GitHubRelease) : GitHubUpdateResult
    data class UpToDate(val release: GitHubRelease) : GitHubUpdateResult
    data object Failed : GitHubUpdateResult
}

internal object GitHubReleaseChecker {
    private const val API_URL =
        "https://api.github.com/repos/Chrrr1y/RedMagic/releases/latest"
    private const val RELEASES_FEED_URL =
        "https://github.com/Chrrr1y/RedMagic/releases.atom"
    private const val RELEASE_DOWNLOAD_BASE =
        "https://github.com/Chrrr1y/RedMagic/releases/download"
    private const val RELEASE_PAGE_PREFIX =
        "https://github.com/Chrrr1y/RedMagic/releases/tag/"
    private const val PREFERENCES = "github_release_check"
    private const val CACHE_DURATION_MS = 6 * 60 * 60 * 1000L

    suspend fun check(
        context: Context,
        installedVersionName: String,
        installedVersionCode: Long,
        forceRefresh: Boolean = false
    ): GitHubUpdateResult = withContext(Dispatchers.IO) {
        val preferences = context.applicationContext
            .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val cachedRelease = readCachedRelease(preferences)
        val checkedAt = preferences.getLong("checked_at", 0L)
        val cacheIsFresh = cachedRelease != null && now >= checkedAt &&
            now - checkedAt < CACHE_DURATION_MS

        val release = try {
            if (!forceRefresh && cacheIsFresh) {
                cachedRelease
            } else {
                fetchLatestRelease().also { latest ->
                    writeCache(preferences, latest, now)
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            return@withContext GitHubUpdateResult.Failed
        }

        val hasUpdate = isNewerThanInstalled(
            release = release,
            installedVersionName = installedVersionName,
            installedVersionCode = installedVersionCode
        )
        if (hasUpdate) GitHubUpdateResult.Available(release)
        else GitHubUpdateResult.UpToDate(release)
    }

    private fun fetchLatestRelease(): GitHubRelease {
        val feedError = try {
            return fetchLatestReleaseFromFeed()
        } catch (error: Exception) {
            error
        }

        return try {
            fetchLatestReleaseFromApi()
        } catch (apiError: Exception) {
            throw IOException("Unable to fetch the latest GitHub release", apiError).apply {
                addSuppressed(feedError)
            }
        }
    }

    private fun fetchLatestReleaseFromApi(): GitHubRelease {
        val connection = URL(API_URL).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "RedMagic-Helper-Android")

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IOException("GitHub API returned HTTP $responseCode")
            }

            val response = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val json = JSONObject(response)
            val tagName = json.optString("tag_name").takeIf(String::isNotBlank)
                ?: throw IOException("GitHub release has no tag")
            val releaseUrl = json.optString("html_url").takeIf(String::isNotBlank)
                ?: throw IOException("GitHub release has no page URL")
            val version = parseTag(tagName)
            val apkUrl = json.optJSONArray("assets")
                ?.let { assets ->
                    (0 until assets.length())
                        .mapNotNull { index -> assets.optJSONObject(index) }
                        .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) }
                        ?.optString("browser_download_url")
                        ?.takeIf(String::isNotBlank)
                }

            return GitHubRelease(
                tagName = tagName,
                title = json.optString("name").takeIf(String::isNotBlank) ?: tagName,
                releaseUrl = releaseUrl,
                apkUrl = apkUrl,
                versionName = version.first,
                versionCode = version.second
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun fetchLatestReleaseFromFeed(): GitHubRelease {
        val connection = URL(RELEASES_FEED_URL).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.setRequestProperty("Accept", "application/atom+xml, application/xml")
            connection.setRequestProperty("User-Agent", "RedMagic-Helper-Android")

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IOException("GitHub releases feed returned HTTP $responseCode")
            }

            var title: String? = null
            var releaseUrl: String? = null
            val parser = Xml.newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            }
            connection.inputStream.use { input ->
                parser.setInput(input, Charsets.UTF_8.name())
                while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                    if (parser.eventType == XmlPullParser.START_TAG && parser.name == "entry") {
                        while (parser.next() != XmlPullParser.END_DOCUMENT) {
                            when (parser.eventType) {
                                XmlPullParser.START_TAG -> when (parser.name) {
                                    "title" -> if (title == null) title = parser.nextText()
                                    "link" -> {
                                        val relation = parser.getAttributeValue(null, "rel")
                                        if (relation == "alternate") {
                                            releaseUrl = parser.getAttributeValue(null, "href")
                                        }
                                    }
                                }
                                XmlPullParser.END_TAG -> if (parser.name == "entry") break
                            }
                        }
                        break
                    }
                    parser.next()
                }
            }

            val pageUrl = releaseUrl?.takeIf { it.startsWith(RELEASE_PAGE_PREFIX) }
                ?: throw IOException("GitHub releases feed has no latest release link")
            val tagName = pageUrl.substringAfterLast('/').takeIf(String::isNotBlank)
                ?: throw IOException("GitHub release link has no tag")
            val version = parseTag(tagName)
            val apkUrl = buildExpectedApkUrl(tagName, version)

            return GitHubRelease(
                tagName = tagName,
                title = title?.takeIf(String::isNotBlank) ?: tagName,
                releaseUrl = pageUrl,
                apkUrl = apkUrl,
                versionName = version.first,
                versionCode = version.second
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun buildExpectedApkUrl(tagName: String, version: Pair<String?, Long?>): String? {
        val versionName = version.first?.takeIf { it.matches(Regex("[A-Za-z0-9._-]+")) }
            ?: return null
        val versionCode = version.second ?: return null
        return "$RELEASE_DOWNLOAD_BASE/${Uri.encode(tagName)}/redmagic-helper-$versionName-$versionCode.apk"
    }

    private fun parseTag(tagName: String): Pair<String?, Long?> {
        val match = Regex("^v?(.+?)(?:-build-(\\d+))?$", RegexOption.IGNORE_CASE)
            .matchEntire(tagName)
            ?: return null to null
        return match.groupValues[1].takeIf(String::isNotBlank) to
            match.groupValues[2].toLongOrNull()
    }

    private fun isNewerThanInstalled(
        release: GitHubRelease,
        installedVersionName: String,
        installedVersionCode: Long
    ): Boolean {
        val versionComparison = release.versionName
            ?.let { compareNumericVersions(it, installedVersionName) }
        if (versionComparison != null && versionComparison != 0) {
            return versionComparison > 0
        }
        return release.versionCode?.let { it > installedVersionCode } ?: false
    }

    private fun compareNumericVersions(first: String, second: String): Int? {
        fun components(value: String) = Regex("\\d+")
            .findAll(value)
            .mapNotNull { it.value.toLongOrNull() }
            .toList()

        val firstParts = components(first)
        val secondParts = components(second)
        if (firstParts.isEmpty() || secondParts.isEmpty()) return null

        for (index in 0 until maxOf(firstParts.size, secondParts.size)) {
            val firstPart = firstParts.getOrElse(index) { 0L }
            val secondPart = secondParts.getOrElse(index) { 0L }
            if (firstPart != secondPart) return firstPart.compareTo(secondPart)
        }
        return 0
    }

    private fun readCachedRelease(preferences: android.content.SharedPreferences): GitHubRelease? {
        val tagName = preferences.getString("tag_name", null) ?: return null
        val releaseUrl = preferences.getString("release_url", null) ?: return null
        val parsedTag = parseTag(tagName)
        return GitHubRelease(
            tagName = tagName,
            title = preferences.getString("title", tagName) ?: tagName,
            releaseUrl = releaseUrl,
            apkUrl = preferences.getString("apk_url", null),
            versionName = preferences.getString("version_name", null) ?: parsedTag.first,
            versionCode = preferences.getString("version_code", null)?.toLongOrNull()
                ?: parsedTag.second
        )
    }

    private fun writeCache(
        preferences: android.content.SharedPreferences,
        release: GitHubRelease,
        checkedAt: Long
    ) {
        preferences.edit()
            .putString("tag_name", release.tagName)
            .putString("title", release.title)
            .putString("release_url", release.releaseUrl)
            .putString("apk_url", release.apkUrl)
            .putString("version_name", release.versionName)
            .putString("version_code", release.versionCode?.toString())
            .putLong("checked_at", checkedAt)
            .apply()
    }
}
