package com.xiaoyin.lifeatlas.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.ui.graphics.vector.ImageVector

sealed class LifeAtlasDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Home : LifeAtlasDestination("home", "首页", Icons.Outlined.Home)
    data object Timeline : LifeAtlasDestination("timeline", "时间轴", Icons.Outlined.Timeline)
    data object AddRecord : LifeAtlasDestination("add_record", "新增", Icons.Outlined.Add)
    data object Map : LifeAtlasDestination("map", "地图", Icons.Outlined.Map)
    data object Settings : LifeAtlasDestination("settings", "设置", Icons.Outlined.Settings)

    data object Auth {
        const val route = "auth"
    }

    data object Onboarding {
        const val route = "onboarding"
    }

    data object RecordDetail {
        const val route = "record_detail/{recordId}"
        const val recordIdArg = "recordId"

        fun createRoute(recordId: Long): String = "record_detail/$recordId"
    }

    data object EditRecord {
        const val route = "edit_record/{recordId}"
        const val recordIdArg = "recordId"

        fun createRoute(recordId: Long): String = "edit_record/$recordId"
    }

    data object MapPicker {
        const val route = "map_picker?lat={lat}&lng={lng}"
        const val latitudeArg = "lat"
        const val longitudeArg = "lng"
        const val resultLatitudeKey = "map_picker_result_latitude"
        const val resultLongitudeKey = "map_picker_result_longitude"
        const val resultAddressKey = "map_picker_result_address"

        fun createRoute(latitude: Double?, longitude: Double?): String {
            val latitudeValue = latitude?.toString().orEmpty()
            val longitudeValue = longitude?.toString().orEmpty()
            return "map_picker?lat=$latitudeValue&lng=$longitudeValue"
        }
    }

    data object TagManagement {
        const val route = "tag_management"
    }
}

val topLevelDestinations = listOf(
    LifeAtlasDestination.Home,
    LifeAtlasDestination.Timeline,
    LifeAtlasDestination.AddRecord,
    LifeAtlasDestination.Map,
    LifeAtlasDestination.Settings
)
