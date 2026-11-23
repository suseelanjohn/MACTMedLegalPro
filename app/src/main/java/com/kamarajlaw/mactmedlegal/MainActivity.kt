package com.kamarajlaw.mactmedlegal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.kamarajlaw.mactmedlegal.data.AppDatabase
import com.kamarajlaw.mactmedlegal.ui.CaseDirectoryScreen
import com.kamarajlaw.mactmedlegal.ui.CaseIntakeOrEditDialog
import com.kamarajlaw.mactmedlegal.ui.CaseWorkspaceScreen
import com.kamarajlaw.mactmedlegal.ui.theme.MactMedLegalTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = AppDatabase.getInstance(this).mcopDao()

        setContent {
            MactMedLegalTheme {
                val cases by dao.observeAllCases().collectAsState(initial = emptyList())
                var activeCaseId by remember { mutableStateOf<Long?>(null) }
                var showNewDialog by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

                val activeBundle = cases.find { it.case.id == activeCaseId }

                if (activeBundle != null) {
                    CaseWorkspaceScreen(
                        bundle = activeBundle,
                        dao = dao,
                        onBack = { activeCaseId = null }
                    )
                } else {
                    CaseDirectoryScreen(
                        cases = cases,
                        onOpenCase = { id -> activeCaseId = id },
                        onCreateNewCase = { showNewDialog = true }
                    )
                }

                if (showNewDialog) {
                    CaseIntakeOrEditDialog(
                        existingCase = null,
                        onDismiss = { showNewDialog = false },
                        onSave = { newCase ->
                            scope.launch {
                                val id = dao.upsertCase(newCase)
                                showNewDialog = false
                                activeCaseId = id
                            }
                        }
                    )
                }
            }
        }
    }
}
