package com.example.collectalogger2.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.collectalogger2.BottomAppBar
import com.example.collectalogger2.CollectaloggerApplication
import com.example.collectalogger2.ui.detail.DetailEditScreen
import com.example.collectalogger2.ui.detail.DetailScreen
import com.example.collectalogger2.ui.detail.DetailViewModel
import com.example.collectalogger2.ui.detail.DetailViewModelFactory
import com.example.collectalogger2.ui.gallery.GalleryScreen
import com.example.collectalogger2.ui.gallery.GalleryViewModel
import com.example.collectalogger2.ui.gallery.GalleryViewModelFactory
import com.example.collectalogger2.ui.settings.SettingsScreen
import com.example.collectalogger2.ui.settings.SettingsViewModel
import com.example.collectalogger2.ui.settings.SettingsViewModelFactory
import com.example.collectalogger2.ui.wishlist.WishListScreen
import com.example.collectalogger2.ui.wishlist.WishListViewModel
import com.example.collectalogger2.ui.wishlist.WishListViewModelFactory
import kotlinx.serialization.Serializable

// Routes
@Serializable
object WishList
@Serializable
object Settings

// Route for nested
@Serializable
object Detail

// Detail graph
@Serializable
data class DetailView(val id: Long)

@Serializable
data class DetailEdit(val id: Long)


@Serializable
data class Gallery(
    val genre: Int? = null,
    val developer: String? = null,
    val publisher: String? = null,
    val isFavorite: Boolean? = null,
    val library: String? = null,
    val platform: String? = null
)


//@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun CollectaloggerNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val appContainer = (LocalContext.current.applicationContext as CollectaloggerApplication).container

    Scaffold(
        bottomBar = { BottomAppBar(
            onNavigateToGallery = {
                navController.navigate(route = Gallery()) {
                    launchSingleTop = true
                }
            },
            onNavigateToWishlist = { navController.navigate(route = WishList) {launchSingleTop = true} },
            onNavigateToSettings = { navController.navigate(route = Settings) {launchSingleTop = true} },
        ) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Gallery(),
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<Gallery> { backStackEntry ->
                val factory = remember {
                    GalleryViewModelFactory(
                        appContainer,
                        backStackEntry,
                        backStackEntry.arguments
                    )
                }
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

            navigation<Detail>(startDestination = DetailView::class) {


                composable<DetailView> { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(Detail)
                    }
                    val factory = remember {
                        DetailViewModelFactory(
                            appContainer,
                            parentEntry,
                            parentEntry.arguments
                        )
                    }
                    val detailViewModel: DetailViewModel = viewModel(parentEntry, factory = factory)
                    DetailScreen(
                        viewModel = detailViewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onEditPlayStatus = { it -> detailViewModel.editPlayStatus(it) },
                        onSelectGalleryFilter = {
                                genre: Int?,
                                developer: String?,
                                publisher: String?,
                                isFavorite: Boolean?,
                                library: String?,
                                platform: String?,
                            ->
                            navController.navigate(
                                route = Gallery(
                                    genre,
                                    developer,
                                    publisher,
                                    isFavorite,
                                    library,
                                    platform
                                )
                            )
                        },
                        onSelectEditScreen = { id -> navController.navigate(route = DetailEdit(id)) }
                    )
                }
                composable<DetailEdit> { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(Detail)
                    }
                    val factory = remember {
                        DetailViewModelFactory(
                            appContainer,
                            parentEntry,
                            parentEntry.arguments
                        )
                    }
                    val detailViewModel: DetailViewModel = viewModel(parentEntry, factory = factory)
                    DetailEditScreen(
                        viewModel = detailViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}