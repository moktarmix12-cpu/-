package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.signOutUser
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CertificateScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LessonScreen
import com.example.ui.screens.PaymentActivationScreen
import com.example.ui.screens.QuizScreen
import com.example.ui.screens.SummaryScreen
import com.example.ui.theme.AppBackgroundTheme
import com.example.ui.theme.SirahKidsTheme
import com.example.ui.theme.ThemePreferences
import com.example.ui.viewmodel.SirahViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

private const val ROUTE_HOME = "home"
private const val ROUTE_SUMMARY = "summary"
private const val ROUTE_LESSON = "lesson/{chapterNumber}"
private const val ROUTE_QUIZ = "quiz/{chapterNumber}"
private const val ROUTE_PAYMENT = "payment"
private const val ROUTE_CERTIFICATE = "certificate"
private const val ROUTE_ADMIN = "admin"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val selectedTheme by ThemePreferences.observeTheme(context)
                .collectAsStateWithLifecycle(initialValue = AppBackgroundTheme.SKY_BLUE)
            SirahKidsTheme(selectedTheme = selectedTheme) {
                SirahRootApp(
                    currentTheme = selectedTheme,
                    onSelectTheme = { newTheme ->
                        scope.launch { ThemePreferences.saveTheme(context, newTheme) }
                    }
                )
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun SirahRootApp(
    currentTheme: AppBackgroundTheme = AppBackgroundTheme.SKY_BLUE,
    onSelectTheme: (AppBackgroundTheme) -> Unit = {},
    auth: FirebaseAuth = Firebase.auth
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            val viewModel: SirahViewModel = viewModel(
                key = user.uid,
                factory = SirahViewModel.factory(
                    userId = user.uid,
                    email = user.email.orEmpty(),
                    displayName = user.displayName.orEmpty()
                )
            )
            SirahNavHost(
                viewModel = viewModel,
                currentTheme = currentTheme,
                onSelectTheme = onSelectTheme,
                onSignOut = {
                    signOutUser(credentialManager = credentialManager, scope = scope)
                }
            )
        }
    }
}

@Composable
fun SirahNavHost(
    viewModel: SirahViewModel,
    currentTheme: AppBackgroundTheme,
    onSelectTheme: (AppBackgroundTheme) -> Unit,
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val chapters by viewModel.chaptersCatalog.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val lessonState by viewModel.lessonState.collectAsStateWithLifecycle()
    val userPayments by viewModel.userPayments.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()

    val adminPayments by viewModel.adminPayments.collectAsStateWithLifecycle()
    val adminUsers by viewModel.adminUsers.collectAsStateWithLifecycle()
    val adminCodes by viewModel.adminCodes.collectAsStateWithLifecycle()
    val adminAppConfig by viewModel.adminAppConfig.collectAsStateWithLifecycle()

    LaunchedEffect(actionMessage) {
        val msg = actionMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    val context = LocalContext.current

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { outerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = outerPadding.calculateBottomPadding())
        ) {
            NavHost(
                navController = navController,
                startDestination = ROUTE_HOME
            ) {
                composable(ROUTE_HOME) {
                    HomeScreen(
                        userProfileState = userProfileState,
                        chapters = chapters,
                        isAdmin = isAdmin,
                        currentTheme = currentTheme,
                        onSelectTheme = onSelectTheme,
                        onChapterClick = { chapter, isUnlocked ->
                            if (isUnlocked) {
                                navController.navigate("lesson/${chapter.chapterNumber}")
                            } else {
                                navController.navigate(ROUTE_PAYMENT)
                            }
                        },
                        onOpenSummary = { navController.navigate(ROUTE_SUMMARY) },
                        onOpenPayment = { navController.navigate(ROUTE_PAYMENT) },
                        onOpenCertificate = { navController.navigate(ROUTE_CERTIFICATE) },
                        onOpenAdmin = { navController.navigate(ROUTE_ADMIN) },
                        onUpdateChildName = { viewModel.updateChildName(it) },
                        onSignOut = onSignOut
                    )
                }

                composable(ROUTE_SUMMARY) {
                    val hasFullAccess = (userProfileState as? com.example.ui.viewmodel.UiState.Success)
                        ?.data?.hasFullAccess == true
                    SummaryScreen(
                        hasFullAccess = hasFullAccess,
                        isAdmin = isAdmin,
                        onOpenChapter = { chapterNum, unlocked ->
                            if (unlocked) {
                                navController.navigate("lesson/$chapterNum")
                            } else {
                                navController.navigate(ROUTE_PAYMENT)
                            }
                        },
                        onOpenPayment = { navController.navigate(ROUTE_PAYMENT) },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = ROUTE_LESSON,
                    arguments = listOf(navArgument("chapterNumber") { type = NavType.IntType })
                ) { backStackEntry ->
                    val chapterNum = backStackEntry.arguments?.getInt("chapterNumber") ?: 1
                    LaunchedEffect(chapterNum) {
                        viewModel.loadLesson(chapterNum)
                    }
                    LessonScreen(
                        lessonState = lessonState,
                        onBack = { navController.popBackStack() },
                        onStartQuiz = { num -> navController.navigate("quiz/$num") },
                        onOpenPayment = {
                            navController.popBackStack()
                            navController.navigate(ROUTE_PAYMENT)
                        }
                    )
                }

                composable(
                    route = ROUTE_QUIZ,
                    arguments = listOf(navArgument("chapterNumber") { type = NavType.IntType })
                ) {
                    QuizScreen(
                        lessonState = lessonState,
                        onQuizFinished = { chNum, correct, total ->
                            viewModel.recordQuizCompletion(chNum, correct, total)
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(ROUTE_PAYMENT) {
                    PaymentActivationScreen(
                        userPayments = userPayments,
                        isBusy = isBusy,
                        onSubmitReceipt = { method, phone, note, uri ->
                            viewModel.submitPaymentReceipt(context, method, phone, note, uri)
                        },
                        onActivateCode = { code ->
                            viewModel.activateWithCode(code) {
                                navController.popBackStack()
                            }
                        },
                        onCopyMessage = { msg ->
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(ROUTE_CERTIFICATE) {
                    CertificateScreen(
                        userProfileState = userProfileState,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(ROUTE_ADMIN) {
                    if (isAdmin) {
                        AdminScreen(
                            payments = adminPayments,
                            users = adminUsers,
                            codes = adminCodes,
                            appConfig = adminAppConfig,
                            chapters = chapters,
                            isBusy = isBusy,
                            onStartObservers = { viewModel.startAdminObservers() },
                            onReviewPayment = { reqId, approve, note, genUnique ->
                                viewModel.adminReviewPayment(reqId, approve, note, genUnique)
                            },
                            onUpdatePrimaryCode = { code, enabled ->
                                viewModel.adminUpdatePrimaryCode(code, enabled)
                            },
                            onCreateUniqueCode = { code, maxUses, note ->
                                viewModel.adminCreateUniqueCode(code, maxUses, note)
                            },
                            onToggleCode = { hash, active ->
                                viewModel.adminToggleCode(hash, active)
                            },
                            onToggleUserRevoke = { uid, revoked ->
                                viewModel.adminToggleUserRevoke(uid, revoked)
                            },
                            onAdminActivateUser = { uid ->
                                viewModel.adminActivateUser(uid)
                            },
                            onSeedCurriculum = { viewModel.adminSeedCurriculum() },
                            onLoadLessonForEdit = { num, cb -> viewModel.adminLoadLessonForEdit(num, cb) },
                            onSaveLesson = { lesson -> viewModel.adminSaveLesson(lesson) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
