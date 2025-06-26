package com.example.collectalogger2.ui

import android.app.Application
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.collectalogger2.AppContainer
import com.example.collectalogger2.CollectaloggerApplication
import com.example.collectalogger2.ui.detail.DetailViewModel
import com.example.collectalogger2.ui.gallery.GalleryViewModel
import com.example.collectalogger2.ui.wishlist.WishListViewModel


object AppViewModelProvider {
    val Factory = viewModelFactory {
        // Initializer for GalleryViewModel
        initializer {
            GalleryViewModel(
                collectaloggerApplication().container
            )
        }

        // Initializer for WishListViewModel
        initializer {
            WishListViewModel(
                collectaloggerApplication().container
            )
        }

    }
}

fun CreationExtras.collectaloggerApplication(): CollectaloggerApplication =
    (this[AndroidViewModelFactory.APPLICATION_KEY] as CollectaloggerApplication)