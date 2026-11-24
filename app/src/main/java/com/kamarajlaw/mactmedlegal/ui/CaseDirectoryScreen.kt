package com.kamarajlaw.mactmedlegal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kamarajlaw.mactmedlegal.data.ClaimCategory
import com.kamarajlaw.mactmedlegal.data.FullCaseBundle
import com.kamarajlaw.mactmedlegal.domain.CaseAnalysisReport
import com.kamarajlaw.mactmedlegal.domain.ValuationEngine
import com.kamarajlaw.mactmedlegal.ui.theme.*
import com.kamarajlaw.mactmedlegal.util.ClientActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseDirectoryScreen(
    cases: List<FullCaseBundle>,
    onOpenCase: (Long) -> Unit,
    onCreateNewCase: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf("ALL") }

    val analyzedCases = remember(cases) {
        cases.map { it to ValuationEngine.analyze(it) }
    }

    val totalPortfolioProjB = remember(analyzedCases) { analyzedCases.sumOf { it.second.projectionB.grossTotal } }
    val totalUnrecoveredSpend = remember(analyzedCases) { analyzedCases.sumOf { it.second.netUnrecoveredExpenses } }

    val filtered = remember(analyzedCases, query, filterMode) {
        analyzedCases.filter { (bundle, report) ->
            val matchesQuery = query.isBlank() ||
                bundle.case.mcopNumber.contains(query, ignoreCase = true) ||
                bundle.case.petitionerName.contains(query, ignoreCase = true) ||
                bundle.case.clientPhone.contains(query, ignoreCase = true) ||
                bundle.case.tribunalName.contains(query, ignoreCase = true) ||
                bundle.case.offendingVehicleReg.contains(query, ignoreCase = true)

            val matchesFilter = when (filterMode) {
                "INJURY" -> bundle.case.claimCategory != ClaimCategory.FATAL_DEATH && bundle.case.claimCategory != ClaimCategory.SECTION_164_NO_FAULT
                "FATAL" -> bundle.case.claimCategory == ClaimCategory.FATAL_DEATH
                "STRONG" -> report.isStrong
                "WEAK" -> !report.isStrong && !report.isModerate
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyPrimary,
                    titleContentColor = Color.White
                ),
                title = {
                    Column {
                        Text("MACT MedLegal Pro", fontWeight = FontWeight.Bold)
                        Text("Advocate MCOP Case Directory • ${cases.size} Dockets", style = MaterialTheme.typography.labelSmall, color = Color(0xFFCBD5E1))
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateNewCase,
                containerColor = NavyPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New MCOP Case") }
            )
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(SlateBg)
        ) {
            val isTablet = maxWidth >= 700.dp

            Column(modifier = Modifier.fillMaxSize()) {
                // Office-Wide Portfolio KPI Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NavyPrimary)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiChip("Active Dockets", "${cases.size}", Modifier.weight(1f))
                    KpiChip("Proj. B Portfolio", ClientActions.formatInr(totalPortfolioProjB), Modifier.weight(1.4f))
                    KpiChip("Due Expenses", ClientActions.formatInr(totalUnrecoveredSpend), Modifier.weight(1.2f))
                }

                // Search & Filter Bar
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search MCOP No., Petitioner, Vehicle Reg., or Phone...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White, focusedContainerColor = Color.White),
                    singleLine = true
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ALL" to "All (${cases.size})", "INJURY" to "Injury", "FATAL" to "Fatal / Death", "STRONG" to "Strong 75+", "WEAK" to "Weak (<50)")
                        .forEach { (key, label) ->
                            FilterChip(
                                selected = filterMode == key,
                                onClick = { filterMode = key },
                                label = { Text(label) }
                            )
                        }
                }

                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(28.dp)) {
                            Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(52.dp), tint = NavyPrimary)
                            Spacer(Modifier.height(10.dp))
                            Text("No MCOP Dockets Yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Tap '+ New MCOP Case' to open the full intake page, log injuries/disability, reconstruct spot mahazar, scan/attach exhibits, and compute dual projections.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                } else if (isTablet) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filtered, key = { it.first.case.id }) { (bundle, report) ->
                            DocketCard(bundle = bundle, report = report, onOpen = { onOpenCase(bundle.case.id) })
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filtered, key = { it.first.case.id }) { (bundle, report) ->
                            DocketCard(bundle = bundle, report = report, onOpen = { onOpenCase(bundle.case.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DocketCard(
    bundle: FullCaseBundle,
    report: CaseAnalysisReport,
    onOpen: () -> Unit
) {
    val context = LocalContext.current
    val badgeBg = when {
        report.isStrong -> TealContainer
        report.isModerate -> AmberContainer
        else -> CrimsonContainer
    }
    val badgeText = when {
        report.isStrong -> Color(0xFF065F46)
        report.isModerate -> Color(0xFF92400E)
        else -> CrimsonDanger
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(bundle.case.mcopNumber, fontWeight = FontWeight.Bold, color = NavyPrimary)
                    Text(bundle.case.tribunalName, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
                Surface(color = badgeBg, shape = RoundedCornerShape(8.dp)) {
                    Text(
                        "Score ${report.totalScore}/100",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = badgeText
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(
                "${bundle.case.petitionerName} • Age ${bundle.case.age} • ${bundle.case.occupation}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                report.fitnessVerdict,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = badgeText
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = SlateBorder)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Proj A: ${ClientActions.formatInr(report.projectionA.grossTotal)} | Proj B: ${ClientActions.formatInr(report.projectionB.grossTotal)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                    Text(
                        "Phone: ${bundle.case.clientPhone.ifBlank { "Not added" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Row {
                    IconButton(onClick = { ClientActions.dialClient(context, bundle.case.clientPhone) }) {
                        Icon(Icons.Default.Call, contentDescription = "Call", tint = NavyPrimary)
                    }
                    IconButton(onClick = {
                        ClientActions.openWhatsApp(context, bundle.case.clientPhone, bundle.case.mcopNumber, bundle.case.petitionerName)
                    }) {
                        Icon(Icons.Default.Chat, contentDescription = "WhatsApp", tint = TealAccent)
                    }
                    IconButton(onClick = {
                        ClientActions.copyText(context, "Phone", bundle.case.clientPhone)
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFFCBD5E1))
            Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
        }
    }
}
