package com.kamarajlaw.mactmedlegal.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kamarajlaw.mactmedlegal.data.FullCaseBundle
import com.kamarajlaw.mactmedlegal.domain.ValuationEngine
import com.kamarajlaw.mactmedlegal.util.ClientActions
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseDirectoryScreen(
    cases: List<FullCaseBundle>,
    onOpenCase: (Long) -> Unit,
    onCreateNewCase: () -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val inr = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 } }

    val filtered = remember(cases, query) {
        if (query.isBlank()) cases
        else cases.filter {
            it.case.mcopNumber.contains(query, ignoreCase = true) ||
            it.case.petitionerName.contains(query, ignoreCase = true) ||
            it.case.clientPhone.contains(query, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MCOP Case Directory (${cases.size})", fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateNewCase,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New MCOP Case") }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search MCOP No., Petitioner, or Phone...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Text("No MCOP cases added yet", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Tap '+ New MCOP Case' to register a petition, score merits, and track expenses.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtered, key = { it.case.id }) { bundle ->
                        val report = remember(bundle) { ValuationEngine.analyze(bundle) }
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onOpenCase(bundle.case.id) }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(bundle.case.mcopNumber, fontWeight = FontWeight.Bold)
                                    AssistChip(
                                        onClick = { onOpenCase(bundle.case.id) },
                                        label = { Text("Score ${report.totalScore}/100") }
                                    )
                                }
                                Text(
                                    "${bundle.case.petitionerName} • Age ${bundle.case.age} • ${bundle.case.occupation}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    report.fitnessVerdict,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Proj B Target: ${inr.format(report.projectionB.grossTotal)}", fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "Client Phone: ${bundle.case.clientPhone.ifBlank { "Not added" }}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Row {
                                        IconButton(onClick = { ClientActions.dialClient(context, bundle.case.clientPhone) }) {
                                            Icon(Icons.Default.Call, contentDescription = "Call Client")
                                        }
                                        IconButton(onClick = {
                                            ClientActions.openWhatsApp(context, bundle.case.clientPhone, bundle.case.mcopNumber, bundle.case.petitionerName)
                                        }) {
                                            Icon(Icons.Default.Chat, contentDescription = "WhatsApp Client")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
