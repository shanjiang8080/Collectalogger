package com.example.collectalogger2.navigation

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import com.example.collectalogger2.AppContainer
import com.example.collectalogger2.BottomAppBar
import com.example.collectalogger2.CollectaloggerApplication
import com.example.collectalogger2.ui.detail.DetailScreen
import com.example.collectalogger2.ui.detail.DetailViewModel
import com.example.collectalogger2.ui.detail.DetailViewModelFactory
import com.example.collectalogger2.ui.wishlist.WishListViewModel
import com.example.collectalogger2.ui.gallery.GalleryScreen
import com.example.collectalogger2.ui.gallery.GalleryViewModel
import com.example.collectalogger2.ui.gallery.GalleryViewModelFactory
import com.example.collectalogger2.ui.settings.SettingsScreen
import com.example.collectalogger2.ui.settings.SettingsViewModel
import com.example.collectalogger2.ui.settings.SettingsViewModelFactory
import com.example.collectalogger2.ui.wishlist.WishListScreen
import com.example.collectalogger2.ui.wishlist.WishListViewModelFactory
import kotlinx.serialization.Serializable

@Serializable
object Gallery
@Serializable
object WishList
@Serializable
object Settings
@Serializable
data class DetailView(val id: Long)

//@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun CollectaloggerNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val appContainer = (LocalContext.current.applicationContext as CollectaloggerApplication).container

    Scaffold(
        bottomBar = { BottomAppBar(
            onNavigateToGallery = { navController.navigate(route = Gallery) {launchSingleTop = true} },
            onNavigateToWishlist = { navController.navigate(route = WishList) {launchSingleTop = true} },
            onNavigateToSettings = { navController.navigate(route = Settings) {launchSingleTop = true} },
        ) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Gallery,
            modifier = Modifier.padding(innerPadding)
        ) {
            // TODO finish this

            composable<Gallery> { backStackEntry ->
                val factory = remember { GalleryViewModelFactory(appContainer) }
                val galleryViewModel: GalleryViewModel = viewModel(backStackEntry, factory = factory)
                GalleryScreen(
                    viewModel = galleryViewModel,
                    onNavigateToDetail = { id -> navController.navigate(route = DetailView(id)) }
                )
            }
            composable<WishList> { backStackEntry ->
                val factory = remember { WishListViewModelFactory(appContainer) }
                val wishListViewModel: WishListViewModel = viewModel(backStackEntry, factory = factory)
                WishListScreen(
                    viewModel = wishListViewModel
                )
            }
            composable<Settings> { backStackEntry ->
                val factory = remember { SettingsViewModelFactory(appContainer) }
                val settingsViewModel: SettingsViewModel = viewModel(backStackEntry, factory = factory)
                SettingsScreen(
                    viewModel = settingsViewModel
                )
            }

            composable<DetailView> { backStackEntry ->
                val factory = remember { DetailViewModelFactory(appContainer, backStackEntry, backStackEntry.arguments) }
                val detailViewModel: DetailViewModel = viewModel(backStackEntry, factory = factory)
                DetailScreen(
                    viewModel = detailViewModel
                )
            }
        }
    }
}