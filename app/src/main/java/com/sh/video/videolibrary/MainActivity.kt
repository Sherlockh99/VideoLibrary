package com.sh.video.videolibrary

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sh.video.videolibrary.data.remote.TmdbMovieDetails
import com.sh.video.videolibrary.ui.MainViewModel
import com.sh.video.videolibrary.ui.MainViewModelFactory
import com.sh.video.videolibrary.ui.screens.AboutScreen
import com.sh.video.videolibrary.ui.screens.HomeScreen
import com.sh.video.videolibrary.ui.screens.LibraryScreen
import com.sh.video.videolibrary.ui.screens.MovieDetailScreen
import com.sh.video.videolibrary.ui.screens.SearchTmdbScreen
import com.sh.video.videolibrary.ui.screens.TmdbMoviePreviewScreen
import com.sh.video.videolibrary.ui.screens.SettingsScreen
import com.sh.video.videolibrary.ui.screens.StorageDetailScreen
import com.sh.video.videolibrary.ui.screens.StoragesScreen
import com.sh.video.videolibrary.ui.theme.VideoLibraryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VideoLibraryTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    val viewModel: MainViewModel = viewModel(
                        factory = MainViewModelFactory(this)
                    )

                    val exportLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.CreateDocument("application/json")
                    ) { uri ->
                        uri?.let { viewModel.exportToUri(it) }
                    }
                    val importLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocument()
                    ) { uri ->
                        uri?.let {
                            viewModel.importFromUri(it, replaceDuplicates = false)
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") {
                            HomeScreen(
                                onCollectionClick = { navController.navigate("library") },
                                onStoragesClick = { navController.navigate("storages") },
                                onSettingsClick = { navController.navigate("settings") },
                                onAboutClick = { navController.navigate("about") }
                            )
                        }
                        composable("about") {
                            AboutScreen(onBack = { navController.popBackStack() })
                        }
                        composable("library") {
                            LibraryScreen(
                                viewModel = viewModel,
                                onAddClick = { navController.navigate("search") },
                                onMovieClick = { movie ->
                                    viewModel.selectMovie(movie)
                                    navController.navigate("detail")
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("storages") {
                            StoragesScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onStorageClick = { item ->
                                    navController.navigate("storage/${item.storage.id}")
                                }
                            )
                        }
                        composable(
                            route = "storage/{storageId}",
                            arguments = listOf(navArgument("storageId") { type = androidx.navigation.NavType.LongType })
                        ) { backStackEntry ->
                            val storageId = backStackEntry.arguments?.getLong("storageId") ?: 0L
                            val storages by viewModel.storages.collectAsState()
                            val storage = storages.find { it.id == storageId }
                            LaunchedEffect(storages, storageId) {
                                if (storages.isNotEmpty() && storage == null) {
                                    navController.popBackStack()
                                }
                            }
                            if (storage != null) {
                                StorageDetailScreen(
                                    storage = storage,
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                    onFileClick = { movieId ->
                                        viewModel.selectMovieById(movieId) {
                                            navController.navigate("detail")
                                        }
                                    }
                                )
                            }
                            // Если хранилище не найдено (ещё не загружено), остаёмся на экране — LaunchedEffect в деталях подгрузит
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onExportClick = { exportLauncher.launch("videolibrary_export.vlp") },
                                onImportClick = { importLauncher.launch(arrayOf("*/*")) },
                                onAboutClick = { navController.navigate("about") }
                            )
                        }
                        composable("search") {
                            SearchTmdbScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onMovieSelected = { movie ->
                                    viewModel.selectTmdbForPreview(movie)
                                    navController.navigate("tmdb_preview")
                                }
                            )
                        }
                        composable("tmdb_preview") {
                            val movie by viewModel.selectedTmdbForPreview.collectAsState()
                            if (movie != null) {
                                TmdbMoviePreviewScreen(
                                    movie = movie!!,
                                    viewModel = viewModel,
                                    onBack = {
                                        viewModel.clearTmdbPreview()
                                        viewModel.clearUiStateError()
                                        navController.popBackStack()
                                    }
                                )
                            }
                        }
                        composable("detail") {
                            val movie by viewModel.selectedMovie.collectAsState()
                            if (movie != null) {
                                MovieDetailScreen(
                                    movie = movie!!,
                                    viewModel = viewModel,
                                    onBack = {
                                        viewModel.clearSelectedMovie()
                                        navController.popBackStack()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
