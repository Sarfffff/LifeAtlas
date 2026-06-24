package com.xiaoyin.lifeatlas.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.xiaoyin.lifeatlas.feature.home.HomeRoute
import com.xiaoyin.lifeatlas.feature.map.MapRoute
import com.xiaoyin.lifeatlas.feature.map.MapPickerRoute
import com.xiaoyin.lifeatlas.feature.record.AddRecordRoute
import com.xiaoyin.lifeatlas.feature.record.EditRecordRoute
import com.xiaoyin.lifeatlas.feature.record.RecordDetailRoute
import com.xiaoyin.lifeatlas.feature.settings.SettingsRoute
import com.xiaoyin.lifeatlas.feature.settings.TagManagementRoute
import com.xiaoyin.lifeatlas.feature.timeline.TimelineRoute

@Composable
fun LifeAtlasNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: LifeAtlasDestination.Home.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                topLevelDestinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label
                            )
                        },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = LifeAtlasDestination.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(LifeAtlasDestination.Home.route) {
                HomeRoute(
                    onRecordClick = { recordId ->
                        navController.navigate(LifeAtlasDestination.RecordDetail.createRoute(recordId))
                    }
                )
            }
            composable(LifeAtlasDestination.Timeline.route) {
                TimelineRoute(
                    onRecordClick = { recordId ->
                        navController.navigate(LifeAtlasDestination.RecordDetail.createRoute(recordId))
                    }
                )
            }
            composable(LifeAtlasDestination.AddRecord.route) {
                val latitudeResult by it.savedStateHandle
                    .getStateFlow(LifeAtlasDestination.MapPicker.resultLatitudeKey, Double.NaN)
                    .collectAsState()
                val longitudeResult by it.savedStateHandle
                    .getStateFlow(LifeAtlasDestination.MapPicker.resultLongitudeKey, Double.NaN)
                    .collectAsState()
                AddRecordRoute(
                    pickedLatitude = latitudeResult.takeIf { value -> !value.isNaN() },
                    pickedLongitude = longitudeResult.takeIf { value -> !value.isNaN() },
                    onMapPickerResultHandled = {
                        it.savedStateHandle.remove<Double>(LifeAtlasDestination.MapPicker.resultLatitudeKey)
                        it.savedStateHandle.remove<Double>(LifeAtlasDestination.MapPicker.resultLongitudeKey)
                    },
                    onMapPickerClick = { latitude, longitude ->
                        navController.navigate(LifeAtlasDestination.MapPicker.createRoute(latitude, longitude))
                    },
                    onRecordSaved = {
                        navController.navigate(LifeAtlasDestination.Timeline.route) {
                            popUpTo(LifeAtlasDestination.AddRecord.route) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(LifeAtlasDestination.Map.route) {
                MapRoute(
                    onRecordClick = { recordId ->
                        navController.navigate(LifeAtlasDestination.RecordDetail.createRoute(recordId))
                    }
                )
            }
            composable(LifeAtlasDestination.Settings.route) {
                SettingsRoute(
                    onTagManagementClick = {
                        navController.navigate(LifeAtlasDestination.TagManagement.route)
                    }
                )
            }
            composable(LifeAtlasDestination.TagManagement.route) {
                TagManagementRoute(onBack = { navController.popBackStack() })
            }
            composable(
                route = LifeAtlasDestination.RecordDetail.route,
                arguments = listOf(
                    navArgument(LifeAtlasDestination.RecordDetail.recordIdArg) {
                        type = NavType.LongType
                    }
                )
            ) {
                RecordDetailRoute(
                    onBack = { navController.popBackStack() },
                    onEdit = { recordId ->
                        navController.navigate(LifeAtlasDestination.EditRecord.createRoute(recordId))
                    },
                    onDeleted = {
                        navController.navigate(LifeAtlasDestination.Timeline.route) {
                            popUpTo(LifeAtlasDestination.Timeline.route) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(
                route = LifeAtlasDestination.EditRecord.route,
                arguments = listOf(
                    navArgument(LifeAtlasDestination.EditRecord.recordIdArg) {
                        type = NavType.LongType
                    }
                )
            ) {
                val latitudeResult by it.savedStateHandle
                    .getStateFlow(LifeAtlasDestination.MapPicker.resultLatitudeKey, Double.NaN)
                    .collectAsState()
                val longitudeResult by it.savedStateHandle
                    .getStateFlow(LifeAtlasDestination.MapPicker.resultLongitudeKey, Double.NaN)
                    .collectAsState()
                EditRecordRoute(
                    pickedLatitude = latitudeResult.takeIf { value -> !value.isNaN() },
                    pickedLongitude = longitudeResult.takeIf { value -> !value.isNaN() },
                    onMapPickerResultHandled = {
                        it.savedStateHandle.remove<Double>(LifeAtlasDestination.MapPicker.resultLatitudeKey)
                        it.savedStateHandle.remove<Double>(LifeAtlasDestination.MapPicker.resultLongitudeKey)
                    },
                    onMapPickerClick = { latitude, longitude ->
                        navController.navigate(LifeAtlasDestination.MapPicker.createRoute(latitude, longitude))
                    },
                    onBack = { navController.popBackStack() },
                    onSaved = { recordId ->
                        navController.popBackStack()
                        navController.navigate(LifeAtlasDestination.RecordDetail.createRoute(recordId)) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(
                route = LifeAtlasDestination.MapPicker.route,
                arguments = listOf(
                    navArgument(LifeAtlasDestination.MapPicker.latitudeArg) {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument(LifeAtlasDestination.MapPicker.longitudeArg) {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                val initialLatitude = backStackEntry.arguments
                    ?.getString(LifeAtlasDestination.MapPicker.latitudeArg)
                    ?.toDoubleOrNull()
                val initialLongitude = backStackEntry.arguments
                    ?.getString(LifeAtlasDestination.MapPicker.longitudeArg)
                    ?.toDoubleOrNull()

                MapPickerRoute(
                    initialLatitude = initialLatitude,
                    initialLongitude = initialLongitude,
                    onBack = { navController.popBackStack() },
                    onPointConfirmed = { point ->
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            LifeAtlasDestination.MapPicker.resultLatitudeKey,
                            point.latitude
                        )
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            LifeAtlasDestination.MapPicker.resultLongitudeKey,
                            point.longitude
                        )
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
