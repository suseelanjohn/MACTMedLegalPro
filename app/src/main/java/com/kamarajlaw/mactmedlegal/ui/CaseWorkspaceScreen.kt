package com.kamarajlaw.mactmedlegal.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.kamarajlaw.mactmedlegal.data.*
import com.kamarajlaw.mactmedlegal.domain.ValuationEngine
import com.kamarajlaw.mactmedlegal.ui.theme.*
import com.kamarajlaw.mactmedlegal.util.ClientActions
import com.kamarajlaw.mactmedlegal.util.CourtPdfGenerator
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

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
    var selectedTab by remember { mutableIntStateOf(0) }
    var showEditDialog by remember { mutableStateOf(false) }
    val isFatal = bundle.case.claimCategory == ClaimCategory.FATAL_DEATH

    // Scan tagging dialog state
    var pendingScannedPath by remember { mutableStateOf<String?>(null) }
    var pendingPageCount by remember { mutableIntStateOf(1) }

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
                pendingScannedPath = dest.absolutePath
                pendingPageCount = pdf.pageCount
            }
        }
    }

    fun launchScanner() {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(25)
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text(bundle.case.mcopNumber, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(
                            "${bundle.case.petitionerName} • ${bundle.case.clientPhone.ifBlank { "No Phone" }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFCBD5E1),
                            maxLines = 1
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Case")
                    }
                    IconButton(onClick = { ClientActions.dialClient(context, bundle.case.clientPhone) }) {
                        Icon(Icons.Default.Call, contentDescription = "Call")
                    }
                    IconButton(onClick = {
                        ClientActions.openWhatsApp(context, bundle.case.clientPhone, bundle.case.mcopNumber, bundle.case.petitionerName)
                    }) {
                        Icon(Icons.Default.Chat, contentDescription = "WhatsApp")
                    }
                    IconButton(onClick = { CourtPdfGenerator.generateAndShareBrief(context, bundle, report) }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Export Brief")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = CardWhite) {
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
                    label = { Text("Docs (${bundle.documents.size})") }
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, null) },
                    label = { Text("Expenses") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(SlateBg)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                0 -> AnalysisTab(bundle = bundle, report = report, dao = dao, onEditFacts = { showEditDialog = true })
                1 -> if (isFatal) HeirsTab(bundle = bundle, report = report, dao = dao) else InjuriesTab(bundle = bundle, dao = dao)
                2 -> SceneTab(bundle = bundle, dao = dao)
                3 -> DocsTab(bundle = bundle, dao = dao, onScanClick = { launchScanner() })
                4 -> ExpensesTab(bundle = bundle, report = report, dao = dao)
            }
        }
    }

    if (showEditDialog) {
        CaseIntakeOrEditDialog(
            existingCase = bundle.case,
            onDismiss = { showEditDialog = false },
            onSave = { updated ->
                scope.launch {
                    dao.upsertCase(updated)
                    showEditDialog = false
                }
            }
        )
    }

    pendingScannedPath?.let { path ->
        var docTitle by remember { mutableStateOf("Scanned Court Record") }
        var exhibitTag by remember { mutableStateOf("Ex.P${bundle.documents.size + 1}") }
        var isMedicalBill by remember { mutableStateOf(false) }
        var billAmtText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { pendingScannedPath = null },
            title = { Text("Tag Scanned Exhibit ($pendingPageCount Pages)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = exhibitTag, onValueChange = { exhibitTag = it }, label = { Text("Exhibit Number (e.g., Ex.P1 FIR / Ex.P8 Bill)") })
                    OutlinedTextField(value = docTitle, onValueChange = { docTitle = it }, label = { Text("Document Title") })
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isMedicalBill, onCheckedChange = { isMedicalBill = it })
                        Text("This is a Medical Bill Voucher (Sync to Valuation)")
                    }
                    if (isMedicalBill) {
                        OutlinedTextField(value = billAmtText, onValueChange = { billAmtText = it }, label = { Text("Verified Bill Amount (₹)") })
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        dao.insertDocument(
                            CaseDocumentEntity(
                                caseId = bundle.case.id,
                                title = docTitle.trim().ifBlank { "Exhibit" },
                                exhibitNumber = exhibitTag.trim().ifBlank { "Ex.P${bundle.documents.size + 1}" },
                                category = if (isMedicalBill) "MEDICAL_BILL" else "COURT_RECORD",
                                billAmount = if (isMedicalBill) (billAmtText.toDoubleOrNull() ?: 0.0) else 0.0,
                                localFilePath = path,
                                pageCount = pendingPageCount
                            )
                        )
                        pendingScannedPath = null
                    }
                }) { Text("Save to Vault") }
            }
        )
    }
}

@Composable
private fun AnalysisTab(
    bundle: FullCaseBundle,
    report: com.kamarajlaw.mactmedlegal.domain.CaseAnalysisReport,
    dao: McopDao,
    onEditFacts: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val c = bundle.case

    // 1. 100-Point Score Map & Merits Card
    Card(colors = CardDefaults.cardColors(containerColor = CardWhite), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("100-Point Evidence Score Map", fontWeight = FontWeight.Bold, color = NavyPrimary)
                    Text(report.fitnessVerdict, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TealAccent)
                }
                OutlinedButton(onClick = onEditFacts) { Text("Edit Facts") }
            }

            ScoreBar("Liability & FIR Strength", report.liabilityScore, 30)
            ScoreBar("Policy & Statutory Compliance", report.statutoryScore, 25)
            ScoreBar("Medical & Disability Proof", report.medicalScore, 30)
            ScoreBar("Income & Dependency Proof", report.incomeScore, 15)
        }
    }

    // 2. Anticipated Insurer Defences
    Card(colors = CardDefaults.cardColors(containerColor = CardWhite), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Anticipated Insurer Defences (${report.defences.size})", fontWeight = FontWeight.Bold, color = NavyPrimary)
            if (report.defences.isEmpty()) {
                Text("No major statutory defence flags triggered on current records.", style = MaterialTheme.typography.bodySmall, color = TealAccent)
            } else {
                report.defences.forEach { def ->
                    Surface(color = AmberContainer.copy(alpha = 0.55f), shape = RoundedCornerShape(10.dp)) {
                        Column(Modifier.padding(10.dp)) {
                            Text(def.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = Color(0xFF92400E))
                            Text("Precedent: ${def.legalPrinciple}", style = MaterialTheme.typography.labelSmall, color = NavyPrimary)
                            Text("Rebuttal: ${def.counterStrategy}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    // 3. Projection A — Strict Tribunal Statutory Projection + Sec. 171 Interest Toggle
    Card(colors = CardDefaults.cardColors(containerColor = CardWhite), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Projection A — Tribunal Award", fontWeight = FontWeight.Bold, color = NavyPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = c.interestRatePercent == 7.5,
                        onClick = { scope.launch { dao.upsertCase(c.copy(interestRatePercent = 7.5)) } },
                        label = { Text("7.5%") }
                    )
                    FilterChip(
                        selected = c.interestRatePercent == 9.0,
                        onClick = { scope.launch { dao.upsertCase(c.copy(interestRatePercent = 9.0)) } },
                        label = { Text("9.0%") }
                    )
                }
            }
            Text(report.projectionA.adjustmentNote, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            LedgerRow("Loss of Future Earning / Dependency", report.projectionA.lossOfEarningOrDependency)
            LedgerRow("Verified Medical Bills (Ex.P8)", report.projectionA.verifiedMedicalBills)
            if (c.claimCategory == ClaimCategory.FATAL_DEATH) {
                LedgerRow("Consortium + Estate + Funeral", report.projectionA.conventionalHeads)
            } else {
                LedgerRow("Pain & Suffering", report.projectionA.painAndSuffering)
                LedgerRow("Treatment Loss (${c.treatmentMonths}m) + Attendant", report.projectionA.lossOfIncomeDuringTreatment + report.projectionA.attendantAndNourishment)
            }
            HorizontalDivider(color = SlateBorder)
            LedgerRow("Tribunal Principal Award", report.projectionA.principalAward, bold = true)
            LedgerRow("Accrued Sec. 171 Interest (${String.format("%.1f", report.elapsedYears)} yrs)", report.projectionA.accruedInterest)
            LedgerRow("Projection A Gross Realization", report.projectionA.grossTotal, bold = true, highlight = NavyPrimary)
        }
    }

    // 4. Projection B — Expense-Absorbing Target Projection
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDFA)), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Projection B — Expense-Absorbing Target", fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
            Text(report.projectionB.adjustmentNote, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            if (c.claimCategory == ClaimCategory.FATAL_DEATH) {
                LedgerRow("Adjusted Monthly Income Basis", report.projectionB.assessedMonthlyIncome)
            } else if (c.claimCategory == ClaimCategory.INJURY_FUNCTIONAL_DISABILITY) {
                Text(
                    "Projected Functional Disability: ${report.projectionB.effectiveDisabilityPercent.toInt()}% (Record: ${report.projectionA.effectiveDisabilityPercent.toInt()}%)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TealAccent
                )
            }
            LedgerRow("Projection B Principal", report.projectionB.principalAward)
            LedgerRow("Projection B Accrued Interest", report.projectionB.accruedInterest)
            LedgerRow("Projection B Gross Target", report.projectionB.grossTotal, bold = true, highlight = Color(0xFF065F46))
        }
    }

    // 5. Advocate Fee & Client Collection Box
    var feeInputText by remember(c.id, c.professionalFeeValue) { mutableStateOf(c.professionalFeeValue.toInt().toString()) }
    Card(colors = CardDefaults.cardColors(containerColor = NavyPrimary), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Advocate Fee & Client Collection Summary", fontWeight = FontWeight.Bold, color = Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = c.isFeePercentage,
                    onClick = { scope.launch { dao.upsertCase(c.copy(isFeePercentage = true)) } },
                    label = { Text("% Fee") }
                )
                FilterChip(
                    selected = !c.isFeePercentage,
                    onClick = { scope.launch { dao.upsertCase(c.copy(isFeePercentage = false)) } },
                    label = { Text("Flat ₹") }
                )
                OutlinedTextField(
                    value = feeInputText,
                    onValueChange = {
                        feeInputText = it
                        it.toDoubleOrNull()?.let { v -> scope.launch { dao.upsertCase(c.copy(professionalFeeValue = v)) } }
                    },
                    modifier = Modifier.width(110.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White, focusedContainerColor = Color.White),
                    singleLine = true
                )
            }
            LedgerRowWhite("Advocate Professional Fee", report.advocateFeeAmount)
            LedgerRowWhite("Unrecovered Office Expenses", report.netUnrecoveredExpenses)
            HorizontalDivider(color = Color.White.copy(alpha = 0.25f))
            LedgerRowWhite("Total Collectible from Client", report.totalCollectibleFromClient, bold = true)
            LedgerRowWhite("Net Take-Home for Petitioner(s)", report.netTakeHomeForClient, bold = true)
        }
    }
}

@Composable
private fun InjuriesTab(bundle: FullCaseBundle, dao: McopDao) {
    val scope = rememberCoroutineScope()
    var isPosterior by remember { mutableStateOf(false) }
    var selectedPin by remember { mutableStateOf<InjuryPinEntity?>(null) }
    var pendingTap by remember { mutableStateOf<Triple<Float, Float, String>?>(null) }

    Card(colors = CardDefaults.cardColors(containerColor = CardWhite), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("2D Anatomical Injury Map", fontWeight = FontWeight.Bold, color = NavyPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = !isPosterior, onClick = { isPosterior = false }, label = { Text("Anterior") })
                    FilterChip(selected = isPosterior, onClick = { isPosterior = true }, label = { Text("Posterior") })
                }
            }
            Text("Tap anywhere on the body diagram to pin a fracture, laceration, or nerve injury.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)

            AnatomicalBodyCanvas(
                pins = bundle.injuries,
                isPosterior = isPosterior,
                selectedPinId = selectedPin?.id,
                onAddPin = { nx, ny, region -> pendingTap = Triple(nx, ny, region) },
                onSelectPin = { selectedPin = it }
            )
        }
    }

    bundle.injuries.forEachIndexed { idx, pin ->
        Card(colors = CardDefaults.cardColors(containerColor = CardWhite), shape = RoundedCornerShape(14.dp)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("#${idx + 1} [${pin.exhibitTag}] ${pin.regionName} (${if (pin.isPosterior) "Back" else "Front"})", fontWeight = FontWeight.Bold)
                    Text("${pin.diagnosis} • ${pin.severity}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                IconButton(onClick = { scope.launch { dao.deleteInjury(pin) } }) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Pin", tint = CrimsonDanger)
                }
            }
        }
    }

    pendingTap?.let { (nx, ny, defaultRegion) ->
        var region by remember { mutableStateOf(defaultRegion) }
        var diagnosis by remember { mutableStateOf("") }
        var severity by remember { mutableStateOf("GRIEVOUS") }
        var exhibit by remember { mutableStateOf("Ex.P${bundle.injuries.size + 3}") }

        AlertDialog(
            onDismissRequest = { pendingTap = null },
            title = { Text("Log Anatomical Injury Pin", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = region, onValueChange = { region = it }, label = { Text("Anatomical Region") })
                    OutlinedTextField(value = diagnosis, onValueChange = { diagnosis = it }, label = { Text("Clinical Diagnosis (e.g., Comminuted Tibia Fracture)") })
                    OutlinedTextField(value = exhibit, onValueChange = { exhibit = it }, label = { Text("Exhibit Reference") })
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("GRIEVOUS", "SIMPLE", "NERVE").forEach { sev ->
                            FilterChip(selected = severity == sev, onClick = { severity = sev }, label = { Text(sev) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        dao.insertInjury(
                            InjuryPinEntity(
                                caseId = bundle.case.id,
                                isPosterior = isPosterior,
                                normX = nx,
                                normY = ny,
                                regionName = region.trim().ifBlank { defaultRegion },
                                diagnosis = diagnosis.trim().ifBlank { "Traumatic Injury" },
                                severity = severity,
                                exhibitTag = exhibit.trim().ifBlank { "Ex.P3" }
                            )
                        )
                        pendingTap = null
                    }
                }) { Text("Pin Injury") }
            }
        )
    }
}

@Composable
private fun HeirsTab(
    bundle: FullCaseBundle,
    report: com.kamarajlaw.mactmedlegal.domain.CaseAnalysisReport,
    dao: McopDao
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var relation by remember { mutableStateOf("Spouse") }
    var ageText by remember { mutableStateOf("32") }
    var shareText by remember { mutableStateOf("50") }

    val totalSharePct = bundle.heirs.sumOf { it.sharePercent }

    Card(colors = CardDefaults.cardColors(containerColor = CardWhite), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Legal Heirs Hierarchy & Apportionment Tree", fontWeight = FontWeight.Bold, color = NavyPrimary)
            Text(
                "Allocated Total: ${totalSharePct.toInt()}%  |  Pranay Sethi Consortium: ₹48,400 per claimant",
                style = MaterialTheme.typography.bodySmall,
                color = if (totalSharePct.toInt() == 100) TealAccent else AmberWarn
            )
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Claimant / Legal Heir Name") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = relation, onValueChange = { relation = it }, label = { Text("Relation") }, modifier = Modifier.weight(1.3f))
                OutlinedTextField(value = ageText, onValueChange = { ageText = it }, label = { Text("Age") }, modifier = Modifier.weight(0.8f))
                OutlinedTextField(value = shareText, onValueChange = { shareText = it }, label = { Text("Share %") }, modifier = Modifier.weight(0.9f))
            }
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        scope.launch {
                            dao.upsertHeir(
                                LegalHeirEntity(
                                    caseId = bundle.case.id,
                                    name = name.trim(),
                                    relation = relation.trim(),
                                    age = ageText.toIntOrNull() ?: 30,
                                    sharePercent = shareText.toDoubleOrNull() ?: 50.0
                                )
                            )
                            name = ""
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("+ Add Legal Heir") }
        }
    }

    bundle.heirs.forEach { heir ->
        val shareProjA = report.projectionA.grossTotal * (heir.sharePercent / 100.0)
        val shareProjB = report.projectionB.grossTotal * (heir.sharePercent / 100.0)
        Card(colors = CardDefaults.cardColors(containerColor = CardWhite), shape = RoundedCornerShape(14.dp)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${heir.name} (${heir.relation}, Age ${heir.age})", fontWeight = FontWeight.Bold)
                    Text("Apportionment: ${heir.sharePercent.toInt()}%  •  Proj A: ${ClientActions.formatInr(shareProjA)}", style = MaterialTheme.typography.bodySmall)
                    Text("Proj B Share: ${ClientActions.formatInr(shareProjB)}", style = MaterialTheme.typography.labelMedium, color = TealAccent, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { scope.launch { dao.deleteHeir(heir) } }) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Remove Heir", tint = CrimsonDanger)
                }
            }
        }
    }
}

@Composable
private fun SceneTab(bundle: FullCaseBundle, dao: McopDao) {
    val scope = rememberCoroutineScope()
    var selectedItem by remember { mutableStateOf<SceneItemEntity?>(null) }
    var notes by remember(bundle.case.id) { mutableStateOf(bundle.case.spotMahazarNotes) }

    Card(colors = CardDefaults.cardColors(containerColor = CardWhite), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Spot Mahazar (Ex.P2) Rough Sketch Reconstruction", fontWeight = FontWeight.Bold, color = NavyPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("TWO_LANE" to "2-Lane", "FOUR_LANE" to "4-Lane", "T_JUNCTION" to "T-Jn", "CROSSROADS" to "4-Way")
                    .forEach { (layout, label) ->
                        FilterChip(
                            selected = bundle.case.roadLayout == layout,
                            onClick = { scope.launch { dao.upsertCase(bundle.case.copy(roadLayout = layout)) } },
                            label = { Text(label) }
                        )
                    }
            }

            AccidentSceneCanvas(
                roadLayout = bundle.case.roadLayout,
                items = bundle.sceneItems,
                selectedItemId = selectedItem?.id,
                onSelectItem = { selectedItem = it },
                onMoveItem = { item, nx, ny ->
                    scope.launch { dao.upsertSceneItem(item.copy(normX = nx, normY = ny)) }
                }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = {
                    scope.launch { dao.upsertSceneItem(SceneItemEntity(caseId = bundle.case.id, itemType = "LORRY", label = "Offending", normX = 0.42f, normY = 0.42f)) }
                }, modifier = Modifier.weight(1f)) { Text("+ Lorry") }
                Button(onClick = {
                    scope.launch { dao.upsertSceneItem(SceneItemEntity(caseId = bundle.case.id, itemType = "BIKE", label = "Petitioner", normX = 0.62f, normY = 0.58f)) }
                }, modifier = Modifier.weight(1f)) { Text("+ Bike") }
                OutlinedButton(onClick = {
                    scope.launch { dao.upsertSceneItem(SceneItemEntity(caseId = bundle.case.id, itemType = "IMPACT_X", label = "Impact X", normX = 0.52f, normY = 0.50f)) }
                }, modifier = Modifier.weight(1f)) { Text("+ Impact") }
            }

            selectedItem?.let { sel ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = {
                        val updated = sel.copy(rotationDeg = (sel.rotationDeg + 30f) % 360f)
                        selectedItem = updated
                        scope.launch { dao.upsertSceneItem(updated) }
                    }) { Text("Rotate ${sel.label} 30°") }
                    TextButton(onClick = {
                        scope.launch {
                            dao.deleteSceneItem(sel)
                            selectedItem = null
                        }
                    }) { Text("Remove", color = CrimsonDanger) }
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = {
                    notes = it
                    scope.launch { dao.upsertCase(bundle.case.copy(spotMahazarNotes = it)) }
                },
                label = { Text("Spot Mahazar Landmarks & Width Notes (Ex.P2)") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DocsTab(bundle: FullCaseBundle, dao: McopDao, onScanClick: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Button(onClick = onScanClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Icon(Icons.Default.DocumentScanner, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Scan Court Document / Medical Bill (ML Kit)")
    }

    if (bundle.documents.isEmpty()) {
        Card(colors = CardDefaults.cardColors(containerColor = CardWhite), shape = RoundedCornerShape(14.dp)) {
            Text(
                "No scanned exhibits yet. Scan FIR, Wound Certificate, Disability Certificate, or Medical Bills (which auto-sum into Projections A & B).",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    } else {
        bundle.documents.forEach { doc ->
            Card(colors = CardDefaults.cardColors(containerColor = CardWhite), shape = RoundedCornerShape(14.dp)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${doc.exhibitNumber} — ${doc.title}", fontWeight = FontWeight.Bold)
                        val billNote = if (doc.billAmount > 0) " • Bill: ${ClientActions.formatInr(doc.billAmount)}" else ""
                        Text("${doc.pageCount} pages$billNote", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Row {
                        IconButton(onClick = { ClientActions.openFile(context, doc.localFilePath) }) {
                            Icon(Icons.Default.Visibility, contentDescription = "View", tint = NavyPrimary)
                        }
                        IconButton(onClick = { ClientActions.shareFile(context, doc.localFilePath, doc.title) }) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = TealAccent)
                        }
                        IconButton(onClick = { scope.launch { dao.deleteDocument(doc) } }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = CrimsonDanger)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpensesTab(
    bundle: FullCaseBundle,
    report: com.kamarajlaw.mactmedlegal.domain.CaseAnalysisReport,
    dao: McopDao
) {
    val scope = rememberCoroutineScope()
    var particulars by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var isAdvance by remember { mutableStateOf(false) }

    Card(colors = CardDefaults.cardColors(containerColor = CardWhite), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Case Expense & Advance Ledger", fontWeight = FontWeight.Bold, color = NavyPrimary)
            LedgerRow("Total Office Spend (Absorbed in Proj. B)", report.totalExpensesSpent)
            LedgerRow("Client Advances Received", report.totalAdvancesReceived)
            LedgerRow("Net Unrecovered Expenses", report.netUnrecoveredExpenses, bold = true, highlight = CrimsonDanger)

            HorizontalDivider(color = SlateBorder)
            OutlinedTextField(value = particulars, onValueChange = { particulars = it }, label = { Text("Particulars (Court Fee / Batta / Medical Board)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text("Amount (₹)") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !isAdvance, onClick = { isAdvance = false }, label = { Text("Office Spend") })
                FilterChip(selected = isAdvance, onClick = { isAdvance = true }, label = { Text("Client Advance") })
            }
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (particulars.isNotBlank() && amt > 0.0) {
                        scope.launch {
                            dao.insertExpense(CaseExpenseEntity(caseId = bundle.case.id, particulars = particulars.trim(), amount = amt, isClientAdvance = isAdvance))
                            particulars = ""
                            amountText = ""
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Record Entry") }
        }
    }

    bundle.expenses.forEach { exp ->
        Card(colors = CardDefaults.cardColors(containerColor = CardWhite), shape = RoundedCornerShape(14.dp)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(exp.particulars, fontWeight = FontWeight.Medium)
                    Text(if (exp.isClientAdvance) "Client Advance" else "Office Disbursement", style = MaterialTheme.typography.labelSmall, color = if (exp.isClientAdvance) TealAccent else TextSecondary)
                }
                Text(
                    "${if (exp.isClientAdvance) "+" else "-"} ${ClientActions.formatInr(exp.amount)}",
                    fontWeight = FontWeight.Bold,
                    color = if (exp.isClientAdvance) TealAccent else CrimsonDanger
                )
                IconButton(onClick = { scope.launch { dao.deleteExpense(exp) } }) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun ScoreBar(label: String, score: Int, maxScore: Int) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text("$score / $maxScore", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { (score.toFloat() / maxScore.toFloat()).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = NavyPrimary,
            trackColor = SlateBorder
        )
    }
}

@Composable
private fun LedgerRow(label: String, amount: Double, bold: Boolean = false, highlight: Color = TextPrimary) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, color = highlight, modifier = Modifier.weight(1f))
        Text(ClientActions.formatInr(amount), style = MaterialTheme.typography.bodySmall, fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold, color = highlight)
    }
}

@Composable
private fun LedgerRowWhite(label: String, amount: Double, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, color = Color.White, modifier = Modifier.weight(1f))
        Text(ClientActions.formatInr(amount), style = MaterialTheme.typography.bodySmall, fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold, color = Color.White)
    }
}
