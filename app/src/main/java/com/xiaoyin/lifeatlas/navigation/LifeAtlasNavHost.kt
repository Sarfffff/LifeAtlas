package com.xiaoyin.lifeatlas.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.xiaoyin.lifeatlas.core.auth.AuthRepository
import com.xiaoyin.lifeatlas.core.datastore.AppSettingsRepository
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessMeadow
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessPaper
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessTeal
import com.xiaoyin.lifeatlas.feature.auth.AuthRoute
import com.xiaoyin.lifeatlas.feature.city.CityDetailRoute
import com.xiaoyin.lifeatlas.feature.favorites.FavoriteCenterRoute
import com.xiaoyin.lifeatlas.feature.home.HomeRoute
import com.xiaoyin.lifeatlas.feature.map.MapRoute
import com.xiaoyin.lifeatlas.feature.map.MapPickerRoute
import com.xiaoyin.lifeatlas.feature.onboarding.OnboardingRoute
import com.xiaoyin.lifeatlas.feature.record.AddRecordRoute
import com.xiaoyin.lifeatlas.feature.record.EditRecordRoute
import com.xiaoyin.lifeatlas.feature.record.RecordDetailRoute
import com.xiaoyin.lifeatlas.feature.settings.SettingsRoute
import com.xiaoyin.lifeatlas.feature.settings.TagManagementRoute
import com.xiaoyin.lifeatlas.feature.timeline.TimelineRoute
import kotlinx.coroutines.launch

@Composable
fun LifeAtlasNavHost() {
    val context = LocalContext.current
    val authRepository = androidx.compose.runtime.remember { AuthRepository(context) }
    val settingsRepository = androidx.compose.runtime.remember { AppSettingsRepository(context) }
    val authSession by authRepository.session.collectAsState(initial = com.xiaoyin.lifeatlas.core.auth.AuthSession())
    val onboardingCompleted by settingsRepository.onboardingCompleted.collectAsState(initial = true)
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: LifeAtlasDestination.Home.route
    val showBottomBar = currentRoute != LifeAtlasDestination.Auth.route &&
        currentRoute != LifeAtlasDestination.Onboarding.route

    LaunchedEffect(authSession.isLoggedIn, authSession.skippedLogin, onboardingCompleted) {
        if (!authSession.isLoggedIn && !authSession.skippedLogin) {
            navController.navigate(LifeAtlasDestination.Auth.route) {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = false
                }
                launchSingleTop = true
            }
        } else if (!onboardingCompleted) {
            navController.navigate(LifeAtlasDestination.Onboarding.route) {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = false
                }
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                LifeAtlasBottomBar(
                    currentRoute = currentRoute,
                    onDestinationClick = { destination ->
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
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
                    onAddClick = {
                        navController.navigate(LifeAtlasDestination.AddRecord.route)
                    },
                    onViewAllClick = {
                        navController.navigate(LifeAtlasDestination.Timeline.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
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
                val addressResult by it.savedStateHandle
                    .getStateFlow(LifeAtlasDestination.MapPicker.resultAddressKey, "")
                    .collectAsState()
                AddRecordRoute(
                    pickedLatitude = latitudeResult.takeIf { value -> !value.isNaN() },
                    pickedLongitude = longitudeResult.takeIf { value -> !value.isNaN() },
                    pickedAddress = addressResult.ifBlank { null },
                    onMapPickerResultHandled = {
                        it.savedStateHandle.remove<Double>(LifeAtlasDestination.MapPicker.resultLatitudeKey)
                        it.savedStateHandle.remove<Double>(LifeAtlasDestination.MapPicker.resultLongitudeKey)
                        it.savedStateHandle.remove<String>(LifeAtlasDestination.MapPicker.resultAddressKey)
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
                    },
                    onAddMemoryClick = {
                        navController.navigate(LifeAtlasDestination.AddRecord.route)
                    },
                    onCityDetailClick = { city ->
                        navController.navigate(LifeAtlasDestination.CityDetail.createRoute(city))
                    }
                )
            }
            composable(LifeAtlasDestination.Settings.route) {
                SettingsRoute(
                    onAccountClick = {
                        navController.navigate(LifeAtlasDestination.Auth.route)
                    },
                    onTagManagementClick = {
                        navController.navigate(LifeAtlasDestination.TagManagement.route)
                    },
                    onFavoritesClick = {
                        navController.navigate(LifeAtlasDestination.Favorites.route)
                    }
                )
            }
            composable(LifeAtlasDestination.Auth.route) {
                AuthRoute(
                    onContinue = {
                        if (!navController.popBackStack()) {
                            navController.navigate(LifeAtlasDestination.Home.route) {
                                popUpTo(LifeAtlasDestination.Auth.route) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
            composable(LifeAtlasDestination.Onboarding.route) {
                OnboardingRoute(
                    onFinish = {
                        scope.launch {
                            settingsRepository.setOnboardingCompleted(true)
                            navController.navigate(LifeAtlasDestination.Home.route) {
                                popUpTo(LifeAtlasDestination.Onboarding.route) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }
                    },
                    onSkip = {
                        scope.launch {
                            settingsRepository.setOnboardingCompleted(true)
                            navController.navigate(LifeAtlasDestination.Home.route) {
                                popUpTo(LifeAtlasDestination.Onboarding.route) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
            composable(LifeAtlasDestination.TagManagement.route) {
                TagManagementRoute(onBack = { navController.popBackStack() })
            }
            composable(LifeAtlasDestination.Favorites.route) {
                FavoriteCenterRoute(
                    onBack = { navController.popBackStack() },
                    onRecordClick = { recordId ->
                        navController.navigate(LifeAtlasDestination.RecordDetail.createRoute(recordId))
                    }
                )
            }
            composable(
                route = LifeAtlasDestination.CityDetail.route,
                arguments = listOf(
                    navArgument(LifeAtlasDestination.CityDetail.cityArg) {
                        type = NavType.StringType
                    }
                )
            ) {
                CityDetailRoute(
                    onBack = { navController.popBackStack() },
                    onRecordClick = { recordId ->
                        navController.navigate(LifeAtlasDestination.RecordDetail.createRoute(recordId))
                    }
                )
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
                val addressResult by it.savedStateHandle
                    .getStateFlow(LifeAtlasDestination.MapPicker.resultAddressKey, "")
                    .collectAsState()
                EditRecordRoute(
                    pickedLatitude = latitudeResult.takeIf { value -> !value.isNaN() },
                    pickedLongitude = longitudeResult.takeIf { value -> !value.isNaN() },
                    pickedAddress = addressResult.ifBlank { null },
                    onMapPickerResultHandled = {
                        it.savedStateHandle.remove<Double>(LifeAtlasDestination.MapPicker.resultLatitudeKey)
                        it.savedStateHandle.remove<Double>(LifeAtlasDestination.MapPicker.resultLongitudeKey)
                        it.savedStateHandle.remove<String>(LifeAtlasDestination.MapPicker.resultAddressKey)
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
                        if (!point.address.isNullOrBlank()) {
                            navController.previousBackStackEntry?.savedStateHandle?.set(
                                LifeAtlasDestination.MapPicker.resultAddressKey,
                                point.address
                            )
                        }
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

@Composable
private fun LifeAtlasBottomBar(
    currentRoute: String,
    onDestinationClick: (LifeAtlasDestination) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .background(WildernessPaper)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        topLevelDestinations.forEach { destination ->
            val selected = currentRoute == destination.route
            val isAdd = destination == LifeAtlasDestination.AddRecord
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(70.dp)
                    .clip(if (isAdd) RoundedCornerShape(24.dp) else RoundedCornerShape(28.dp))
                    .background(
                        when {
                            isAdd -> androidx.compose.ui.graphics.Color.Transparent
                            selected -> WildernessMeadow.copy(alpha = 0.7f)
                            else -> androidx.compose.ui.graphics.Color.Transparent
                        }
                    )
                    .clickable { onDestinationClick(destination) },
                contentAlignment = Alignment.Center
            ) {
                if (isAdd) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(WildernessTeal.copy(alpha = 0.72f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label,
                                tint = WildernessPaper,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = destination.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = WildernessTeal.copy(alpha = 0.62f)
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.label,
                            tint = if (selected) WildernessTeal else WildernessTeal.copy(alpha = 0.58f),
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = destination.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
                            color = if (selected) WildernessTeal else WildernessTeal.copy(alpha = 0.58f)
                        )
                    }
                }
            }
        }
    }
}
