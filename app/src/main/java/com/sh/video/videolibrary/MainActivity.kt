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
import com.sh.video.videolibrary.data.remote.TmdbMovieDetails
import com.sh.video.videolibrary.ui.MainViewModel
import com.sh.video.videolibrary.ui.MainViewModelFactory
import com.sh.video.videolibrary.ui.screens.HomeScreen
import com.sh.video.videolibrary.ui.screens.LibraryScreen
import com.sh.video.videolibrary.ui.screens.MovieDetailScreen
import com.sh.video.videolibrary.ui.screens.SearchTmdbScreen
import com.sh.video.videolibrary.ui.screens.SettingsScreen
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
                                onSettingsClick = { navController.navigate("settings") }
                            )
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
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onExportClick = { exportLauncher.launch("videolibrary_export.vlp") },
                                onImportClick = { importLauncher.launch(arrayOf("*/*")) }
                            )
                        }
                        composable("search") {
                            SearchTmdbScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onMovieSelected = { movie: TmdbMovieDetails ->
                                    viewModel.addMovie(movie)
                                    navController.popBackStack()
                                }
                            )
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
