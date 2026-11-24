package com.kamarajlaw.mactmedlegal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kamarajlaw.mactmedlegal.data.AppDatabase
import com.kamarajlaw.mactmedlegal.data.CaseEntity
import com.kamarajlaw.mactmedlegal.ui.CaseDirectoryScreen
import com.kamarajlaw.mactmedlegal.ui.CaseFormScreen
import com.kamarajlaw.mactmedlegal.ui.CaseWorkspaceScreen
import com.kamarajlaw.mactmedlegal.ui.theme.MactMedLegalTheme
import com.kamarajlaw.mactmedlegal.ui.theme.NavyPrimary
import com.kamarajlaw.mactmedlegal.ui.theme.TealAccent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = AppDatabase.getInstance(this).mcopDao()

        setContent {
            MactMedLegalTheme {
                var showSplash by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(1200)
                    showSplash = false
                }

                val cases by dao.observeAllCases().collectAsState(initial = emptyList())
                var activeCaseId by remember { mutableStateOf<Long?>(null) }
                var isFormOpen by remember { mutableStateOf(false) }
                var editingCase by remember { mutableStateOf<CaseEntity?>(null) }
                val scope = rememberCoroutineScope()

                val activeBundle = cases.find { it.case.id == activeCaseId }

                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        isFormOpen -> {
                            CaseFormScreen(
                                existingCase = editingCase,
                                onCancel = {
                                    isFormOpen = false
                                    editingCase = null
                                },
                                onSave = { savedEntity ->
                                    scope.launch {
                                        val id = dao.upsertCase(savedEntity)
                                        isFormOpen = false
                                        editingCase = null
                                        activeCaseId = id
                                    }
                                }
                            )
                        }

                        activeBundle != null -> {
                            CaseWorkspaceScreen(
                                bundle = activeBundle,
                                dao = dao,
                                onBack = { activeCaseId = null },
                                onEditCaseFullPage = {
                                    editingCase = activeBundle.case
                                    isFormOpen = true
                                }
                            )
                        }

                        else -> {
                            CaseDirectoryScreen(
                                cases = cases,
                                onOpenCase = { id -> activeCaseId = id },
                                onCreateNewCase = {
                                    editingCase = null
                                    isFormOpen = true
                                }
                            )
                        }
                    }

                    // Branded Launcher Splash Screen Overlay
                    AnimatedVisibility(visible = showSplash, exit = fadeOut()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(NavyPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(
                                    color = TealAccent,
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier.size(84.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Gavel,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(44.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "MACT MedLegal Pro",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "MCOP Litigation • Injury & Scene Visualizer • Valuation",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
