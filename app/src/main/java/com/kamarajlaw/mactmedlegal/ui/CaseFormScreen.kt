package com.kamarajlaw.mactmedlegal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kamarajlaw.mactmedlegal.data.CaseEntity
import com.kamarajlaw.mactmedlegal.data.ClaimCategory
import com.kamarajlaw.mactmedlegal.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CaseFormScreen(
    existingCase: CaseEntity? = null,
    onCancel: () -> Unit,
    onSave: (CaseEntity) -> Unit
) {
    val dateFmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val now = remember { System.currentTimeMillis() }

    // 1. Petition & Court Details
    var mcopNo by remember { mutableStateOf(existingCase?.mcopNumber ?: "") }
    var tribunal by remember { mutableStateOf(existingCase?.tribunalName ?: "MACT / Principal Sub Court") }
    var category by remember { mutableStateOf(existingCase?.claimCategory ?: ClaimCategory.INJURY_FUNCTIONAL_DISABILITY) }
    var accidentDateStr by remember {
        mutableStateOf(dateFmt.format(Date(existingCase?.dateOfAccidentMillis ?: (now - 90L * 86400000L))))
    }
    var filingDateStr by remember {
        mutableStateOf(dateFmt.format(Date(existingCase?.dateOfFilingMillis ?: (now - 45L * 86400000L))))
    }
    var interestRateStr by remember { mutableStateOf((existingCase?.interestRatePercent ?: 7.5).toString()) }

    // 2. Petitioner / Deceased Details
    var petitioner by remember { mutableStateOf(existingCase?.petitionerName ?: "") }
    var phone by remember { mutableStateOf(existingCase?.clientPhone ?: "") }
    var address by remember { mutableStateOf(existingCase?.petitionerAddress ?: "") }
    var ageText by remember { mutableStateOf((existingCase?.age ?: 36).toString()) }
    var occupation by remember { mutableStateOf(existingCase?.occupation ?: "") }
    var incomeText by remember { mutableStateOf((existingCase?.claimedMonthlyIncome?.toInt() ?: 18000).toString()) }
    var hasIncomeProof by remember { mutableStateOf(existingCase?.hasIncomeProof ?: false) }
    var isSalaried by remember { mutableStateOf(existingCase?.isSalariedPermanent ?: false) }
    var isBachelor by remember { mutableStateOf(existingCase?.isBachelorDeceased ?: false) }

    // 3. FIR, Criminal & Hospital Details
    var firNo by remember { mutableStateOf(existingCase?.firNumber ?: "") }
    var policeStation by remember { mutableStateOf(existingCase?.policeStation ?: "") }
    var sections by remember { mutableStateOf(existingCase?.bnsIpcSections ?: "281, 125(a) BNS") }
    var firDelayText by remember { mutableStateOf((existingCase?.firDelayDays ?: 0).toString()) }
    var hasChargeSheet by remember { mutableStateOf(existingCase?.hasChargeSheetFiled ?: true) }
    var hasMahazarMvi by remember { mutableStateOf(existingCase?.hasSpotMahazarAndMvi ?: true) }
    var hospitalName by remember { mutableStateOf(existingCase?.hospitalName ?: "") }
    var inpatientDaysText by remember { mutableStateOf((existingCase?.inpatientDays ?: 0).toString()) }

    // 4. Vehicle, Owner & Insurer Details
    var vehicleReg by remember { mutableStateOf(existingCase?.offendingVehicleReg ?: "") }
    var vehicleType by remember { mutableStateOf(existingCase?.offendingVehicleType ?: "Lorry / Heavy Goods") }
    var ownerName by remember { mutableStateOf(existingCase?.respondentOwnerName ?: "") }
    var insurerName by remember { mutableStateOf(existingCase?.insuranceCompanyName ?: "") }
    var policyNo by remember { mutableStateOf(existingCase?.policyNumber ?: "") }
    var hasInsurance by remember { mutableStateOf(existingCase?.hasActiveInsurance ?: true) }
    var hasDlBadge by remember { mutableStateOf(existingCase?.hasValidDriverLicenceAndBadge ?: true) }
    var woreHelmet by remember { mutableStateOf(existingCase?.woreHelmetOrSeatbelt ?: true) }
    var isHeadOn by remember { mutableStateOf(existingCase?.isHeadOnCollision ?: false) }

    // 5. Disability & Non-Pecuniary Heads
    var permDisText by remember { mutableStateOf((existingCase?.permanentDisabilityPercent?.toInt() ?: 25).toString()) }
    var funcDisText by remember { mutableStateOf((existingCase?.functionalDisabilityPercent?.toInt() ?: 15).toString()) }
    var treatmentMonthsText by remember { mutableStateOf((existingCase?.treatmentMonths ?: 3).toString()) }
    var painText by remember { mutableStateOf((existingCase?.painAndSufferingBase?.toInt() ?: 35000).toString()) }
    var attendantText by remember { mutableStateOf((existingCase?.attendantAndNourishmentBase?.toInt() ?: 25000).toString()) }
    var transportText by remember { mutableStateOf((existingCase?.transportAndDamageBase?.toInt() ?: 10000).toString()) }
    var futureMedText by remember { mutableStateOf((existingCase?.futureMedicalExpensesBase?.toInt() ?: 0).toString()) }

    fun parseDateOrDefault(str: String, fallback: Long): Long =
        runCatching { dateFmt.parse(str.trim())?.time ?: fallback }.getOrDefault(fallback)

    fun saveDocket() {
        if (mcopNo.isBlank() || petitioner.isBlank()) return
        val accMillis = parseDateOrDefault(accidentDateStr, now - 90L * 86400000L)
        val filMillis = parseDateOrDefault(filingDateStr, now - 45L * 86400000L)
        val permD = permDisText.toDoubleOrNull() ?: 0.0
        val funcD = if (category == ClaimCategory.INJURY_FUNCTIONAL_DISABILITY) (funcDisText.toDoubleOrNull() ?: 0.0) else 0.0

        val entity = (existingCase ?: CaseEntity(
            mcopNumber = "",
            tribunalName = "",
            claimCategory = category,
            petitionerName = "",
            clientPhone = "",
            age = 35,
            occupation = "",
            claimedMonthlyIncome = 15000.0,
            dateOfAccidentMillis = accMillis,
            dateOfFilingMillis = filMillis
        )).copy(
            mcopNumber = mcopNo.trim(),
            tribunalName = tribunal.trim(),
            claimCategory = category,
            dateOfAccidentMillis = accMillis,
            dateOfFilingMillis = filMillis,
            interestRatePercent = interestRateStr.toDoubleOrNull() ?: 7.5,
            petitionerName = petitioner.trim(),
            clientPhone = phone.trim(),
            petitionerAddress = address.trim(),
            age = ageText.toIntOrNull() ?: 35,
            occupation = occupation.trim().ifBlank { "Self-Employed" },
            isSalariedPermanent = isSalaried,
            isBachelorDeceased = isBachelor,
            claimedMonthlyIncome = incomeText.toDoubleOrNull() ?: 15000.0,
            hasIncomeProof = hasIncomeProof,
            firNumber = firNo.trim(),
            policeStation = policeStation.trim(),
            bnsIpcSections = sections.trim(),
            firDelayDays = firDelayText.toIntOrNull() ?: 0,
            hasChargeSheetFiled = hasChargeSheet,
            hasSpotMahazarAndMvi = hasMahazarMvi,
            hospitalName = hospitalName.trim(),
            inpatientDays = inpatientDaysText.toIntOrNull() ?: 0,
            offendingVehicleReg = vehicleReg.trim(),
            offendingVehicleType = vehicleType.trim(),
            respondentOwnerName = ownerName.trim(),
            insuranceCompanyName = insurerName.trim(),
            policyNumber = policyNo.trim(),
            hasActiveInsurance = hasInsurance,
            hasValidDriverLicenceAndBadge = hasDlBadge,
            woreHelmetOrSeatbelt = woreHelmet,
            isHeadOnCollision = isHeadOn,
            permanentDisabilityPercent = permD,
            functionalDisabilityPercent = funcD,
            treatmentMonths = treatmentMonthsText.toIntOrNull() ?: 3,
            painAndSufferingBase = painText.toDoubleOrNull() ?: 35000.0,
            attendantAndNourishmentBase = attendantText.toDoubleOrNull() ?: 25000.0,
            transportAndDamageBase = transportText.toDoubleOrNull() ?: 10000.0,
            futureMedicalExpensesBase = futureMedText.toDoubleOrNull() ?: 0.0
        )
        onSave(entity)
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
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text(
                            if (existingCase == null) "New MCOP Case Intake" else "Edit MCOP Case Record",
                            fontWeight = FontWeight.Bold
                        )
                        Text("Full Tribunal, FIR, Vehicle, Insurer & Compensation Setup", style = MaterialTheme.typography.labelSmall, color = Color(0xFFCBD5E1))
                    }
                },
                actions = {
                    TextButton(onClick = { saveDocket() }) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(4.dp))
                        Text("SAVE", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(SlateBg)
        ) {
            val isWideTablet = maxWidth >= 720.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (isWideTablet) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            SectionCard("1. Tribunal & Petition Details") {
                                OutlinedTextField(value = mcopNo, onValueChange = { mcopNo = it }, label = { Text("MCOP Number * (e.g. MCOP No. 412/2026)") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = tribunal, onValueChange = { tribunal = it }, label = { Text("Tribunal / Court Name") }, modifier = Modifier.fillMaxWidth())
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = accidentDateStr, onValueChange = { accidentDateStr = it }, label = { Text("Date of Accident (dd/MM/yyyy)") }, modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = filingDateStr, onValueChange = { filingDateStr = it }, label = { Text("Date of Filing (dd/MM/yyyy)") }, modifier = Modifier.weight(1f))
                                }
                                OutlinedTextField(value = interestRateStr, onValueChange = { interestRateStr = it }, label = { Text("Statutory Interest Rate (% p.a.)") }, modifier = Modifier.fillMaxWidth())
                                Text("Claim Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    ClaimCategory.entries.forEach { cat ->
                                        FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat.shortLabel) })
                                    }
                                }
                            }

                            SectionCard("3. FIR, Criminal Case & Hospitalization") {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = firNo, onValueChange = { firNo = it }, label = { Text("Crime / FIR No.") }, modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = policeStation, onValueChange = { policeStation = it }, label = { Text("Police Station") }, modifier = Modifier.weight(1.3f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = sections, onValueChange = { sections = it }, label = { Text("BNS / IPC Sections") }, modifier = Modifier.weight(1.4f))
                                    OutlinedTextField(value = firDelayText, onValueChange = { firDelayText = it }, label = { Text("FIR Delay (Days)") }, modifier = Modifier.weight(0.8f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = hospitalName, onValueChange = { hospitalName = it }, label = { Text("Treating Hospital Name") }, modifier = Modifier.weight(1.4f))
                                    OutlinedTextField(value = inpatientDaysText, onValueChange = { inpatientDaysText = it }, label = { Text("IP Days") }, modifier = Modifier.weight(0.8f))
                                }
                                FormSwitch("Charge Sheet / Final Report Filed", hasChargeSheet) { hasChargeSheet = it }
                                FormSwitch("Spot Mahazar (Ex.P2) & MVI Report Available", hasMahazarMvi) { hasMahazarMvi = it }
                            }
                        }

                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            SectionCard("2. Petitioner / Deceased & Income Details") {
                                OutlinedTextField(value = petitioner, onValueChange = { petitioner = it }, label = { Text("Petitioner / Deceased Full Name *") }, modifier = Modifier.fillMaxWidth())
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Client Phone Number") }, modifier = Modifier.weight(1.3f))
                                    OutlinedTextField(value = ageText, onValueChange = { ageText = it }, label = { Text("Age") }, modifier = Modifier.weight(0.7f))
                                }
                                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Residential Address") }, modifier = Modifier.fillMaxWidth())
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = occupation, onValueChange = { occupation = it }, label = { Text("Occupation") }, modifier = Modifier.weight(1.2f))
                                    OutlinedTextField(value = incomeText, onValueChange = { incomeText = it }, label = { Text("Monthly Income (₹)") }, modifier = Modifier.weight(1f))
                                }
                                FormSwitch("Documentary Income Proof (Salary Slip / ITR)", hasIncomeProof) { hasIncomeProof = it }
                                FormSwitch("Permanent Salaried Employment (50%/30% FP)", isSalaried) { isSalaried = it }
                                if (category == ClaimCategory.FATAL_DEATH) {
                                    FormSwitch("Deceased Was Unmarried Bachelor (1/2 Deduction)", isBachelor) { isBachelor = it }
                                }
                            }

                            SectionCard("4. Offending Vehicle, Owner & Insurer") {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = vehicleReg, onValueChange = { vehicleReg = it }, label = { Text("Offending Vehicle Reg. No.") }, modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = vehicleType, onValueChange = { vehicleType = it }, label = { Text("Vehicle Class") }, modifier = Modifier.weight(1f))
                                }
                                OutlinedTextField(value = ownerName, onValueChange = { ownerName = it }, label = { Text("Respondent 1 (Owner Name)") }, modifier = Modifier.fillMaxWidth())
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = insurerName, onValueChange = { insurerName = it }, label = { Text("Respondent 2 (Insurer)") }, modifier = Modifier.weight(1.2f))
                                    OutlinedTextField(value = policyNo, onValueChange = { policyNo = it }, label = { Text("Policy Number") }, modifier = Modifier.weight(1f))
                                }
                                FormSwitch("Active Third-Party Insurance Policy", hasInsurance) { hasInsurance = it }
                                FormSwitch("Valid Driving Licence & Transport Badge", hasDlBadge) { hasDlBadge = it }
                                if (category != ClaimCategory.FATAL_DEATH) {
                                    FormSwitch("Helmet / Seatbelt Compliance", woreHelmet) { woreHelmet = it }
                                }
                                FormSwitch("Head-On Collision (Contributory Negligence Check)", isHeadOn) { isHeadOn = it }
                            }
                        }
                    }
                } else {
                    // Phone Single-Column Full-Screen Form
                    SectionCard("1. Tribunal & Petition Details") {
                        OutlinedTextField(value = mcopNo, onValueChange = { mcopNo = it }, label = { Text("MCOP Number * (e.g. MCOP No. 412/2026)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = tribunal, onValueChange = { tribunal = it }, label = { Text("Tribunal / Court Name") }, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = accidentDateStr, onValueChange = { accidentDateStr = it }, label = { Text("Accident Date") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = filingDateStr, onValueChange = { filingDateStr = it }, label = { Text("Filing Date") }, modifier = Modifier.weight(1f))
                        }
                        OutlinedTextField(value = interestRateStr, onValueChange = { interestRateStr = it }, label = { Text("Sec. 171 Interest Rate (% p.a.)") }, modifier = Modifier.fillMaxWidth())
                        Text("Claim Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ClaimCategory.entries.forEach { cat ->
                                FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat.shortLabel) })
                            }
                        }
                    }

                    SectionCard("2. Petitioner / Deceased & Income Details") {
                        OutlinedTextField(value = petitioner, onValueChange = { petitioner = it }, label = { Text("Petitioner / Deceased Full Name *") }, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Client Phone Number") }, modifier = Modifier.weight(1.3f))
                            OutlinedTextField(value = ageText, onValueChange = { ageText = it }, label = { Text("Age") }, modifier = Modifier.weight(0.7f))
                        }
                        OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Residential Address") }, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = occupation, onValueChange = { occupation = it }, label = { Text("Occupation") }, modifier = Modifier.weight(1.2f))
                            OutlinedTextField(value = incomeText, onValueChange = { incomeText = it }, label = { Text("Monthly Income (₹)") }, modifier = Modifier.weight(1f))
                        }
                        FormSwitch("Documentary Income Proof (Salary / ITR)", hasIncomeProof) { hasIncomeProof = it }
                        FormSwitch("Permanent Salaried Job (50%/30% FP)", isSalaried) { isSalaried = it }
                        if (category == ClaimCategory.FATAL_DEATH) {
                            FormSwitch("Deceased Was Unmarried Bachelor (1/2 Cut)", isBachelor) { isBachelor = it }
                        }
                    }

                    SectionCard("3. FIR, Criminal Case & Hospitalization") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = firNo, onValueChange = { firNo = it }, label = { Text("Crime / FIR No.") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = policeStation, onValueChange = { policeStation = it }, label = { Text("Police Station") }, modifier = Modifier.weight(1.3f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = sections, onValueChange = { sections = it }, label = { Text("BNS / IPC Sections") }, modifier = Modifier.weight(1.4f))
                            OutlinedTextField(value = firDelayText, onValueChange = { firDelayText = it }, label = { Text("FIR Delay (Days)") }, modifier = Modifier.weight(0.8f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = hospitalName, onValueChange = { hospitalName = it }, label = { Text("Treating Hospital") }, modifier = Modifier.weight(1.4f))
                            OutlinedTextField(value = inpatientDaysText, onValueChange = { inpatientDaysText = it }, label = { Text("IP Days") }, modifier = Modifier.weight(0.8f))
                        }
                        FormSwitch("Charge Sheet / Final Report Filed", hasChargeSheet) { hasChargeSheet = it }
                        FormSwitch("Spot Mahazar (Ex.P2) & MVI Report Available", hasMahazarMvi) { hasMahazarMvi = it }
                    }

                    SectionCard("4. Offending Vehicle, Owner & Insurer") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = vehicleReg, onValueChange = { vehicleReg = it }, label = { Text("Vehicle Reg. No.") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = vehicleType, onValueChange = { vehicleType = it }, label = { Text("Vehicle Class") }, modifier = Modifier.weight(1f))
                        }
                        OutlinedTextField(value = ownerName, onValueChange = { ownerName = it }, label = { Text("Respondent 1 (Owner Name)") }, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = insurerName, onValueChange = { insurerName = it }, label = { Text("Respondent 2 (Insurer)") }, modifier = Modifier.weight(1.2f))
                            OutlinedTextField(value = policyNo, onValueChange = { policyNo = it }, label = { Text("Policy No.") }, modifier = Modifier.weight(1f))
                        }
                        FormSwitch("Active Third-Party Insurance Policy", hasInsurance) { hasInsurance = it }
                        FormSwitch("Valid Driving Licence & Transport Badge", hasDlBadge) { hasDlBadge = it }
                        if (category != ClaimCategory.FATAL_DEATH) {
                            FormSwitch("Helmet / Seatbelt Compliance", woreHelmet) { woreHelmet = it }
                        }
                        FormSwitch("Head-On Collision (Contributory Check)", isHeadOn) { isHeadOn = it }
                    }
                }

                if (category != ClaimCategory.FATAL_DEATH) {
                    SectionCard("5. Disability & Non-Pecuniary Compensation Heads") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = permDisText, onValueChange = { permDisText = it }, label = { Text("Permanent Dis. %") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = funcDisText, onValueChange = { funcDisText = it }, label = { Text("Functional Dis. %") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = treatmentMonthsText, onValueChange = { treatmentMonthsText = it }, label = { Text("Rest (Mos)") }, modifier = Modifier.weight(0.9f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = painText, onValueChange = { painText = it }, label = { Text("Pain & Suffering ₹") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = attendantText, onValueChange = { attendantText = it }, label = { Text("Attendant + Nourish ₹") }, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = transportText, onValueChange = { transportText = it }, label = { Text("Transport + Clothes ₹") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = futureMedText, onValueChange = { futureMedText = it }, label = { Text("Future Medical ₹") }, modifier = Modifier.weight(1f))
                        }
                    }
                }

                Button(
                    onClick = { saveDocket() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                ) {
                    Text(
                        if (existingCase == null) "Save MCOP Case & Open Workspace" else "Save Updated Case Record",
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = NavyPrimary, style = MaterialTheme.typography.titleSmall)
            content()
        }
    }
}

@Composable
private fun FormSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
