package com.kamarajlaw.mactmedlegal.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kamarajlaw.mactmedlegal.data.CaseEntity
import com.kamarajlaw.mactmedlegal.data.ClaimCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseIntakeOrEditDialog(
    existingCase: CaseEntity? = null,
    onDismiss: () -> Unit,
    onSave: (CaseEntity) -> Unit
) {
    var mcopNo by remember { mutableStateOf(existingCase?.mcopNumber ?: "") }
    var tribunal by remember { mutableStateOf(existingCase?.tribunalName ?: "MACT / Principal Sub Court") }
    var petitioner by remember { mutableStateOf(existingCase?.petitionerName ?: "") }
    var phone by remember { mutableStateOf(existingCase?.clientPhone ?: "") }
    var ageText by remember { mutableStateOf((existingCase?.age ?: 36).toString()) }
    var occupation by remember { mutableStateOf(existingCase?.occupation ?: "") }
    var incomeText by remember { mutableStateOf((existingCase?.claimedMonthlyIncome?.toInt() ?: 18000).toString()) }
    var permDisText by remember { mutableStateOf((existingCase?.permanentDisabilityPercent?.toInt() ?: 25).toString()) }
    var funcDisText by remember { mutableStateOf((existingCase?.functionalDisabilityPercent?.toInt() ?: 15).toString()) }
    var firDelayText by remember { mutableStateOf((existingCase?.firDelayDays ?: 0).toString()) }
    var category by remember { mutableStateOf(existingCase?.claimCategory ?: ClaimCategory.INJURY_FUNCTIONAL_DISABILITY) }

    // Statutory & Evidentiary Switches
    var hasInsurance by remember { mutableStateOf(existingCase?.hasActiveInsurance ?: true) }
    var hasDlBadge by remember { mutableStateOf(existingCase?.hasValidDriverLicenceAndBadge ?: true) }
    var hasChargeSheet by remember { mutableStateOf(existingCase?.hasChargeSheetFiled ?: true) }
    var hasIncomeProof by remember { mutableStateOf(existingCase?.hasIncomeProof ?: false) }
    var woreHelmet by remember { mutableStateOf(existingCase?.woreHelmetOrSeatbelt ?: true) }
    var isHeadOn by remember { mutableStateOf(existingCase?.isHeadOnCollision ?: false) }
    var isSalaried by remember { mutableStateOf(existingCase?.isSalariedPermanent ?: false) }
    var isBachelor by remember { mutableStateOf(existingCase?.isBachelorDeceased ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (existingCase == null) "Register New MCOP Case" else "Edit Case Facts & Statutory Flags",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = mcopNo, onValueChange = { mcopNo = it }, label = { Text("MCOP Number (e.g., MCOP 412/2026)") }, singleLine = true)
                OutlinedTextField(value = tribunal, onValueChange = { tribunal = it }, label = { Text("Tribunal / Court Name") }, singleLine = true)
                OutlinedTextField(value = petitioner, onValueChange = { petitioner = it }, label = { Text("Petitioner / Deceased Name") }, singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Client Phone Number (for Call/WhatsApp)") }, singleLine = true)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = ageText, onValueChange = { ageText = it }, label = { Text("Age") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = incomeText, onValueChange = { incomeText = it }, label = { Text("Monthly Income ₹") }, modifier = Modifier.weight(1.4f), singleLine = true)
                }
                OutlinedTextField(value = occupation, onValueChange = { occupation = it }, label = { Text("Occupation") }, singleLine = true)

                Text("Claim Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ClaimCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.shortLabel) }
                        )
                    }
                }

                if (category != ClaimCategory.FATAL_DEATH) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = permDisText, onValueChange = { permDisText = it }, label = { Text("Perm. Dis %") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = funcDisText, onValueChange = { funcDisText = it }, label = { Text("Func. Dis %") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                }

                OutlinedTextField(value = firDelayText, onValueChange = { firDelayText = it }, label = { Text("FIR Delay (Days)") }, singleLine = true)

                HorizontalDivider()
                Text("Statutory & Evidentiary Checklist", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                SwitchRow("Active Third-Party Insurance", hasInsurance) { hasInsurance = it }
                SwitchRow("Valid Driver Licence & Transport Badge", hasDlBadge) { hasDlBadge = it }
                SwitchRow("Police Charge Sheet / Final Report Filed", hasChargeSheet) { hasChargeSheet = it }
                SwitchRow("Documentary Income Proof (Salary/ITR)", hasIncomeProof) { hasIncomeProof = it }
                SwitchRow("Permanent Salaried Job (50%/30% FP)", isSalaried) { isSalaried = it }
                if (category == ClaimCategory.FATAL_DEATH) {
                    SwitchRow("Deceased Was Unmarried Bachelor (1/2 Cut)", isBachelor) { isBachelor = it }
                } else {
                    SwitchRow("Helmet / Seatbelt Compliance", woreHelmet) { woreHelmet = it }
                }
                SwitchRow("Head-On Collision (Contributory Risk)", isHeadOn) { isHeadOn = it }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (mcopNo.isNotBlank() && petitioner.isNotBlank()) {
                    val now = System.currentTimeMillis()
                    val permD = permDisText.toDoubleOrNull() ?: 0.0
                    val funcD = if (category == ClaimCategory.INJURY_FUNCTIONAL_DISABILITY) (funcDisText.toDoubleOrNull() ?: 0.0) else 0.0
                    onSave(
                        (existingCase ?: CaseEntity(
                            mcopNumber = "",
                            tribunalName = "",
                            claimCategory = category,
                            petitionerName = "",
                            clientPhone = "",
                            age = 35,
                            occupation = "",
                            claimedMonthlyIncome = 15000.0,
                            dateOfAccidentMillis = now - (90L * 24 * 60 * 60 * 1000),
                            dateOfFilingMillis = now - (45L * 24 * 60 * 60 * 1000)
                        )).copy(
                            mcopNumber = mcopNo.trim(),
                            tribunalName = tribunal.trim(),
                            claimCategory = category,
                            petitionerName = petitioner.trim(),
                            clientPhone = phone.trim(),
                            age = ageText.toIntOrNull() ?: 35,
                            occupation = occupation.trim().ifBlank { "Self-Employed" },
                            isSalariedPermanent = isSalaried,
                            isBachelorDeceased = isBachelor,
                            claimedMonthlyIncome = incomeText.toDoubleOrNull() ?: 15000.0,
                            hasIncomeProof = hasIncomeProof,
                            hasActiveInsurance = hasInsurance,
                            hasValidDriverLicenceAndBadge = hasDlBadge,
                            hasChargeSheetFiled = hasChargeSheet,
                            firDelayDays = firDelayText.toIntOrNull() ?: 0,
                            woreHelmetOrSeatbelt = woreHelmet,
                            isHeadOnCollision = isHeadOn,
                            permanentDisabilityPercent = permD,
                            functionalDisabilityPercent = funcD
                        )
                    )
                }
            }) { Text("Save Docket") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
