package com.example.collectalogger2.navigation

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import com.example.collectalogger2.BottomAppBar
import com.example.collectalogger2.ui.AppViewModelProvider
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

    Scaffold(
        bottomBar = { BottomAppBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Gallery,
            modifier = Modifier.padding(innerPadding)
        ) {
            // TODO finish this

            composable<Gallery> { backStackEntry ->
                val gallery: Gallery = backStackEntry.toRoute()
                val galleryViewModel: GalleryViewModel = viewModel(
                    factory = AppViewModelProvider.Factory,
                    viewModelStoreOwner = backStackEntry
                )
                GalleryScreen(
                    gallery = gallery,
                    viewModel = galleryViewModel
                )
            }
            composable<WishList> { backStackEntry ->
                val wishList: WishList = backStackEntry.toRoute()
                val wishListViewModel: WishListViewModel = viewModel(
                    factory = AppViewModelProvider.Factory,
                    viewModelStoreOwner = backStackEntry
                )
                WishListScreen(
                    wishList = wishList,
                    viewModel = wishListViewModel
                )
            }
            composable<DetailView> { /* things */ }
        }

    }
}