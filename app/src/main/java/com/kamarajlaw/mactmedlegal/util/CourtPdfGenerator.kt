package com.kamarajlaw.mactmedlegal.util

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.kamarajlaw.mactmedlegal.data.ClaimCategory
import com.kamarajlaw.mactmedlegal.data.FullCaseBundle
import com.kamarajlaw.mactmedlegal.domain.CaseAnalysisReport
import java.io.File
import java.io.FileOutputStream

object CourtPdfGenerator {

    fun generateAndShareBrief(context: Context, bundle: FullCaseBundle, report: CaseAnalysisReport) {
        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val headerBg = Paint().apply { color = Color.parseColor("#0F2942") }
        val headerTitle = Paint().apply {
            color = Color.WHITE
            textSize = 14.5f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val headerSub = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 9.5f
            isAntiAlias = true
        }
        val sectionPaint = Paint().apply {
            color = Color.parseColor("#0F2942")
            textSize = 11.5f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = 10f
            isAntiAlias = true
        }
        val boldPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 10f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 1f
        }

        val c = bundle.case
        canvas.drawRect(0f, 0f, 595f, 74f, headerBg)
        canvas.drawText("MACT MEDLEGAL PRO — TRIBUNAL VISUAL AID & CALCULATION MEMO", 32f, 30f, headerTitle)
        canvas.drawText("${c.mcopNumber}  |  ${c.tribunalName}  |  Merits Score: ${report.totalScore}/100", 32f, 52f, headerSub)

        var y = 96f
        canvas.drawText("Petitioner: ${c.petitionerName} (Age ${c.age}, ${c.occupation})  |  Phone: ${c.clientPhone.ifBlank { "N/A" }}", 32f, y, boldPaint)
        y += 15f
        canvas.drawText("FIR: ${c.firNumber.ifBlank { "N/A" }} (${c.policeStation.ifBlank { "PS" }})  |  Vehicle: ${c.offendingVehicleReg.ifBlank { "N/A" }}  |  Insurer: ${c.insuranceCompanyName.ifBlank { "N/A" }}", 32f, y, bodyPaint)
        y += 15f
        canvas.drawText("Fitness Verdict: ${report.fitnessVerdict}", 32f, y, sectionPaint)

        y += 12f
        canvas.drawLine(32f, y, 563f, y, linePaint)
        y += 20f

        canvas.drawText("1. PROJECTION A — STRICT STATUTORY TRIBUNAL AWARD", 32f, y, sectionPaint)
        y += 16f
        canvas.drawText("Loss of Future Earning / Dependency: ${ClientActions.formatInr(report.projectionA.lossOfEarningOrDependency)}", 40f, y, bodyPaint)
        y += 14f
        canvas.drawText("Verified Medical Bills (Ex.P8): ${ClientActions.formatInr(report.projectionA.verifiedMedicalBills)}", 40f, y, bodyPaint)
        y += 14f
        if (c.claimCategory == ClaimCategory.FATAL_DEATH) {
            canvas.drawText("Conventional Heads (Consortium + Estate + Funeral): ${ClientActions.formatInr(report.projectionA.conventionalHeads)}", 40f, y, bodyPaint)
        } else {
            val nonPec = report.projectionA.painAndSuffering + report.projectionA.lossOfIncomeDuringTreatment + report.projectionA.attendantAndNourishment + report.projectionA.transportAndFutureMedical
            canvas.drawText("Pain, Treatment Loss, Attendant, Transport & Future Med: ${ClientActions.formatInr(nonPec)}", 40f, y, bodyPaint)
        }
        y += 16f
        canvas.drawText(
            "Principal: ${ClientActions.formatInr(report.projectionA.principalAward)}  +  Interest (${c.interestRatePercent}%): ${ClientActions.formatInr(report.projectionA.accruedInterest)}  =  ${ClientActions.formatInr(report.projectionA.grossTotal)}",
            40f, y, boldPaint
        )

        y += 12f
        canvas.drawLine(32f, y, 563f, y, linePaint)
        y += 20f

        canvas.drawText("2. PROJECTION B — EXPENSE-ABSORBING TARGET & CLIENT RECOVERY", 32f, y, sectionPaint)
        y += 15f
        canvas.drawText(report.projectionB.adjustmentNote, 40f, y, bodyPaint)
        y += 16f
        canvas.drawText("Projection B Gross Target: ${ClientActions.formatInr(report.projectionB.grossTotal)}", 40f, y, boldPaint)
        y += 14f
        canvas.drawText(
            "Advocate Fee (${ClientActions.formatInr(report.advocateFeeAmount)}) + Unrecovered Spend (${ClientActions.formatInr(report.netUnrecoveredExpenses)}) = Collectible: ${ClientActions.formatInr(report.totalCollectibleFromClient)}",
            40f, y, bodyPaint
        )
        y += 14f
        canvas.drawText("Net Take-Home Balance for Petitioner(s): ${ClientActions.formatInr(report.netTakeHomeForClient)}", 40f, y, boldPaint)

        y += 12f
        canvas.drawLine(32f, y, 563f, y, linePaint)
        y += 20f

        if (c.claimCategory == ClaimCategory.FATAL_DEATH) {
            canvas.drawText("3. LEGAL HEIRS APPORTIONMENT SCHEDULE", 32f, y, sectionPaint)
            bundle.heirs.forEach { heir ->
                y += 15f
                val shareAmt = report.projectionA.grossTotal * (heir.sharePercent / 100.0)
                canvas.drawText("• ${heir.name} (${heir.relation}, Age ${heir.age}) — ${heir.sharePercent.toInt()}% = ${ClientActions.formatInr(shareAmt)}", 40f, y, bodyPaint)
            }
        } else {
            canvas.drawText("3. ANATOMICAL INJURY & DISABILITY SCHEDULE (${bundle.injuries.size} Pins)", 32f, y, sectionPaint)
            bundle.injuries.forEachIndexed { idx, pin ->
                y += 15f
                val side = if (pin.isPosterior) "Back" else "Front"
                val disStr = if (pin.pinDisabilityPercent > 0) " [Dis: ${pin.pinDisabilityPercent.toInt()}%]" else ""
                canvas.drawText("#${idx + 1} [${pin.exhibitTag}] ${pin.regionName} ($side) — ${pin.diagnosis}$disStr", 40f, y, bodyPaint)
            }
        }

        y += 22f
        canvas.drawText("4. SPOT MAHAZAR (Ex.P2) ACCIDENT RECONSTRUCTION (${c.roadLayout})", 32f, y, sectionPaint)
        y += 15f
        canvas.drawText("Scenario: ${c.accidentScenarioSummary.ifBlank { "Standard road collision" }}", 40f, y, boldPaint)
        y += 14f
        canvas.drawText("Landmarks: ${c.spotMahazarNotes.ifBlank { "As per Ex.P2 rough sketch" }}", 40f, y, bodyPaint)

        pdf.finishPage(page)

        val dir = File(context.cacheDir, "court_briefs").apply { mkdirs() }
        val safeName = c.mcopNumber.replace(Regex("[^A-Za-z0-9]"), "_")
        val outFile = File(dir, "${safeName}_Brief.pdf")
        FileOutputStream(outFile).use { pdf.writeTo(it) }
        pdf.close()

        ClientActions.shareFile(context, outFile.absolutePath, "${c.mcopNumber} Tribunal Brief")
    }
}
