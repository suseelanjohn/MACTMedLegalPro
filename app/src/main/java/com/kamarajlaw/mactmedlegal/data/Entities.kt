package com.kamarajlaw.mactmedlegal.data

import androidx.room.*

enum class ClaimCategory(val label: String, val shortLabel: String) {
    INJURY_FUNCTIONAL_DISABILITY("Sec. 166 Functional Disability", "Injury (FD%)"),
    INJURY_GRIEVOUS_NO_DISABILITY("Sec. 166 Grievous (0% FD)", "Grievous 0%"),
    INJURY_SIMPLE("Sec. 166 Simple Injury", "Simple"),
    FATAL_DEATH("Sec. 166 Fatal / Death Claim", "Fatal / Death"),
    SECTION_164_NO_FAULT("Sec. 164 Fixed No-Fault", "Sec. 164")
}

@Entity(tableName = "cases")
data class CaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    // 1. Petition & Court Details
    val mcopNumber: String,
    val tribunalName: String,
    val claimCategory: ClaimCategory,
    val dateOfAccidentMillis: Long,
    val dateOfFilingMillis: Long,
    val interestRatePercent: Double = 7.5,

    // 2. Petitioner / Deceased Details
    val petitionerName: String,
    val clientPhone: String,
    val petitionerAddress: String = "",
    val age: Int,
    val occupation: String,
    val isSalariedPermanent: Boolean = false,
    val isBachelorDeceased: Boolean = false,
    val claimedMonthlyIncome: Double,
    val hasIncomeProof: Boolean = false,

    // 3. FIR, Criminal Case & Hospital Details
    val firNumber: String = "",
    val policeStation: String = "",
    val bnsIpcSections: String = "281, 125(a) BNS",
    val firDelayDays: Int = 0,
    val hasChargeSheetFiled: Boolean = true,
    val hasSpotMahazarAndMvi: Boolean = true,
    val hospitalName: String = "",
    val inpatientDays: Int = 0,

    // 4. Offending Vehicle, Owner & Insurer Details
    val offendingVehicleReg: String = "",
    val offendingVehicleType: String = "Lorry / Heavy Goods",
    val respondentOwnerName: String = "",
    val insuranceCompanyName: String = "",
    val policyNumber: String = "",
    val hasActiveInsurance: Boolean = true,
    val hasValidDriverLicenceAndBadge: Boolean = true,
    val woreHelmetOrSeatbelt: Boolean = true,
    val isHeadOnCollision: Boolean = false,

    // 5. Disability & Non-Pecuniary Heads
    val permanentDisabilityPercent: Double = 0.0,
    val functionalDisabilityPercent: Double = 0.0,
    val treatmentMonths: Int = 3,
    val painAndSufferingBase: Double = 35000.0,
    val attendantAndNourishmentBase: Double = 25000.0,
    val transportAndDamageBase: Double = 10000.0,
    val futureMedicalExpensesBase: Double = 0.0,

    // 6. Advocate Fee Configuration
    val isFeePercentage: Boolean = true,
    val professionalFeeValue: Double = 10.0,

    // 7. Spot Mahazar Scene Reconstruction
    val roadLayout: String = "TWO_LANE",
    val accidentScenarioSummary: String = "",
    val spotMahazarNotes: String = ""
)

@Entity(
    tableName = "injury_pins",
    foreignKeys = [ForeignKey(entity = CaseEntity::class, parentColumns = ["id"], childColumns = ["caseId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("caseId")]
)
data class InjuryPinEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val caseId: Long,
    val isPosterior: Boolean,
    val normX: Float,
    val normY: Float,
    val regionName: String,
    val diagnosis: String,
    val severity: String, // "GRIEVOUS", "SIMPLE", "NERVE"
    val pinDisabilityPercent: Double = 0.0,
    val exhibitTag: String,
    val occupationalImpact: String = ""
)

@Entity(
    tableName = "legal_heirs",
    foreignKeys = [ForeignKey(entity = CaseEntity::class, parentColumns = ["id"], childColumns = ["caseId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("caseId")]
)
data class LegalHeirEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val caseId: Long,
    val name: String,
    val relation: String,
    val age: Int,
    val sharePercent: Double
)

@Entity(
    tableName = "scene_items",
    foreignKeys = [ForeignKey(entity = CaseEntity::class, parentColumns = ["id"], childColumns = ["caseId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("caseId")]
)
data class SceneItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val caseId: Long,
    val itemType: String, // "LORRY", "CAR", "BIKE", "PEDESTRIAN", "IMPACT_X", "SKID", "ARROW_R", "ARROW_L", "ZEBRA"
    val label: String,
    val normX: Float,
    val normY: Float,
    val rotationDeg: Float = 0f
)

@Entity(
    tableName = "case_documents",
    foreignKeys = [ForeignKey(entity = CaseEntity::class, parentColumns = ["id"], childColumns = ["caseId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("caseId")]
)
data class CaseDocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val caseId: Long,
    val title: String,
    val exhibitNumber: String,
    val category: String, // "COURT_RECORD", "MEDICAL_RECORD", "MEDICAL_BILL"
    val sourceType: String = "SCANNED", // "SCANNED" or "ATTACHED"
    val mimeType: String = "application/pdf",
    val billAmount: Double = 0.0,
    val localFilePath: String,
    val pageCount: Int = 1,
    val scannedAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "case_expenses",
    foreignKeys = [ForeignKey(entity = CaseEntity::class, parentColumns = ["id"], childColumns = ["caseId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("caseId")]
)
data class CaseExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val caseId: Long,
    val particulars: String,
    val amount: Double,
    val isClientAdvance: Boolean = false,
    val dateMillis: Long = System.currentTimeMillis()
)

data class FullCaseBundle(
    @Embedded val case: CaseEntity,
    @Relation(parentColumn = "id", entityColumn = "caseId") val injuries: List<InjuryPinEntity>,
    @Relation(parentColumn = "id", entityColumn = "caseId") val heirs: List<LegalHeirEntity>,
    @Relation(parentColumn = "id", entityColumn = "caseId") val sceneItems: List<SceneItemEntity>,
    @Relation(parentColumn = "id", entityColumn = "caseId") val documents: List<CaseDocumentEntity>,
    @Relation(parentColumn = "id", entityColumn = "caseId") val expenses: List<CaseExpenseEntity>
)
