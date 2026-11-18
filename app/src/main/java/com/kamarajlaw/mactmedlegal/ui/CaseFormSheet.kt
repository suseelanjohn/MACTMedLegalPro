package com.kamarajlaw.mactmedlegal.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kamarajlaw.mactmedlegal.data.CaseEntity
import com.kamarajlaw.mactmedlegal.data.ClaimCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewCaseDialog(
    onDismiss: () -> Unit,
    onSave: (CaseEntity) -> Unit
) {
    var mcopNo by remember { mutableStateOf("") }
    var tribunal by remember { mutableStateOf("MACT / Principal Sub Court") }
    var petitioner by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var ageText by remember { mutableStateOf("35") }
    var occupation by remember { mutableStateOf("") }
    var incomeText by remember { mutableStateOf("18000") }
    var disabilityText by remember { mutableStateOf("15") }
    var category by remember { mutableStateOf(ClaimCategory.INJURY_FUNCTIONAL_DISABILITY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register New MCOP Case", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = mcopNo, onValueChange = { mcopNo = it }, label = { Text("MCOP Number (e.g., MCOP No. 12/2026)") })
                OutlinedTextField(value = tribunal, onValueChange = { tribunal = it }, label = { Text("Tribunal / Court Name") })
                OutlinedTextField(value = petitioner, onValueChange = { petitioner = it }, label = { Text("Petitioner Name") })
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Client Phone Number") })
                OutlinedTextField(value = ageText, onValueChange = { ageText = it }, label = { Text("Age") })
                OutlinedTextField(value = occupation, onValueChange = { occupation = it }, label = { Text("Occupation") })
                OutlinedTextField(value = incomeText, onValueChange = { incomeText = it }, label = { Text("Claimed Monthly Income (₹)") })
                OutlinedTextField(value = disabilityText, onValueChange = { disabilityText = it }, label = { Text("Functional Disability (%)") })

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = category == ClaimCategory.INJURY_FUNCTIONAL_DISABILITY,
                        onClick = { category = ClaimCategory.INJURY_FUNCTIONAL_DISABILITY },
                        shape = SegmentedButtonDefaults.itemShape(0, 3)
                    ) { Text("Injury") }
                    SegmentedButton(
                        selected = category == ClaimCategory.FATAL_DEATH,
                        onClick = { category = ClaimCategory.FATAL_DEATH },
                        shape = SegmentedButtonDefaults.itemShape(1, 3)
                    ) { Text("Fatal") }
                    SegmentedButton(
                        selected = category == ClaimCategory.SECTION_164_NO_FAULT,
                        onClick = { category = ClaimCategory.SECTION_164_NO_FAULT },
                        shape = SegmentedButtonDefaults.itemShape(2, 3)
                    ) { Text("Sec.164") }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (mcopNo.isNotBlank() && petitioner.isNotBlank()) {
                    val now = System.currentTimeMillis()
                    onSave(
                        CaseEntity(
                            mcopNumber = mcopNo.trim(),
                            tribunalName = tribunal.trim(),
                            claimCategory = category,
                            petitionerName = petitioner.trim(),
                            clientPhone = phone.trim(),
                            age = ageText.toIntOrNull() ?: 35,
                            occupation = occupation.trim().ifBlank { "Self-Employed" },
                            claimedMonthlyIncome = incomeText.toDoubleOrNull() ?: 15000.0,
                            dateOfAccidentMillis = now - (60L * 24 * 60 * 60 * 1000),
                            dateOfFilingMillis = now - (30L * 24 * 60 * 60 * 1000),
                            permanentDisabilityPercent = disabilityText.toDoubleOrNull() ?: 0.0,
                            functionalDisabilityPercent = disabilityText.toDoubleOrNull() ?: 0.0
                        )
                    )
                }
            }) { Text("Save Case") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
