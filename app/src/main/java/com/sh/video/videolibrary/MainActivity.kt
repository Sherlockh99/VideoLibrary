package com.sh.video.videolibrary

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.sh.video.videolibrary.util.LocaleHelper
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
import com.sh.video.videolibrary.data.remote.TmdbMediaDetails
import com.sh.video.videolibrary.ui.MainViewModel
import com.sh.video.videolibrary.ui.MainViewModelFactory
import com.sh.video.videolibrary.ui.screens.AboutScreen
import com.sh.video.videolibrary.ui.screens.ActorDetailScreen
import com.sh.video.videolibrary.ui.screens.ActorsScreen
import com.sh.video.videolibrary.ui.screens.GenreDetailScreen
import com.sh.video.videolibrary.ui.screens.GenresScreen
import com.sh.video.videolibrary.ui.screens.CategoriesScreen
import com.sh.video.videolibrary.ui.screens.ImportFromArticleScreen
import com.sh.video.videolibrary.ui.screens.CategoryDetailScreen
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
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrapContext(newBase))
    }

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
                                onCategoriesClick = { navController.navigate("categories") },
                                onSettingsClick = { navController.navigate("settings") },
                                onActorsClick = { navController.navigate("actors") },
                                onGenresClick = { navController.navigate("genres") },
                                onImportFromArticleClick = { navController.navigate("import_article") }
                            )
                        }
                        composable("import_article") {
                            ImportFromArticleScreen(
                                viewModel = viewModel,
                                onBack = {
                                    viewModel.resetArticleImport()
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable("actors") {
                            ActorsScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onActorClick = { item ->
                                    navController.navigate("actor/${item.actor.id}")
                                }
                            )
                        }
                        composable("genres") {
                            GenresScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onGenreClick = { item ->
                                    navController.navigate("genre/${item.genre.id}")
                                }
                            )
                        }
                        composable(
                            route = "genre/{genreId}",
                            arguments = listOf(navArgument("genreId") { type = androidx.navigation.NavType.LongType })
                        ) { backStackEntry ->
                            val genreId = backStackEntry.arguments?.getLong("genreId") ?: 0L
                            val genre by viewModel.selectedGenre.collectAsState()
                            LaunchedEffect(genreId) {
                                viewModel.loadGenre(genreId)
                            }
                            genre?.let { g ->
                                GenreDetailScreen(
                                    genre = g,
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                    onMovieClick = { movie ->
                                        viewModel.selectMovie(movie)
                                        navController.navigate("detail")
                                    },
                                    onGenreClick = { genreId -> navController.navigate("genre/$genreId") }
                                )
                            }
                        }
                        composable(
                            route = "actor/{actorId}",
                            arguments = listOf(navArgument("actorId") { type = androidx.navigation.NavType.LongType })
                        ) { backStackEntry ->
                            val actorId = backStackEntry.arguments?.getLong("actorId") ?: 0L
                            val actor by viewModel.selectedActor.collectAsState()
                            LaunchedEffect(actorId) {
                                viewModel.loadActor(actorId)
                            }
                            actor?.let { a ->
                                ActorDetailScreen(
                                    actor = a,
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                    onMovieClick = { movie ->
                                        viewModel.selectMovie(movie)
                                        navController.navigate("detail")
                                    },
                                    onGenreClick = { genreId -> navController.navigate("genre/$genreId") },
                                    onAddFromTmdb = { navController.navigate("search") }
                                )
                            }
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
                                onBack = { navController.popBackStack() },
                                onGenreClick = { genreId -> navController.navigate("genre/$genreId") }
                            )
                        }
                        composable("categories") {
                            CategoriesScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onCategoryClick = { item ->
                                    navController.navigate("category/${item.category.id}")
                                }
                            )
                        }
                        composable(
                            route = "category/{categoryId}",
                            arguments = listOf(navArgument("categoryId") { type = androidx.navigation.NavType.LongType })
                        ) { backStackEntry ->
                            val categoryId = backStackEntry.arguments?.getLong("categoryId") ?: 0L
                            val categories by viewModel.categories.collectAsState()
                            val category = categories.find { it.id == categoryId }
                            LaunchedEffect(categories, categoryId) {
                                if (categories.isNotEmpty() && category == null) {
                                    navController.popBackStack()
                                }
                            }
                            if (category != null) {
                                CategoryDetailScreen(
                                    category = category,
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                    onMovieClick = { movie ->
                                        viewModel.selectMovie(movie)
                                        navController.navigate("detail")
                                    },
                                    onGenreClick = { genreId -> navController.navigate("genre/$genreId") },
                                    onAddFromTmdb = {
                                        viewModel.setPendingCategoryForNewMovie(category.id)
                                        navController.navigate("search")
                                    }
                                )
                            }
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
                                onAboutClick = { navController.navigate("about") },
                                onLanguageChange = { recreate() }
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
                                    },
                                    onMovieAdded = { movieId ->
                                        viewModel.clearReturnToCategoryIdAfterAdd()
                                        viewModel.setOpenDetailOnFilesTab(true)
                                        viewModel.selectMovieById(movieId) {
                                            navController.navigate("detail") {
                                                popUpTo("tmdb_preview") { inclusive = true }
                                            }
                                        }
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
                                    },
                                    onActorClick = { actorId ->
                                        navController.navigate("actor/$actorId")
                                    },
                                    onGenreClick = { genreId ->
                                        navController.navigate("genre/$genreId")
                                    },
                                    onCategoryClick = { categoryId ->
                                        navController.navigate("category/$categoryId")
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
