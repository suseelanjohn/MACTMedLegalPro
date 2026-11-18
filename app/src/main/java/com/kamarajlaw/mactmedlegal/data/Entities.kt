package com.kamarajlaw.mactmedlegal.data

import androidx.room.*

enum class ClaimCategory {
    INJURY_FUNCTIONAL_DISABILITY, // Sec 166 Multiplier Method (Raj Kumar)
    INJURY_GRIEVOUS_NO_DISABILITY,// Sec 166 Lump-sum + Treatment Loss (0% functional disability)
    INJURY_SIMPLE,                // Sec 166 Simple Injuries
    FATAL_DEATH,                  // Sec 166 Strict Pranay Sethi + Sarla Verma
    SECTION_164_NO_FAULT          // Sec 164 Fixed Liability (₹5L Death / ₹2.5L Grievous)
}

@Entity(tableName = "cases")
data class CaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mcopNumber: String,
    val tribunalName: String,
    val claimCategory: ClaimCategory,
    val petitionerName: String,
    val clientPhone: String,
    val age: Int,
    val occupation: String,
    val isSalariedPermanent: Boolean = false,
    val isBachelorDeceased: Boolean = false,
    val claimedMonthlyIncome: Double,
    val hasIncomeProof: Boolean = false,
    val dateOfAccidentMillis: Long,
    val dateOfFilingMillis: Long,
    val interestRatePercent: Double = 7.5,

    // Statutory & Evidentiary Flags
    val hasActiveInsurance: Boolean = true,
    val hasValidDriverLicenceAndBadge: Boolean = true,
    val hasChargeSheetFiled: Boolean = true,
    val hasSpotMahazarAndMvi: Boolean = true,
    val firDelayDays: Int = 0,
    val woreHelmetOrSeatbelt: Boolean = true,
    val isHeadOnCollision: Boolean = false,

    // Injury Parameters
    val permanentDisabilityPercent: Double = 0.0,
    val functionalDisabilityPercent: Double = 0.0,
    val treatmentMonths: Int = 3,
    val painAndSufferingBase: Double = 35000.0,
    val attendantAndNourishmentBase: Double = 25000.0,

    // Advocate Fee Configuration
    val isFeePercentage: Boolean = true,
    val professionalFeeValue: Double = 10.0, // 10% or flat ₹

    // Spot Mahazar Reconstruction Notes
    val roadLayout: String = "TWO_LANE",
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
    val normX: Float, // 0f..1f
    val normY: Float, // 0f..1f
    val diagnosis: String,
    val severity: String, // "GRIEVOUS", "SIMPLE", "NERVE"
    val exhibitTag: String
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
    val relation: String, // "Spouse", "Minor Child", "Mother", "Father"
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
    val itemType: String, // "LORRY", "CAR", "BIKE", "IMPACT_X", "SKID"
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
    val exhibitNumber: String, // e.g., "Ex.P1 FIR", "Ex.P8 Medical Bill"
    val category: String,      // "COURT_RECORD", "MEDICAL_RECORD", "MEDICAL_BILL"
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
    val isClientAdvance: Boolean = false, // false = Office Spend, true = Advance Received
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
