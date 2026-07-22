package com.haminyan.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.haminyan.app.data.ThemeMode
import com.haminyan.app.ui.screens.DetailScreen
import com.haminyan.app.ui.screens.FavoritesScreen
import com.haminyan.app.ui.screens.NearbyScreen
import com.haminyan.app.ui.screens.SearchScreen
import com.haminyan.app.ui.screens.SettingsScreen
import com.haminyan.app.ui.screens.ZmanimScreen
import com.haminyan.app.ui.components.SplashScreen
import com.haminyan.app.ui.components.UpdateResultDialog
import com.haminyan.app.ui.theme.HaMinyanTheme
import com.haminyan.app.ui.vm.DetailViewModel
import com.haminyan.app.ui.vm.NearbyViewModel
import com.haminyan.app.ui.vm.SearchViewModel
import com.haminyan.app.ui.vm.StartupViewModel
import com.haminyan.app.ui.vm.UpdateViewModel
import com.haminyan.app.ui.vm.ZmanimViewModel
import java.net.URLDecoder
import java.net.URLEncoder

private data class BottomDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val bottomDestinations = listOf(
    BottomDestination("nearby", "בקרבתי", Icons.Filled.NearMe, Icons.Outlined.NearMe),
    BottomDestination("search", "חיפוש", Icons.Filled.Search, Icons.Outlined.Search),
    BottomDestination("zmanim", "זמנים", Icons.Filled.Schedule, Icons.Outlined.Schedule),
    BottomDestination("favorites", "מועדפים", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
    BottomDestination("settings", "הגדרות", Icons.Filled.Settings, Icons.Outlined.Settings),
)

class MainActivity : ComponentActivity() {

    private val startupViewModel: StartupViewModel by viewModels {
        StartupViewModel.factory(application as MinyanApp)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { startupViewModel.isShowingSplash() }
        enableEdgeToEdge()
        val app = application as MinyanApp
        setContent {
            val themeMode by app.prefs.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val showSplash by startupViewModel.showSplash.collectAsState()
            HaMinyanTheme(themeMode = themeMode) {
                if (showSplash) {
                    SplashScreen()
                } else {
                    MinyanNavHost(app)
                }
            }
        }
    }
}

@Composable
private fun MinyanNavHost(app: MinyanApp) {
    val navController = rememberNavController()
    val updateViewModel: UpdateViewModel = viewModel(factory = UpdateViewModel.factory(app))
    val updateState by updateViewModel.state.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showBottomBar = bottomDestinations.any { it.route == currentRoute }

    LaunchedEffect(Unit) {
        updateViewModel.checkOnStartup()
    }
    UpdateResultDialog(
        state = updateState,
        onDismiss = updateViewModel::dismissResult,
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomDestinations.forEach { destination ->
                        val selected = currentRoute == destination.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                    contentDescription = destination.label,
                                )
                            },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "nearby",
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            composable("nearby") {
                val vm: NearbyViewModel = viewModel(factory = NearbyViewModel.factory(app))
                NearbyScreen(viewModel = vm, onOpenMosad = { id, name ->
                    navController.navigate("mosad/$id/${URLEncoder.encode(name, "UTF-8")}")
                })
            }
            composable("search") {
                val vm: SearchViewModel = viewModel(factory = SearchViewModel.factory(app))
                SearchScreen(viewModel = vm, onOpenMosad = { id, name ->
                    navController.navigate("mosad/$id/${URLEncoder.encode(name, "UTF-8")}")
                })
            }
            composable("zmanim") {
                val vm: ZmanimViewModel = viewModel(factory = ZmanimViewModel.factory(app))
                ZmanimScreen(viewModel = vm)
            }
            composable("favorites") {
                FavoritesScreen(onOpenMosad = { id, name ->
                    navController.navigate("mosad/$id/${URLEncoder.encode(name, "UTF-8")}")
                })
            }
            composable("settings") {
                SettingsScreen(
                    updateState = updateState,
                    onCheckForUpdates = updateViewModel::checkManually,
                )
            }
            composable("mosad/{id}/{name}") { entry ->
                val id = entry.arguments?.getString("id") ?: return@composable
                val name = URLDecoder.decode(entry.arguments?.getString("name") ?: "", "UTF-8")
                val vm: DetailViewModel = viewModel(
                    key = "detail-$id",
                    factory = DetailViewModel.factory(app, id, name),
                )
                DetailScreen(viewModel = vm, mosadName = name, onBack = { navController.popBackStack() })
            }
        }
    }
}
