package com.flower.launcher.search

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class SearchTarget(
    val id: String,
    val label: String,
    val primaryUri: String,
    val fallbackUri: String? = null,
    val packageName: String? = null,
    val prefix: String? = null,
    val actionTemplate: String = DEFAULT_ACTION_TEMPLATE,
    val enabled: Boolean = true,
    /** Optional container. Null means the target is ungrouped (legacy/default). */
    val groupId: String? = null,
) {
    companion object {
        const val DEFAULT_ACTION_TEMPLATE = "在“{name}”上搜索"
        const val QUERY_PLACEHOLDER = "{q}"
        const val NAME_PLACEHOLDER = "{name}"
    }
}

/**
 * A named container for search targets. Groups are presentation-only: they never
 * carry a URI, prefix or enabled flag, and the routing logic keeps working on the
 * flat [SearchTarget] list.
 */
data class SearchTargetGroup(
    val id: String,
    val label: String,
    val order: Int = 0,
)

/** A group header plus the members that survive the visibility filter, in list order. */
data class SearchTargetGroupMembers(
    val group: SearchTargetGroup,
    val targets: List<SearchTarget>,
)

/**
 * Presentation layout of a flat target list plus optional groups.
 *
 * - [ungrouped] holds targets with no group and targets whose group no longer
 *   exists, so removing a group never hides its members.
 * - [groups] is ordered by [SearchTargetGroup.order]. With `enabledOnly = true`
 *   the Search page drops empty groups; the settings page keeps them so the user
 *   can still open and refill them.
 */
data class SearchTargetLayout(
    val ungrouped: List<SearchTarget>,
    val groups: List<SearchTargetGroupMembers>,
)

fun layoutSearchTargets(
    targets: List<SearchTarget>,
    groups: List<SearchTargetGroup>,
    enabledOnly: Boolean,
): SearchTargetLayout {
    val knownGroupIds = groups.mapTo(HashSet()) { it.id }
    val isVisible: (SearchTarget) -> Boolean = { !enabledOnly || it.enabled }
    val ungrouped = targets.filter { target ->
        (target.groupId == null || target.groupId !in knownGroupIds) && isVisible(target)
    }
    val orderedGroups = groups.sortedWith(compareBy({ it.order }, { it.label }))
    val members = orderedGroups.mapNotNull { group ->
        val groupTargets = targets.filter { it.groupId == group.id && isVisible(it) }
        if (enabledOnly && groupTargets.isEmpty()) null else SearchTargetGroupMembers(group, groupTargets)
    }
    return SearchTargetLayout(ungrouped = ungrouped, groups = members)
}

data class PrefixMatch(
    val target: SearchTarget,
    val remainder: String,
)

/**
 * When the query starts with a target prefix ("/gd 南京东路") the whole query is
 * routed to that target and the remaining text is used as the search term.
 */
fun matchSearchTargetPrefix(query: String, targets: List<SearchTarget>): PrefixMatch? {
    val trimmed = query.trimStart()
    if (!trimmed.startsWith("/")) return null
    val match = targets.firstOrNull { target ->
        val prefix = target.prefix
        !prefix.isNullOrBlank() &&
            target.enabled &&
            trimmed.startsWith(prefix, ignoreCase = true) &&
            (trimmed.length == prefix.length || !trimmed[prefix.length].isLetterOrDigit())
    } ?: return null
    val prefixLength = match.prefix!!.length
    return PrefixMatch(match, trimmed.substring(prefixLength).trimStart())
}

/** Targets whose prefix can complete the "/xxx" token currently being typed. */
fun searchPrefixSuggestions(query: String, targets: List<SearchTarget>): List<SearchTarget> {
    val trimmed = query.trimStart()
    if (trimmed.contains(' ')) return emptyList()
    if (!trimmed.startsWith("/")) return emptyList()
    return targets
        .filter { target ->
            target.enabled &&
                !target.prefix.isNullOrBlank() &&
                target.prefix.startsWith(trimmed, ignoreCase = true)
        }
}

data class SearchTargetPreset(
    val label: String,
    val primaryUri: String,
    val fallbackUri: String? = null,
)

object SearchTargetPresets {
    val all: List<SearchTargetPreset> = listOf(
        SearchTargetPreset(
            label = "Play Store",
            primaryUri = "market://search?q={q}",
            fallbackUri = "https://play.google.com/store/search?q={q}&c=apps",
        ),
        SearchTargetPreset(label = "Baidu", primaryUri = "https://www.baidu.com/s?wd={q}"),
        SearchTargetPreset(label = "Google", primaryUri = "https://www.google.com/search?q={q}"),
        SearchTargetPreset(label = "酷安", primaryUri = "https://www.coolapk.com/search?q={q}"),
        SearchTargetPreset(label = "APKPure", primaryUri = "https://apkpure.com/search?q={q}"),
        SearchTargetPreset(label = "F-Droid", primaryUri = "https://f-droid.org/en/search/?q={q}"),
        SearchTargetPreset(label = "应用宝", primaryUri = "https://sj.qq.com/search?q={q}"),
    )

    fun newTarget(
        label: String,
        primaryUri: String,
        fallbackUri: String? = null,
    ): SearchTarget = SearchTarget(
        id = UUID.randomUUID().toString(),
        label = label,
        primaryUri = primaryUri,
        fallbackUri = fallbackUri,
    )
}

class SearchTargetRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): List<SearchTarget> {
        if (!prefs.contains(KEY_TARGETS)) return defaultTargets()
        val raw = prefs.getString(KEY_TARGETS, "") ?: return emptyList()
        return runCatching { parse(raw) }.getOrDefault(emptyList())
    }

    fun save(targets: List<SearchTarget>) {
        prefs.edit().putString(KEY_TARGETS, serialize(targets)).apply()
    }

    /** Returns only groups the user created; legacy installs have none. */
    fun loadGroups(): List<SearchTargetGroup> {
        if (!prefs.contains(KEY_GROUPS)) return emptyList()
        val raw = prefs.getString(KEY_GROUPS, "") ?: return emptyList()
        return runCatching { parseGroups(raw) }.getOrDefault(emptyList())
    }

    fun saveGroups(groups: List<SearchTargetGroup>) {
        prefs.edit().putString(KEY_GROUPS, serializeGroups(groups)).apply()
    }

    /** Ids of groups the user expanded on the Search page (persisted UX state). */
    fun loadExpandedGroups(): Set<String> =
        prefs.getStringSet(KEY_EXPANDED_GROUPS, emptySet())?.toSet() ?: emptySet()

    fun saveExpandedGroups(ids: Set<String>) {
        prefs.edit().putStringSet(KEY_EXPANDED_GROUPS, ids.toSet()).apply()
    }

    private fun defaultTargets(): List<SearchTarget> = listOf(
        SearchTarget(
            id = "default-web",
            label = "Baidu",
            primaryUri = "https://www.baidu.com/s?wd={q}",
        ),
        SearchTarget(
            id = "default-store",
            label = "Play Store",
            primaryUri = "market://search?q={q}",
            fallbackUri = "https://play.google.com/store/search?q={q}&c=apps",
        ),
    )

    private fun serialize(targets: List<SearchTarget>): String {
        val array = JSONArray()
        targets.forEach { target ->
            array.put(
                JSONObject().apply {
                    put("id", target.id)
                    put("label", target.label)
                    put("primary", target.primaryUri)
                    put("fallback", target.fallbackUri ?: JSONObject.NULL)
                    put("package", target.packageName ?: JSONObject.NULL)
                    put("prefix", target.prefix ?: JSONObject.NULL)
                    put("action", target.actionTemplate)
                    put("enabled", target.enabled)
                    put("group", target.groupId ?: JSONObject.NULL)
                },
            )
        }
        return array.toString()
    }

    private fun parse(raw: String): List<SearchTarget> {
        val array = JSONArray(raw)
        val result = ArrayList<SearchTarget>(array.length())
        for (index in 0 until array.length()) {
            val obj = array.optJSONObject(index) ?: continue
            val primary = obj.optString("primary", "")
            if (primary.isBlank()) continue
            result += SearchTarget(
                id = obj.optString("id", UUID.randomUUID().toString()),
                label = obj.optString("label", primary),
                primaryUri = primary,
                fallbackUri = if (obj.isNull("fallback")) null else obj.optString("fallback", "").ifBlank { null },
                packageName = if (obj.isNull("package")) null else obj.optString("package", "").ifBlank { null },
                prefix = if (obj.isNull("prefix")) null else obj.optString("prefix", "").ifBlank { null },
                actionTemplate = obj.optString("action", SearchTarget.DEFAULT_ACTION_TEMPLATE)
                    .ifBlank { SearchTarget.DEFAULT_ACTION_TEMPLATE },
                enabled = obj.optBoolean("enabled", true),
                groupId = if (obj.isNull("group")) null else obj.optString("group", "").ifBlank { null },
            )
        }
        return result
    }

    private fun serializeGroups(groups: List<SearchTargetGroup>): String {
        val array = JSONArray()
        groups.forEach { group ->
            array.put(
                JSONObject().apply {
                    put("id", group.id)
                    put("label", group.label)
                    put("order", group.order)
                },
            )
        }
        return array.toString()
    }

    private fun parseGroups(raw: String): List<SearchTargetGroup> {
        val array = JSONArray(raw)
        val result = ArrayList<SearchTargetGroup>(array.length())
        for (index in 0 until array.length()) {
            val obj = array.optJSONObject(index) ?: continue
            val id = obj.optString("id", "").ifBlank { continue }
            result += SearchTargetGroup(
                id = id,
                label = obj.optString("label", "").ifBlank { "未命名分类" },
                order = obj.optInt("order", index),
            )
        }
        return result
    }

    private companion object {
        const val PREFS = "flower_search_settings"
        const val KEY_TARGETS = "search_targets"
        const val KEY_GROUPS = "search_groups"
        const val KEY_EXPANDED_GROUPS = "search_expanded_groups"
    }
}
