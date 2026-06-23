package com.xiaoyin.lifeatlas.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.xiaoyin.lifeatlas.feature.home.HomeRoute
import com.xiaoyin.lifeatlas.feature.map.MapRoute
import com.xiaoyin.lifeatlas.feature.record.AddRecordRoute
import com.xiaoyin.lifeatlas.feature.settings.SettingsRoute
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
            composable(LifeAtlasDestination.Home.route) { HomeRoute() }
            composable(LifeAtlasDestination.Timeline.route) { TimelineRoute() }
            composable(LifeAtlasDestination.AddRecord.route) { AddRecordRoute() }
            composable(LifeAtlasDestination.Map.route) { MapRoute() }
            composable(LifeAtlasDestination.Settings.route) { SettingsRoute() }
        }
    }
}

