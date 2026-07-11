package com.xiaoyin.lifeatlas.core.sync

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.xiaoyin.lifeatlas.core.auth.AuthRepository
import com.xiaoyin.lifeatlas.core.datastore.AppSettingsRepository
import com.xiaoyin.lifeatlas.data.export.ExportServiceProvider
import com.xiaoyin.lifeatlas.data.repository.RepositoryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@Composable
fun AutomaticCloudBackupEffect() {
    val context = LocalContext.current.applicationContext
    val coordinator = remember(context) { AutomaticCloudBackupCoordinator(context) }
    LaunchedEffect(coordinator) { coordinator.run() }
}

private class AutomaticCloudBackupCoordinator(context: Context) {
    private val authRepository = AuthRepository(context)
    private val settingsRepository = AppSettingsRepository(context)
    private val memoryRepository = RepositoryProvider.memoryRepository(context)
    private val exportService = ExportServiceProvider.exportService(context)

    @OptIn(FlowPreview::class)
    suspend fun run() {
        combine(
            memoryRepository.observeAllRecords(),
            memoryRepository.observeAllPhotosIncludingDeleted(),
            memoryRepository.observeAllTags(),
            memoryRepository.observeFavoriteRecordIds()
        ) { records, photos, tags, favorites ->
            listOf(records.hashCode(), photos.hashCode(), tags.hashCode(), favorites.hashCode()).hashCode()
        }
            .distinctUntilChanged()
            .drop(1)
            .debounce(6_000)
            .collect {
                val session = authRepository.session.first()
                val cloudSettings = settingsRepository.cloudSyncSettings.first()
                if (!session.isLoggedIn || !cloudSettings.enabled || !authRepository.isBackendConfigured()) return@collect

                runCatching {
                    val backupJson = withContext(Dispatchers.IO) { exportService.exportJson() }
                    authRepository.uploadCloudBackup(backupJson)
                }.onSuccess {
                    settingsRepository.markCloudSyncPrepared()
                }
            }
    }
}
