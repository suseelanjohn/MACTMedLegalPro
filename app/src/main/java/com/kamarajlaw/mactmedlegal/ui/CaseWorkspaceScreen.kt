package com.kamarajlaw.mactmedlegal.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.kamarajlaw.mactmedlegal.data.*
import com.kamarajlaw.mactmedlegal.domain.ValuationEngine
import com.kamarajlaw.mactmedlegal.util.ClientActions
import com.kamarajlaw.mactmedlegal.util.CourtPdfGenerator
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseWorkspaceScreen(
    bundle: FullCaseBundle,
    dao: McopDao,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val report = remember(bundle) { ValuationEngine.analyze(bundle) }
    val inr = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 } }
    var selectedTab by remember { mutableIntStateOf(0) }
    val isFatal = bundle.case.claimCategory == ClaimCategory.FATAL_DEATH

    val scannerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanResult?.pdf?.let { pdf ->
                val dir = File(context.filesDir, "case_docs").apply { mkdirs() }
                val dest = File(dir, "DOC_${bundle.case.id}_${System.currentTimeMillis()}.pdf")
                context.contentResolver.openInputStream(pdf.uri)?.use { input ->
                    FileOutputStream(dest).use { output -> input.copyTo(output) }
                }
                scope.launch {
                    dao.insertDocument(
                        CaseDocumentEntity(
                            caseId = bundle.case.id,
                            title = "Scanned Exhibit (${pdf.pageCount}p)",
                            exhibitNumber = "Ex.P${bundle.documents.size + 1}",
                            category = "COURT_RECORD",
                            localFilePath = dest.absolutePath,
                            pageCount = pdf.pageCount
                        )
                    )
                }
            }
        }
    }

    fun launchScanner() {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(20)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
        val activity = context as? Activity ?: return
        GmsDocumentScanning.getClient(options).getStartScanIntent(activity)
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Directory")
                    }
                },
                title = {
                    Column {
                        Text(bundle.case.mcopNumber, fontWeight = FontWeight.Bold)
                        Text(bundle.case.petitionerName, style = MaterialTheme.typography.bodySmall)
                    }
                },
                actions = {
                    IconButton(onClick = { ClientActions.dialClient(context, bundle.case.clientPhone) }) {
                        Icon(Icons.Default.Call, contentDescription = "Call")
                    }
                    IconButton(onClick = { CourtPdfGenerator.generateAndShareBrief(context, bundle, report) }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Export Court Brief")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Analytics, null) },
                    label = { Text("Analysis") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(if (isFatal) Icons.Default.AccountTree else Icons.Default.AccessibilityNew, null) },
                    label = { Text(if (isFatal) "Heirs" else "Injuries") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Map, null) },
                    label = { Text("Scene") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.FolderShared, null) },
                    label = { Text("Docs") }
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.ReceiptLong, null) },
                    label = { Text("Expenses") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    Card {
                        Column(Modifier.padding(16.dp)) {
                            Text("Case Fitness & Evidence Score: ${report.totalScore}/100", fontWeight = FontWeight.Bold)
                            Text(report.fitnessVerdict, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Liability ${report.liabilityScore}/30 • Statutory ${report.statutoryScore}/25 • Medical ${report.medicalScore}/30 • Income ${report.incomeScore}/15",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    if (report.defences.isNotEmpty()) {
                        Card {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Anticipated Insurer Defences", fontWeight = FontWeight.Bold)
                                report.defences.forEach { d ->
                                    Text("• ${d.title} (${d.legalPrinciple})", fontWeight = FontWeight.SemiBold)
                                    Text(d.counterStrategy, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    Card {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Projection A — Tribunal Statutory Projection", fontWeight = FontWeight.Bold)
                            Text("Principal Award: ${inr.format(report.projectionA.principalAward)}")
                            Text("Accrued Interest (${bundle.case.interestRatePercent}% p.a.): ${inr.format(report.projectionA.accruedInterest)}")
                            Text("Gross Realization (A): ${inr.format(report.projectionA.grossTotal)}", fontWeight = FontWeight.Bold)
                        }
                    }

                    Card {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Projection B — Expense-Adjusted Target", fontWeight = FontWeight.Bold)
                            Text(report.projectionB.adjustmentNote, style = MaterialTheme.typography.bodySmall)
                            Text("Projection B Gross Target: ${inr.format(report.projectionB.grossTotal)}", fontWeight = FontWeight.Bold)
                            HorizontalDivider(Modifier.padding(vertical = 6.dp))
                            Text("Advocate Professional Fee: ${inr.format(report.advocateFeeAmount)}")
                            Text("Unrecovered Office Expenses: ${inr.format(report.netUnrecoveredExpenses)}")
                            Text(
                                "Total Collectible from Client: ${inr.format(report.totalCollectibleFromClient)}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("Net Take-Home for Petitioner(s): ${inr.format(report.netTakeHomeForClient)}")
                        }
                    }
                }

                1 -> {
                    if (isFatal) {
                        var heirName by remember { mutableStateOf("") }
                        var heirRelation by remember { mutableStateOf("Spouse") }
                        var heirShare by remember { mutableStateOf("50") }
                        Text("Legal Heirs Hierarchy & Tribunal Apportionment", fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = heirName, onValueChange = { heirName = it }, label = { Text("Heir Name") }, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = heirRelation, onValueChange = { heirRelation = it }, label = { Text("Relation") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = heirShare, onValueChange = { heirShare = it }, label = { Text("Share %") }, modifier = Modifier.weight(1f))
                        }
                        Button(onClick = {
                            if (heirName.isNotBlank()) {
                                scope.launch {
                                    dao.upsertHeir(LegalHeirEntity(caseId = bundle.case.id, name = heirName.trim(), relation = heirRelation.trim(), age = 30, sharePercent = heirShare.toDoubleOrNull() ?: 50.0))
                                    heirName = ""
                                }
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("+ Add Legal Heir") }

                        bundle.heirs.forEach { heir ->
                            Card {
                                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("${heir.name} (${heir.relation})", fontWeight = FontWeight.Bold)
                                        Text("Share ${heir.sharePercent.toInt()}%")
                                    }
                                    Text(inr.format(report.projectionA.grossTotal * (heir.sharePercent / 100.0)), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        var posterior by remember { mutableStateOf(false) }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = !posterior, onClick = { posterior = false }, label = { Text("Anterior") })
                            FilterChip(selected = posterior, onClick = { posterior = true }, label = { Text("Posterior") })
                        }
                        AnatomicalBodyCanvas(
                            pins = bundle.injuries,
                            isPosterior = posterior,
                            onAddPin = { nx, ny ->
                                scope.launch {
                                    dao.insertInjury(
                                        InjuryPinEntity(
                                            caseId = bundle.case.id,
                                            isPosterior = posterior,
                                            normX = nx,
                                            normY = ny,
                                            diagnosis = "Fracture / Trauma #${bundle.injuries.size + 1}",
                                            severity = "GRIEVOUS",
                                            exhibitTag = "Ex.P${bundle.injuries.size + 3}"
                                        )
                                    )
                                }
                            },
                            onSelectPin = {}
                        )
                    }
                }

                2 -> {
                    Text("Spot Mahazar (Ex.P2) Accident Reconstruction", fontWeight = FontWeight.Bold)
                    AccidentSceneCanvas(
                        roadLayout = bundle.case.roadLayout,
                        items = bundle.sceneItems,
                        onMoveItem = { item, nx, ny ->
                            scope.launch { dao.upsertSceneItem(item.copy(normX = nx, normY = ny)) }
                        }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            scope.launch {
                                dao.upsertSceneItem(SceneItemEntity(caseId = bundle.case.id, itemType = "LORRY", label = "Offending", normX = 0.45f, normY = 0.45f))
                            }
                        }) { Text("+ Vehicle") }
                        Button(onClick = {
                            scope.launch {
                                dao.upsertSceneItem(SceneItemEntity(caseId = bundle.case.id, itemType = "BIKE", label = "Petitioner", normX = 0.60f, normY = 0.55f))
                            }
                        }) { Text("+ Bike") }
                        OutlinedButton(onClick = {
                            scope.launch {
                                dao.upsertSceneItem(SceneItemEntity(caseId = bundle.case.id, itemType = "IMPACT_X", label = "Impact", normX = 0.52f, normY = 0.50f))
                            }
                        }) { Text("+ Impact X") }
                    }
                }

                3 -> {
                    Button(onClick = { launchScanner() }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.DocumentScanner, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Scan Court Record / Medical Bill (ML Kit)")
                    }
                    bundle.documents.forEach { doc ->
                        Card {
                            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("${doc.exhibitNumber} — ${doc.title}", fontWeight = FontWeight.Bold)
                                    Text("${doc.pageCount} pages", style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = { ClientActions.shareFile(context, doc.localFilePath, doc.title) }) {
                                    Icon(Icons.Default.Share, contentDescription = "Share Document")
                                }
                            }
                        }
                    }
                }

                4 -> {
                    var particulars by remember { mutableStateOf("") }
                    var amountText by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = particulars,
                        onValueChange = { particulars = it },
                        label = { Text("Expense Particulars (e.g., Certified Copy / Batta)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount (₹)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (particulars.isNotBlank() && amt > 0.0) {
                                scope.launch {
                                    dao.insertExpense(CaseExpenseEntity(caseId = bundle.case.id, particulars = particulars, amount = amt))
                                    particulars = ""
                                    amountText = ""
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Log Expense (Auto-Adjusts Projection B)") }

                    bundle.expenses.forEach { exp ->
                        Card {
                            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(exp.particulars)
                                Text(inr.format(exp.amount), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
