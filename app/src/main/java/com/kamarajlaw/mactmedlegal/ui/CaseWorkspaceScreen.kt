package com.kamarajlaw.mactmedlegal.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import com.kamarajlaw.mactmedlegal.domain.CaseAnalysisReport
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
    onBack: () -> Unit,
    onEditCaseFullPage: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val report = remember(bundle) { ValuationEngine.analyze(bundle) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val isFatal = bundle.case.claimCategory == ClaimCategory.FATAL_DEATH

    // Pending Document Tagging State (for both Scanned & Attached files)
    var pendingFilePath by remember { mutableStateOf<String?>(null) }
    var pendingDefaultTitle by remember { mutableStateOf("Scanned Court Exhibit") }
    var pendingSourceType by remember { mutableStateOf("SCANNED") }
    var pendingMimeType by remember { mutableStateOf("application/pdf") }
    var pendingPageCount by remember { mutableIntStateOf(1) }

    // 1. ML Kit Camera Document Scanner
    val scannerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanResult?.pdf?.let { pdf ->
                val dir = File(context.filesDir, "case_docs").apply { mkdirs() }
                val dest = File(dir, "SCAN_${bundle.case.id}_${System.currentTimeMillis()}.pdf")
                context.contentResolver.openInputStream(pdf.uri)?.use { input ->
                    FileOutputStream(dest).use { output -> input.copyTo(output) }
                }
                pendingFilePath = dest.absolutePath
                pendingDefaultTitle = "Scanned Exhibit (${pdf.pageCount}p)"
                pendingSourceType = "SCANNED"
                pendingMimeType = "application/pdf"
                pendingPageCount = pdf.pageCount
            }
        }
    }

    // 2. Device Storage File Picker (Attach existing PDF / JPG / PNG)
    val attachLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val copied = ClientActions.copyUriToInternalCaseVault(context, bundle.case.id, uri)
            if (copied != null) {
                pendingFilePath = copied.first
                pendingDefaultTitle = copied.second
                pendingSourceType = "ATTACHED"
                pendingMimeType = copied.third
                pendingPageCount = 1
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

    val navItems = listOf(
        Triple("Analysis", Icons.Default.Analytics, 0),
        Triple(if (isFatal) "Heirs" else "Injuries", if (isFatal) Icons.Default.AccountTree else Icons.Default.AccessibilityNew, 1),
        Triple("Scene", Icons.Default.Map, 2),
        Triple("Docs (${bundle.documents.size})", Icons.Default.FolderShared, 3),
        Triple("Expenses", Icons.Default.AccountBalanceWallet, 4)
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 720.dp

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
                        IconButton(onClick = onEditCaseFullPage) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Full Case Form")
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
                if (!isTablet) {
                    NavigationBar(containerColor = CardWhite) {
                        navItems.forEach { (label, icon, index) ->
                            NavigationBarItem(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                icon = { Icon(icon, null) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Row(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(SlateBg)
            ) {
                if (isTablet) {
                    NavigationRail(containerColor = CardWhite) {
                        Spacer(Modifier.height(12.dp))
                        navItems.forEach { (label, icon, index) ->
                            NavigationRailItem(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                icon = { Icon(icon, null) },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (selectedTab) {
                        0 -> AnalysisTab(bundle = bundle, report = report, dao = dao, onEditFacts = onEditCaseFullPage)
                        1 -> if (isFatal) HeirsTab(bundle = bundle, report = report, dao = dao) else InjuriesTab(bundle = bundle, report = report, dao = dao)
                        2 -> SceneTab(bundle = bundle, dao = dao)
                        3 -> DocsTab(
                            bundle = bundle,
                            dao = dao,
                            onScanClick = { launchScanner() },
                            onAttachClick = { attachLauncher.launch(arrayOf("application/pdf", "image/*")) }
                        )
                        4 -> ExpensesTab(bundle = bundle, report = report, dao = dao)
                    }
                }
            }
        }
    }

    pendingFilePath?.let { path ->
        var docTitle by remember { mutableStateOf(pendingDefaultTitle) }
        var exhibitTag by remember { mutableStateOf("Ex.P${bundle.documents.size + 1}") }
        var docCategory by remember { mutableStateOf("COURT_RECORD") }
        var billAmtText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { pendingFilePath = null },
            title = {
                Text(
                    if (pendingSourceType == "SCANNED") "Save Scanned Exhibit ($pendingPageCount p)" else "Save Attached Document",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = exhibitTag, onValueChange = { exhibitTag = it }, label = { Text("Exhibit Tag (Ex.P1 FIR / Ex.P8 Bill)") })
                    OutlinedTextField(value = docTitle, onValueChange = { docTitle = it }, label = { Text("Document Title") })
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("COURT_RECORD" to "Court", "MEDICAL_RECORD" to "Medical", "MEDICAL_BILL" to "Bill ₹")
                            .forEach { (cat, lbl) ->
                                FilterChip(selected = docCategory == cat, onClick = { docCategory = cat }, label = { Text(lbl) })
                            }
                    }
                    if (docCategory == "MEDICAL_BILL") {
                        OutlinedTextField(value = billAmtText, onValueChange = { billAmtText = it }, label = { Text("Verified Medical Bill Amount (₹)") })
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
                                category = docCategory,
                                sourceType = pendingSourceType,
                                mimeType = pendingMimeType,
                                billAmount = if (docCategory == "MEDICAL_BILL") (billAmtText.toDoubleOrNull() ?: 0.0) else 0.0,
                                localFilePath = path,
                                pageCount = pendingPageCount
                            )
                        )
                        pendingFilePath = null
                    }
                }) { Text("Save Under Case") }
            }
        )
    }
}

@Composable
private fun AnalysisTab(
    bundle: FullCaseBundle,
    report: CaseAnalysisReport,
    dao: McopDao,
    onEditFacts: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val c = bundle.case

    Card(colors = CardDefaults.cardColors(containerColor = CardWhite), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("100-Point Evidence Score Map", fontWeight = FontWeight.Bold, color = NavyPrimary)
                    Text(report.fitnessVerdict, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TealAccent)
                }
                OutlinedButton(onClick = onEditFacts) { Text("Edit Case") }
            }

            ScoreBar("Liability & FIR Strength", report.liabilityScore, 30)
            ScoreBar("Policy & Statutory Compliance", report.statutoryScore, 25)
            ScoreBar("Medical & Disability Proof", report.medicalScore, 30)
            ScoreBar("Income & Dependency Proof", report.incomeScore, 15)
        }
    }

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
                LedgerRow("Loss of Income During Treatment (${c.treatmentMonths}m)", report.projectionA.lossOfIncomeDuringTreatment)
                LedgerRow("Attendant & Extra Nourishment", report.projectionA.attendantAndNourishment)
                LedgerRow("Transport, Damage & Future Medical", report.projectionA.transportAndFutureMedical)
            }
            HorizontalDivider(color = SlateBorder)
            LedgerRow("Tribunal Principal Award", report.projectionA.principalAward, bold = true)
            LedgerRow("Accrued Sec. 171 Interest (${String.format("%.1f", report.elapsedYears)} yrs)", report.projectionA.accruedInterest)
            LedgerRow("Projection A Gross Realization", report.projectionA.grossTotal, bold = true, highlight = NavyPrimary)
        }
    }

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
private fun InjuriesTab(bundle: FullCaseBundle, report: CaseAnalysisReport, dao: McopDao) {
    val scope = rememberCoroutineScope()
    val c = bundle.case
    var isPosterior by remember { mutableStateOf(false) }
    var selectedPin by remember { mutableStateOf<InjuryPinEntity?>(null) }
    var pendingTap by remember { mutableStateOf<Triple<Float, Float, String>?>(null) }

    // 1. Interactive 2D Anatomical Body Mapper
    Card(colors = CardDefaults.cardColors(containerColor = CardWhite), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("2D Anatomical Injury Map", fontWeight = FontWeight.Bold, color = NavyPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = !isPosterior, onClick = { isPosterior = false }, label = { Text("Anterior") })
                    FilterChip(selected = isPosterior, onClick = { isPosterior = true }, label = { Text("Posterior") })
                }
            }
            Text("Tap body to pin an injury and assign its disability %.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)

            AnatomicalBodyCanvas(
                pins = bundle.injuries,
                isPosterior = isPosterior,
                selectedPinId = selectedPin?.id,
                onAddPin = { nx, ny, region -> pendingTap = Triple(nx, ny, region) },
                onSelectPin = { selectedPin = it }
            )
        }
    }

    // 2. Pinned Injuries List with Per-Pin Disability
    bundle.injuries.forEachIndexed { idx, pin ->
        Card(colors = CardDefaults.cardColors(containerColor = CardWhite), shape = RoundedCornerShape(14.dp)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("#${idx + 1} [${pin.exhibitTag}] ${pin.regionName} (${if (pin.isPosterior) "Back" else "Front"})", fontWeight = FontWeight.Bold)
                    val disLabel = if (pin.pinDisabilityPercent > 0) " • Disability: ${pin.pinDisabilityPercent.toInt()}%" else ""
                    Text("${pin.diagnosis} • ${pin.severity}$disLabel", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                IconButton(onClick = { scope.launch { dao.deleteInjury(pin) } }) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Pin", tint = CrimsonDanger)
                }
            }
        }
    }

    // 3. Direct Disability & Other Compensation Heads Editor on the Same Page
    var funcDisStr by remember(c.id, c.functionalDisabilityPercent) { mutableStateOf(c.functionalDisabilityPercent.toInt().toString()) }
    var permDisStr by remember(c.id, c.permanentDisabilityPercent) { mutableStateOf(c.permanentDisabilityPercent.toInt().toString()) }
    var painStr by remember(c.id, c.painAndSufferingBase) { mutableStateOf(c.painAndSufferingBase.toInt().toString()) }
    var attendantStr by remember(c.id, c.attendantAndNourishmentBase) { mutableStateOf(c.attendantAndNourishmentBase.toInt().toString()) }
    var transportStr by remember(c.id, c.transportAndDamageBase) { mutableStateOf(c.transportAndDamageBase.toInt().toString()) }
    var futureMedStr by remember(c.id, c.futureMedicalExpensesBase) { mutableStateOf(c.futureMedicalExpensesBase.toInt().toString()) }
    var restMosStr by remember(c.id, c.treatmentMonths) { mutableStateOf(c.treatmentMonths.toString()) }

    Card(colors = CardDefaults.cardColors(containerColor = CardWhite), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Disability & Other Compensation Heads", fontWeight = FontWeight.Bold, color = NavyPrimary)
                if (report.summedPinDisability > 0) {
                    TextButton(onClick = {
                        funcDisStr = report.summedPinDisability.toInt().toString()
                        scope.launch { dao.upsertCase(c.copy(functionalDisabilityPercent = report.summedPinDisability)) }
                    }) {
                        Text("Apply Pin Sum (${report.summedPinDisability.toInt()}%)")
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = funcDisStr,
                    onValueChange = {
                        funcDisStr = it
                        it.toDoubleOrNull()?.let { v -> scope.launch { dao.upsertCase(c.copy(functionalDisabilityPercent = v)) } }
                    },
                    label = { Text("Functional Dis. %") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = permDisStr,
                    onValueChange = {
                        permDisStr = it
                        it.toDoubleOrNull()?.let { v -> scope.launch { dao.upsertCase(c.copy(permanentDisabilityPercent = v)) } }
                    },
                    label = { Text("Permanent Dis. %") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = restMosStr,
                    onValueChange = {
                        restMosStr = it
                        it.toIntOrNull()?.let { v -> scope.launch { dao.upsertCase(c.copy(treatmentMonths = v)) } }
                    },
                    label = { Text("Rest (Mos)") },
                    modifier = Modifier.weight(0.8f),
                    singleLine = true
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = painStr,
                    onValueChange = {
                        painStr = it
                        it.toDoubleOrNull()?.let { v -> scope.launch { dao.upsertCase(c.copy(painAndSufferingBase = v)) } }
                    },
                    label = { Text("Pain & Suffering ₹") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = attendantStr,
                    onValueChange = {
                        attendantStr = it
                        it.toDoubleOrNull()?.let { v -> scope.launch { dao.upsertCase(c.copy(attendantAndNourishmentBase = v)) } }
                    },
                    label = { Text("Attendant + Nourish ₹") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = transportStr,
                    onValueChange = {
                        transportStr = it
                        it.toDoubleOrNull()?.let { v -> scope.launch { dao.upsertCase(c.copy(transportAndDamageBase = v)) } }
                    },
                    label = { Text("Transport + Clothes ₹") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = futureMedStr,
                    onValueChange = {
                        futureMedStr = it
                        it.toDoubleOrNull()?.let { v -> scope.launch { dao.upsertCase(c.copy(futureMedicalExpensesBase = v)) } }
                    },
                    label = { Text("Future Medical ₹") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }
    }

    // 4. Overall Live Calculation Summary Card right on the Injuries Page
    Card(colors = CardDefaults.cardColors(containerColor = NavyPrimary), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Live Overall Compensation Calculation", fontWeight = FontWeight.Bold, color = Color.White)
            LedgerRowWhite("Loss of Future Earning (${report.projectionA.effectiveDisabilityPercent.toInt()}% FD)", report.projectionA.lossOfEarningOrDependency)
            LedgerRowWhite("Verified Medical Bills (Ex.P8)", report.projectionA.verifiedMedicalBills)
            LedgerRowWhite("Pain & Suffering", report.projectionA.painAndSuffering)
            LedgerRowWhite("Treatment Loss (${c.treatmentMonths}m) + Attendant", report.projectionA.lossOfIncomeDuringTreatment + report.projectionA.attendantAndNourishment)
            LedgerRowWhite("Transport, Damage & Future Medical", report.projectionA.transportAndFutureMedical)
            HorizontalDivider(color = Color.White.copy(alpha = 0.25f))
            LedgerRowWhite("Total Principal Award (Proj. A)", report.projectionA.principalAward, bold = true)
            LedgerRowWhite("Gross Award + ${c.interestRatePercent}% Interest", report.projectionA.grossTotal, bold = true)
            LedgerRowWhite("Proj. B Expense-Adjusted Target", report.projectionB.grossTotal, bold = true)
        }
    }

    // Pin Creation Dialog with Disability % Field
    pendingTap?.let { (nx, ny, defaultRegion) ->
        var region by remember { mutableStateOf(defaultRegion) }
        var diagnosis by remember { mutableStateOf("") }
        var severity by remember { mutableStateOf("GRIEVOUS") }
        var pinDisText by remember { mutableStateOf("10") }
        var exhibit by remember { mutableStateOf("Ex.P${bundle.injuries.size + 3}") }

        AlertDialog(
            onDismissRequest = { pendingTap = null },
            title = { Text("Pin Anatomical Injury & Disability", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = region, onValueChange = { region = it }, label = { Text("Anatomical Region") })
                    OutlinedTextField(value = diagnosis, onValueChange = { diagnosis = it }, label = { Text("Clinical Diagnosis (e.g., Femur Fracture)") })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = pinDisText, onValueChange = { pinDisText = it }, label = { Text("Disability %") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = exhibit, onValueChange = { exhibit = it }, label = { Text("Exhibit Tag") }, modifier = Modifier.weight(1f))
                    }
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
                        val addedDis = pinDisText.toDoubleOrNull() ?: 0.0
                        dao.insertInjury(
                            InjuryPinEntity(
                                caseId = bundle.case.id,
                                isPosterior = isPosterior,
                                normX = nx,
                                normY = ny,
                                regionName = region.trim().ifBlank { defaultRegion },
                                diagnosis = diagnosis.trim().ifBlank { "Traumatic Injury" },
                                severity = severity,
                                pinDisabilityPercent = addedDis,
                                exhibitTag = exhibit.trim().ifBlank { "Ex.P3" }
                            )
                        )
                        // Auto-sync functional disability if not manually overridden
                        val newTotalDis = bundle.injuries.sumOf { it.pinDisabilityPercent } + addedDis
                        if (newTotalDis > 0.0) {
                            dao.upsertCase(c.copy(functionalDisabilityPercent = newTotalDis))
                        }
                        pendingTap = null
                    }
                }) { Text("Save Injury Pin") }
            }
        )
    }
}

@Composable
private fun HeirsTab(
    bundle: FullCaseBundle,
    report: CaseAnalysisReport,
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
    var scenarioSummary by remember(bundle.case.id) { mutableStateOf(bundle.case.accidentScenarioSummary) }
    var notes by remember(bundle.case.id) { mutableStateOf(bundle.case.spotMahazarNotes) }

    Card(colors = CardDefaults.cardColors(containerColor = CardWhite), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Spot Mahazar (Ex.P2) Accident Reconstruction", fontWeight = FontWeight.Bold, color = NavyPrimary)

            // 1-Tap Scenario Presets for Rapid Court Reconstruction
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(onClick = {
                    scope.launch {
                        dao.clearScene(bundle.case.id)
                        dao.upsertCase(bundle.case.copy(roadLayout = "TWO_LANE", accidentScenarioSummary = "Head-On Collision: Offending Lorry crossed center line onto wrong side"))
                        dao.upsertSceneItem(SceneItemEntity(caseId = bundle.case.id, itemType = "LORRY", label = "Offending Lorry", normX = 0.44f, normY = 0.58f, rotationDeg = 180f))
                        dao.upsertSceneItem(SceneItemEntity(caseId = bundle.case.id, itemType = "BIKE", label = "Petitioner Bike", normX = 0.62f, normY = 0.60f, rotationDeg = 0f))
                        dao.upsertSceneItem(SceneItemEntity(caseId = bundle.case.id, itemType = "IMPACT_X", label = "Impact X", normX = 0.53f, normY = 0.59f))
                        dao.upsertSceneItem(SceneItemEntity(caseId = bundle.case.id, itemType = "SKID", label = "Skid 18ft", normX = 0.32f, normY = 0.54f))
                    }
                }) { Text("Preset: Head-On") }

                OutlinedButton(onClick = {
                    scope.launch {
                        dao.clearScene(bundle.case.id)
                        dao.upsertCase(bundle.case.copy(roadLayout = "FOUR_LANE", accidentScenarioSummary = "Rear-End Impact: Speeding Car rammed Petitioner Bike from behind"))
                        dao.upsertSceneItem(SceneItemEntity(caseId = bundle.case.id, itemType = "CAR", label = "Offending Car", normX = 0.44f, normY = 0.61f))
                        dao.upsertSceneItem(SceneItemEntity(caseId = bundle.case.id, itemType = "BIKE", label = "Petitioner Bike", normX = 0.58f, normY = 0.61f))
                        dao.upsertSceneItem(SceneItemEntity(caseId = bundle.case.id, itemType = "IMPACT_X", label = "Rear Impact", normX = 0.51f, normY = 0.61f))
                    }
                }) { Text("Preset: Rear-End") }

                OutlinedButton(onClick = {
                    scope.launch {
                        dao.clearScene(bundle.case.id)
                        dao.upsertCase(bundle.case.copy(roadLayout = "T_JUNCTION", accidentScenarioSummary = "T-Junction T-Bone: Lorry entered main highway without halting"))
                        dao.upsertSceneItem(SceneItemEntity(caseId = bundle.case.id, itemType = "LORRY", label = "Offending Lorry", normX = 0.50f, normY = 0.66f, rotationDeg = 270f))
                        dao.upsertSceneItem(SceneItemEntity(caseId = bundle.case.id, itemType = "BIKE", label = "Petitioner Bike", normX = 0.44f, normY = 0.56f))
                        dao.upsertSceneItem(SceneItemEntity(caseId = bundle.case.id, itemType = "IMPACT_X", label = "Junction Impact", normX = 0.48f, normY = 0.58f))
                    }
                }) { Text("Preset: T-Junction") }
            }

            // Road Layout Selector
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

            // Palette of Placeable Vehicles & Spot Markers
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(onClick = {
                    scope.launch { dao.upsertSceneItem(SceneItemEntity(caseId = bundle.case.id, itemType = "LORRY", label = "Lorry/Bus", normX = 0.40f, normY = 0.42f)) }
                }) { Text("+ Lorry") }
                Button(onClick = {
                    scope.launch { dao.upsertSceneItem(SceneItemEntity(caseId = bundle.case.id, itemType = "CAR", label = "Car/Auto", normX = 0.45f, normY = 0.58f)) }
                }) { Text("+ Car") }
                Button(onClick = {
                    scope.launch { dao.upsertSceneItem(SceneItemEntity(caseId = bundle.case.id, itemType = "BIKE", label = "Petitioner Bike", normX = 0.62f, normY = 0.58f)) }
                }) { Text("+ Bike") }
                OutlinedButton(onClick = {
                    scope.launch { dao.upsertSceneItem(SceneItemEntity(caseId = bundle.case.id, itemType = "PEDESTRIAN", label = "Pedestrian", normX = 0.55f, normY = 0.68f)) }
                }) { Text("+ Pedestrian") }
                OutlinedButton(onClick = {
                    scope.launch { dao.upsertSceneItem(SceneItemEntity(caseId = bundle.case.id, itemType = "IMPACT_X", label = "Impact X", normX = 0.52f, normY = 0.50f)) }
                }) { Text("+ Impact X") }
                OutlinedButton(onClick = {
                    scope.launch { dao.upsertSceneItem(SceneItemEntity(caseId = bundle.case.id, itemType = "SKID", label = "Skid Marks", normX = 0.35f, normY = 0.50f)) }
                }) { Text("+ Skid") }
                OutlinedButton(onClick = {
                    scope.launch { dao.upsertSceneItem(SceneItemEntity(caseId = bundle.case.id, itemType = "ARROW_R", label = "Direction", normX = 0.30f, normY = 0.60f)) }
                }) { Text("+ Arrow") }
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
                value = scenarioSummary,
                onValueChange = {
                    scenarioSummary = it
                    scope.launch { dao.upsertCase(bundle.case.copy(accidentScenarioSummary = it)) }
                },
                label = { Text("Manner of Accident (e.g., Wrong-side head-on collision)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = {
                    notes = it
                    scope.launch { dao.upsertCase(bundle.case.copy(spotMahazarNotes = it)) }
                },
                label = { Text("Spot Mahazar Landmarks & Road Width Notes (Ex.P2)") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DocsTab(
    bundle: FullCaseBundle,
    dao: McopDao,
    onScanClick: () -> Unit,
    onAttachClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onScanClick,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
        ) {
            Icon(Icons.Default.DocumentScanner, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Scan Doc")
        }
        OutlinedButton(
            onClick = onAttachClick,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.AttachFile, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Attach File")
        }
    }

    if (bundle.documents.isEmpty()) {
        Card(colors = CardDefaults.cardColors(containerColor = CardWhite), shape = RoundedCornerShape(14.dp)) {
            Text(
                "No scanned or attached documents saved under ${bundle.case.mcopNumber} yet. Use 'Scan Doc' (camera) or 'Attach File' (PDF/Images from phone) to store FIR, Wound Certificate, Disability Certificate, Policy, and Medical Bills.",
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${doc.exhibitNumber} — ${doc.title}", fontWeight = FontWeight.Bold)
                        }
                        val billNote = if (doc.billAmount > 0) " • Bill: ${ClientActions.formatInr(doc.billAmount)}" else ""
                        Text("${doc.sourceType} • ${doc.pageCount}p$billNote", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Row {
                        IconButton(onClick = { ClientActions.openFile(context, doc.localFilePath, doc.mimeType) }) {
                            Icon(Icons.Default.Visibility, contentDescription = "View", tint = NavyPrimary)
                        }
                        IconButton(onClick = { ClientActions.shareFile(context, doc.localFilePath, doc.title, doc.mimeType) }) {
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
    report: CaseAnalysisReport,
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
