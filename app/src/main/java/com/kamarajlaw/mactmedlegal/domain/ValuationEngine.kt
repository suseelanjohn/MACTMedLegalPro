package com.kamarajlaw.mactmedlegal.domain

import com.kamarajlaw.mactmedlegal.data.CaseEntity
import com.kamarajlaw.mactmedlegal.data.ClaimCategory
import com.kamarajlaw.mactmedlegal.data.FullCaseBundle
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

data class DefenceAlert(
    val title: String,
    val legalPrinciple: String,
    val counterStrategy: String
)

data class AwardBreakdown(
    val assessedMonthlyIncome: Double,
    val effectiveDisabilityPercent: Double,
    val multiplier: Int,
    val futureProspectsPercent: Int,
    val lossOfEarningOrDependency: Double,
    val verifiedMedicalBills: Double,
    val painAndSuffering: Double,
    val lossOfIncomeDuringTreatment: Double,
    val attendantAndNourishment: Double,
    val conventionalHeads: Double,
    val principalAward: Double,
    val accruedInterest: Double,
    val grossTotal: Double,
    val adjustmentNote: String
)

data class CaseAnalysisReport(
    val totalScore: Int,
    val liabilityScore: Int,
    val statutoryScore: Int,
    val medicalScore: Int,
    val incomeScore: Int,
    val fitnessVerdict: String,
    val isStrong: Boolean,
    val isModerate: Boolean,
    val defences: List<DefenceAlert>,
    val elapsedYears: Double,
    val totalExpensesSpent: Double,
    val totalAdvancesReceived: Double,
    val netUnrecoveredExpenses: Double,
    val projectionA: AwardBreakdown,
    val projectionB: AwardBreakdown,
    val advocateFeeAmount: Double,
    val totalCollectibleFromClient: Double,
    val netTakeHomeForClient: Double
)

object ValuationEngine {

    fun sarlaVermaMultiplier(age: Int): Int = when {
        age <= 25 -> 18
        age <= 30 -> 17
        age <= 35 -> 16
        age <= 40 -> 15
        age <= 45 -> 14
        age <= 50 -> 13
        age <= 55 -> 11
        age <= 60 -> 9
        age <= 65 -> 7
        else -> 5
    }

    fun pranaySethiFutureProspects(age: Int, isSalariedPermanent: Boolean): Int = when {
        isSalariedPermanent && age < 40 -> 50
        isSalariedPermanent && age in 40..50 -> 30
        isSalariedPermanent && age in 51..60 -> 15
        !isSalariedPermanent && age < 40 -> 40
        !isSalariedPermanent && age in 40..50 -> 25
        !isSalariedPermanent && age in 51..60 -> 10
        else -> 0
    }

    fun analyze(bundle: FullCaseBundle, nowMillis: Long = System.currentTimeMillis()): CaseAnalysisReport {
        val c = bundle.case

        // 1. 100-Point Evidence Score Map
        val liabilityScore = (if (c.hasChargeSheetFiled) 14 else 4) +
                (if (c.hasSpotMahazarAndMvi) 8 else 2) +
                (if (c.firDelayDays <= 2) 8 else if (c.firDelayDays <= 7) 4 else 1)

        val statutoryScore = (if (c.hasActiveInsurance) 15 else 0) +
                (if (c.hasValidDriverLicenceAndBadge) 6 else 2) +
                (if (c.woreHelmetOrSeatbelt) 4 else 1)

        val medicalScore = when (c.claimCategory) {
            ClaimCategory.FATAL_DEATH -> 28
            ClaimCategory.SECTION_164_NO_FAULT -> 24
            else -> {
                val pinBonus = (bundle.injuries.size * 4).coerceAtMost(12)
                val disBonus = if (c.functionalDisabilityPercent > 0) 14 else 8
                (4 + pinBonus + disBonus).coerceAtMost(30)
            }
        }

        val incomeScore = if (c.hasIncomeProof) 15 else 8
        val totalScore = (liabilityScore + statutoryScore + medicalScore + incomeScore).coerceIn(0, 100)

        val fitnessVerdict = when {
            c.claimCategory == ClaimCategory.SECTION_164_NO_FAULT ->
                "FIT FOR SEC. 164 NO-FAULT FIXED AWARD"
            totalScore >= 75 ->
                "STRONG — FIT FOR SEC. 166 CONTEST"
            totalScore >= 50 ->
                "MODERATE — LOK ADALAT CANDIDATE"
            else ->
                "WEAK — CONSIDER SEC. 164 CONVERSION"
        }

        // 2. Anticipated Insurer Defences
        val defences = buildList {
            if (!c.hasActiveInsurance) {
                add(
                    DefenceAlert(
                        title = "No Active Third-Party Insurance Policy",
                        legalPrinciple = "Owner Liability Only (Sec. 146 MV Act)",
                        counterStrategy = "Verify cover note / renewal grace period; proceed directly against registered vehicle owner & attach vehicle."
                    )
                )
            }
            if (!c.hasValidDriverLicenceAndBadge) {
                add(
                    DefenceAlert(
                        title = "Invalid DL / Missing Transport Badge",
                        legalPrinciple = "National Insurance Co. Ltd. vs. Swaran Singh",
                        counterStrategy = "Press for 'Pay and Recover' order against the Insurer; third-party claimant cannot be denied compensation."
                    )
                )
            }
            if (!c.woreHelmetOrSeatbelt || c.isHeadOnCollision) {
                add(
                    DefenceAlert(
                        title = "Contributory Negligence Plea (10%–25% Cut Risk)",
                        legalPrinciple = "Mohammed Siddique vs. National Insurance Co.",
                        counterStrategy = "Rely on Spot Mahazar (Ex.P2) & MVI Report to prove sole negligence of offending vehicle."
                    )
                )
            }
            if (c.firDelayDays > 2) {
                add(
                    DefenceAlert(
                        title = "FIR Delay (${c.firDelayDays} Days) — Vehicle Implantation Plea",
                        legalPrinciple = "Ravi vs. Badrinarayan (2011) 4 SCC 693",
                        counterStrategy = "Produce Accident Register (AR) entry recorded on the date of accident showing RTA history immediately upon admission."
                    )
                )
            }
            if (!c.hasIncomeProof && c.claimCategory != ClaimCategory.SECTION_164_NO_FAULT) {
                add(
                    DefenceAlert(
                        title = "Dispute Over Unverified Monthly Income",
                        legalPrinciple = "Ramachandrappa vs. Royal Sundaram Alliance",
                        counterStrategy = "Adopt Minimum Wages / Notional Income benchmark for ${c.occupation} plus Pranay Sethi future prospects."
                    )
                )
            }
        }

        // 3. Verified Medical Bills & Office Expenses
        val verifiedBills = bundle.documents
            .filter { it.category == "MEDICAL_BILL" }
            .sumOf { it.billAmount }

        val totalSpent = bundle.expenses.filter { !it.isClientAdvance }.sumOf { it.amount }
        val totalAdvance = bundle.expenses.filter { it.isClientAdvance }.sumOf { it.amount }
        val netUnrecovered = max(0.0, totalSpent - totalAdvance)

        val elapsedDays = max(1L, (nowMillis - c.dateOfFilingMillis) / (1000L * 60L * 60L * 24L))
        val elapsedYears = elapsedDays / 365.0
        val interestFactor = (c.interestRatePercent / 100.0) * elapsedYears

        val baseIncome = if (c.hasIncomeProof) c.claimedMonthlyIncome else c.claimedMonthlyIncome.coerceAtMost(15000.0)
        val mult = sarlaVermaMultiplier(c.age)
        val fpPct = pranaySethiFutureProspects(c.age, c.isSalariedPermanent)

        // 4. Projection A (Record-Strict Statutory Award)
        val projA = computeProjectionA(c, baseIncome, mult, fpPct, verifiedBills, bundle.heirs.size, interestFactor)

        // 5. Projection B (Expense-Tallying Target Projection)
        val targetPrincipalBump = if (totalSpent > 0.0) totalSpent / (1.0 + interestFactor) else 0.0
        val projB = computeProjectionB(c, projA, targetPrincipalBump, mult, fpPct, verifiedBills, bundle.heirs.size, interestFactor)

        // 6. Client Fee & Net Settlement Recovery
        val basisForFee = projB.grossTotal
        val fee = if (c.isFeePercentage) {
            basisForFee * (c.professionalFeeValue / 100.0)
        } else {
            c.professionalFeeValue
        }
        val collectibleFromClient = fee + netUnrecovered
        val netClientTakeHome = max(0.0, projB.grossTotal - collectibleFromClient)

        return CaseAnalysisReport(
            totalScore = totalScore,
            liabilityScore = liabilityScore,
            statutoryScore = statutoryScore,
            medicalScore = medicalScore,
            incomeScore = incomeScore,
            fitnessVerdict = fitnessVerdict,
            isStrong = totalScore >= 75,
            isModerate = totalScore in 50..74,
            defences = defences,
            elapsedYears = elapsedYears,
            totalExpensesSpent = totalSpent,
            totalAdvancesReceived = totalAdvance,
            netUnrecoveredExpenses = netUnrecovered,
            projectionA = projA,
            projectionB = projB,
            advocateFeeAmount = fee,
            totalCollectibleFromClient = collectibleFromClient,
            netTakeHomeForClient = netClientTakeHome
        )
    }

    private fun computeProjectionA(
        c: CaseEntity,
        baseIncome: Double,
        mult: Int,
        fpPct: Int,
        verifiedBills: Double,
        heirCount: Int,
        interestFactor: Double
    ): AwardBreakdown {
        return when (c.claimCategory) {
            ClaimCategory.SECTION_164_NO_FAULT -> {
                val principal = 250000.0
                val interest = principal * interestFactor
                AwardBreakdown(
                    assessedMonthlyIncome = baseIncome,
                    effectiveDisabilityPercent = c.functionalDisabilityPercent,
                    multiplier = 0,
                    futureProspectsPercent = 0,
                    lossOfEarningOrDependency = principal,
                    verifiedMedicalBills = 0.0,
                    painAndSuffering = 0.0,
                    lossOfIncomeDuringTreatment = 0.0,
                    attendantAndNourishment = 0.0,
                    conventionalHeads = 0.0,
                    principalAward = principal,
                    accruedInterest = interest,
                    grossTotal = principal + interest,
                    adjustmentNote = "Fixed statutory liability under Sec. 164 MV Act (₹2.5L grievous / ₹5L fatal)."
                )
            }

            ClaimCategory.FATAL_DEATH -> {
                val effectiveHeirs = max(1, heirCount)
                val deductionFraction = when {
                    c.isBachelorDeceased -> 0.50
                    effectiveHeirs in 2..3 -> 1.0 / 3.0
                    effectiveHeirs in 4..6 -> 0.25
                    else -> 0.20
                }
                val annualWithFp = baseIncome * 12.0 * (1.0 + fpPct / 100.0)
                val lossOfDependency = annualWithFp * (1.0 - deductionFraction) * mult
                val conventional = (48400.0 * effectiveHeirs) + 18150.0 + 18150.0
                val principal = lossOfDependency + verifiedBills + conventional
                val interest = principal * interestFactor
                AwardBreakdown(
                    assessedMonthlyIncome = baseIncome,
                    effectiveDisabilityPercent = 0.0,
                    multiplier = mult,
                    futureProspectsPercent = fpPct,
                    lossOfEarningOrDependency = lossOfDependency,
                    verifiedMedicalBills = verifiedBills,
                    painAndSuffering = 0.0,
                    lossOfIncomeDuringTreatment = 0.0,
                    attendantAndNourishment = 0.0,
                    conventionalHeads = conventional,
                    principalAward = principal,
                    accruedInterest = interest,
                    grossTotal = principal + interest,
                    adjustmentNote = "Strict Sarla Verma & Pranay Sethi formula (${(deductionFraction * 100).roundToInt()}% personal deduction)."
                )
            }

            else -> {
                val disPct = if (c.claimCategory == ClaimCategory.INJURY_FUNCTIONAL_DISABILITY) {
                    c.functionalDisabilityPercent
                } else 0.0

                val lossOfFutureEarning = if (disPct > 0.0) {
                    baseIncome * 12.0 * (1.0 + fpPct / 100.0) * mult * (disPct / 100.0)
                } else {
                    c.permanentDisabilityPercent * 7000.0
                }

                val treatmentLoss = baseIncome * c.treatmentMonths
                val pain = if (c.claimCategory == ClaimCategory.INJURY_SIMPLE) 15000.0 else c.painAndSufferingBase
                val attendant = if (c.claimCategory == ClaimCategory.INJURY_SIMPLE) 5000.0 else c.attendantAndNourishmentBase
                val principal = lossOfFutureEarning + verifiedBills + pain + treatmentLoss + attendant
                val interest = principal * interestFactor

                AwardBreakdown(
                    assessedMonthlyIncome = baseIncome,
                    effectiveDisabilityPercent = disPct,
                    multiplier = mult,
                    futureProspectsPercent = fpPct,
                    lossOfEarningOrDependency = lossOfFutureEarning,
                    verifiedMedicalBills = verifiedBills,
                    painAndSuffering = pain,
                    lossOfIncomeDuringTreatment = treatmentLoss,
                    attendantAndNourishment = attendant,
                    conventionalHeads = 0.0,
                    principalAward = principal,
                    accruedInterest = interest,
                    grossTotal = principal + interest,
                    adjustmentNote = "Strict record-based Tribunal award projection."
                )
            }
        }
    }

    private fun computeProjectionB(
        c: CaseEntity,
        projA: AwardBreakdown,
        targetPrincipalBump: Double,
        mult: Int,
        fpPct: Int,
        verifiedBills: Double,
        heirCount: Int,
        interestFactor: Double
    ): AwardBreakdown {
        if (targetPrincipalBump <= 0.0 || c.claimCategory == ClaimCategory.SECTION_164_NO_FAULT) {
            return projA.copy(adjustmentNote = "Log expenses in Expenses tab to auto-calibrate Projection B target heads.")
        }

        if (c.claimCategory == ClaimCategory.FATAL_DEATH) {
            val effectiveHeirs = max(1, heirCount)
            val deductionFraction = when {
                c.isBachelorDeceased -> 0.50
                effectiveHeirs in 2..3 -> 1.0 / 3.0
                effectiveHeirs in 4..6 -> 0.25
                else -> 0.20
            }
            val factorPerRupeeIncome = 12.0 * (1.0 + fpPct / 100.0) * (1.0 - deductionFraction) * mult
            val requiredIncomeDelta = ceil((targetPrincipalBump / factorPerRupeeIncome) / 100.0) * 100.0
            val adjustedIncome = projA.assessedMonthlyIncome + requiredIncomeDelta
            val newDependency = adjustedIncome * factorPerRupeeIncome
            val newPrincipal = newDependency + verifiedBills + projA.conventionalHeads
            val newInterest = newPrincipal * interestFactor

            return projA.copy(
                assessedMonthlyIncome = adjustedIncome,
                lossOfEarningOrDependency = newDependency,
                principalAward = newPrincipal,
                accruedInterest = newInterest,
                grossTotal = newPrincipal + newInterest,
                adjustmentNote = "Fatal strict formula locked; Monthly Income stepped up by ₹${requiredIncomeDelta.toInt()} to tally expenses."
            )
        }

        val onePercentValue = projA.assessedMonthlyIncome * 12.0 * (1.0 + fpPct / 100.0) * mult * 0.01
        return if (c.claimCategory == ClaimCategory.INJURY_FUNCTIONAL_DISABILITY && onePercentValue > 0.0) {
            val bumpedPctSteps = ceil(targetPrincipalBump / onePercentValue).coerceAtLeast(1.0)
            val newDisPct = (projA.effectiveDisabilityPercent + bumpedPctSteps).coerceAtMost(100.0)
            val newLossEarning = onePercentValue * newDisPct
            val gainedFromDisability = newLossEarning - projA.lossOfEarningOrDependency
            val residualGap = max(0.0, targetPrincipalBump - gainedFromDisability)
            val newPain = projA.painAndSuffering + (residualGap * 0.6)
            val newAttendant = projA.attendantAndNourishment + (residualGap * 0.4)
            val newPrincipal = newLossEarning + verifiedBills + newPain + projA.lossOfIncomeDuringTreatment + newAttendant
            val newInterest = newPrincipal * interestFactor

            projA.copy(
                effectiveDisabilityPercent = newDisPct,
                lossOfEarningOrDependency = newLossEarning,
                painAndSuffering = newPain,
                attendantAndNourishment = newAttendant,
                principalAward = newPrincipal,
                accruedInterest = newInterest,
                grossTotal = newPrincipal + newInterest,
                adjustmentNote = "Functional Disability stepped up ${projA.effectiveDisabilityPercent.toInt()}% → ${newDisPct.toInt()}% + non-pecuniary heads to absorb spend."
            )
        } else {
            val newPain = projA.painAndSuffering + (targetPrincipalBump * 0.6)
            val newAttendant = projA.attendantAndNourishment + (targetPrincipalBump * 0.4)
            val newPrincipal = projA.lossOfEarningOrDependency + verifiedBills + newPain + projA.lossOfIncomeDuringTreatment + newAttendant
            val newInterest = newPrincipal * interestFactor

            projA.copy(
                painAndSuffering = newPain,
                attendantAndNourishment = newAttendant,
                principalAward = newPrincipal,
                accruedInterest = newInterest,
                grossTotal = newPrincipal + newInterest,
                adjustmentNote = "Pain & Suffering and Attendant/Nourishment heads adjusted to absorb spend (0% functional disability)."
            )
        }
    }
}
