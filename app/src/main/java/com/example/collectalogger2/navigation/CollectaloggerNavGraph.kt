package com.example.collectalogger2.navigation

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.Composable
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
import com.example.collectalogger2.ui.AppViewModelProvider
import com.example.collectalogger2.ui.detail.DetailScreen
import com.example.collectalogger2.ui.detail.DetailViewModel
import com.example.collectalogger2.ui.wishlist.WishListViewModel
import com.example.collectalogger2.ui.gallery.GalleryScreen
import com.example.collectalogger2.ui.gallery.GalleryViewModel
import com.example.collectalogger2.ui.wishlist.WishListScreen
import kotlinx.serialization.Serializable

@Serializable
object Gallery
@Serializable
object WishList
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
            onNavigateToGallery = { navController.navigate(route = Gallery) },
            onNavigateToWishlist = { navController.navigate(route = WishList) },
        ) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Gallery,
            modifier = Modifier.padding(innerPadding)
        ) {
            // TODO finish this

            composable<Gallery> { backStackEntry ->
                val gallery: Gallery = backStackEntry.toRoute()
                val galleryViewModel: GalleryViewModel = viewModel()
                GalleryScreen(
                    viewModel = galleryViewModel,
                    onNavigateToDetail = { id -> navController.navigate(route = DetailView(id)) }
                )
            }
            composable<WishList> { backStackEntry ->
                val wishList: WishList = backStackEntry.toRoute()
                val wishListViewModel = WishListViewModel(container = appContainer)
                WishListScreen(
                    viewModel = wishListViewModel
                )
            }
            composable<DetailView> { backStackEntry ->
                val detail: DetailView = backStackEntry.toRoute()
                val detailViewModel = DetailViewModel(
                    container = appContainer,
                    gameId = detail.id
                )
                DetailScreen(
                    viewModel = detailViewModel
                )
            }
        }

    }
}