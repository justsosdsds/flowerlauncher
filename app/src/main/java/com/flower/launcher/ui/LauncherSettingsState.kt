package com.flower.launcher.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.flower.launcher.home.HomeLayoutRepository
import com.flower.launcher.home.HomePreferences
import com.flower.launcher.search.SearchPreferences
import com.flower.launcher.search.SearchPreferencesRepository
import com.flower.launcher.search.SearchTarget
import com.flower.launcher.search.SearchTargetGroup
import com.flower.launcher.search.SearchTargetRepository
import com.flower.launcher.settings.DebouncedPreferenceWriter
import com.flower.launcher.settings.LauncherPreferences
import com.flower.launcher.settings.LauncherPreferencesRepository
import kotlinx.coroutines.CoroutineScope

/** Owns settings snapshots and their persistence; never retains an Activity. */
internal class LauncherSettingsState(context: Context, scope: CoroutineScope) {
    private val homeRepository = HomeLayoutRepository(context.applicationContext)
    private val searchRepository = SearchPreferencesRepository(context.applicationContext)
    private val targetRepository = SearchTargetRepository(context.applicationContext)
    private val launcherRepository = LauncherPreferencesRepository(context.applicationContext)

    private var homeValue by mutableStateOf(homeRepository.load())
    private var searchValue by mutableStateOf(searchRepository.load())
    private var targetsValue by mutableStateOf(targetRepository.load())
    private var groupsValue by mutableStateOf(targetRepository.loadGroups())
    private var expandedValue by mutableStateOf(targetRepository.loadExpandedGroups())
    private var launcherValue by mutableStateOf(launcherRepository.load())

    private val homeWriter = DebouncedPreferenceWriter(scope, homeValue, save = homeRepository::save)
    private val searchWriter = DebouncedPreferenceWriter(scope, searchValue, save = searchRepository::save)
    private val targetsWriter = DebouncedPreferenceWriter(scope, targetsValue, save = targetRepository::save)
    private val groupsWriter = DebouncedPreferenceWriter(scope, groupsValue, save = targetRepository::saveGroups)
    private val expandedWriter = DebouncedPreferenceWriter(scope, expandedValue, save = targetRepository::saveExpandedGroups)
    private val launcherWriter = DebouncedPreferenceWriter(scope, launcherValue, save = launcherRepository::save)

    var home: HomePreferences
        get() = homeValue
        set(value) { homeValue = value; homeWriter.submit(value) }
    var search: SearchPreferences
        get() = searchValue
        set(value) { searchValue = value; searchWriter.submit(value) }
    var targets: List<SearchTarget>
        get() = targetsValue
        set(value) { targetsValue = value; targetsWriter.submit(value) }
    var groups: List<SearchTargetGroup>
        get() = groupsValue
        set(value) { groupsValue = value; groupsWriter.submit(value) }
    var expandedGroups: Set<String>
        get() = expandedValue
        set(value) { expandedValue = value; expandedWriter.submit(value) }
    var launcher: LauncherPreferences
        get() = launcherValue
        set(value) { launcherValue = value; launcherWriter.submit(value) }

    fun flush() {
        homeWriter.flush()
        searchWriter.flush()
        targetsWriter.flush()
        groupsWriter.flush()
        expandedWriter.flush()
        launcherWriter.flush()
    }
}
