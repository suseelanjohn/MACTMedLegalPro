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
            textSize = 15f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val headerSub = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 10f
            isAntiAlias = true
        }
        val sectionPaint = Paint().apply {
            color = Color.parseColor("#0F2942")
            textSize = 12f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = 10.5f
            isAntiAlias = true
        }
        val boldPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 10.5f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 1f
        }

        val c = bundle.case
        // Header Banner
        canvas.drawRect(0f, 0f, 595f, 76f, headerBg)
        canvas.drawText("MACT MEDLEGAL PRO — TRIBUNAL VISUAL AID & VALUATION MEMO", 32f, 32f, headerTitle)
        canvas.drawText("${c.mcopNumber}  |  ${c.tribunalName}  |  Score: ${report.totalScore}/100", 32f, 54f, headerSub)

        var y = 102f
        canvas.drawText("Petitioner: ${c.petitionerName} (Age ${c.age}, ${c.occupation})", 32f, y, boldPaint)
        y += 16f
        canvas.drawText("Client Phone: ${c.clientPhone.ifBlank { "N/A" }}  |  Category: ${c.claimCategory.label}", 32f, y, bodyPaint)
        y += 16f
        canvas.drawText("Verdict: ${report.fitnessVerdict}", 32f, y, sectionPaint)

        y += 14f
        canvas.drawLine(32f, y, 563f, y, linePaint)
        y += 22f

        // Projection A
        canvas.drawText("1. PROJECTION A — STRICT STATUTORY TRIBUNAL AWARD", 32f, y, sectionPaint)
        y += 18f
        canvas.drawText("Loss of Future Earning / Dependency: ${ClientActions.formatInr(report.projectionA.lossOfEarningOrDependency)}", 40f, y, bodyPaint)
        y += 15f
        canvas.drawText("Verified Medical Bills: ${ClientActions.formatInr(report.projectionA.verifiedMedicalBills)}", 40f, y, bodyPaint)
        y += 15f
        if (c.claimCategory == ClaimCategory.FATAL_DEATH) {
            canvas.drawText("Conventional Heads (Consortium + Estate + Funeral): ${ClientActions.formatInr(report.projectionA.conventionalHeads)}", 40f, y, bodyPaint)
        } else {
            val nonPec = report.projectionA.painAndSuffering + report.projectionA.lossOfIncomeDuringTreatment + report.projectionA.attendantAndNourishment
            canvas.drawText("Pain & Suffering, Treatment Loss & Attendant/Nourishment: ${ClientActions.formatInr(nonPec)}", 40f, y, bodyPaint)
        }
        y += 17f
        canvas.drawText(
            "Principal: ${ClientActions.formatInr(report.projectionA.principalAward)}  +  Interest (${c.interestRatePercent}%): ${ClientActions.formatInr(report.projectionA.accruedInterest)}  =  ${ClientActions.formatInr(report.projectionA.grossTotal)}",
            40f, y, boldPaint
        )

        y += 14f
        canvas.drawLine(32f, y, 563f, y, linePaint)
        y += 22f

        // Projection B
        canvas.drawText("2. PROJECTION B — EXPENSE-ABSORBING TARGET & CLIENT SETTLEMENT", 32f, y, sectionPaint)
        y += 16f
        canvas.drawText(report.projectionB.adjustmentNote, 40f, y, bodyPaint)
        y += 17f
        canvas.drawText("Projection B Gross Target: ${ClientActions.formatInr(report.projectionB.grossTotal)}", 40f, y, boldPaint)
        y += 15f
        canvas.drawText(
            "Advocate Fee (${ClientActions.formatInr(report.advocateFeeAmount)}) + Unrecovered Spend (${ClientActions.formatInr(report.netUnrecoveredExpenses)}) = Collectible: ${ClientActions.formatInr(report.totalCollectibleFromClient)}",
            40f, y, bodyPaint
        )
        y += 15f
        canvas.drawText("Net Take-Home Balance for Petitioner(s): ${ClientActions.formatInr(report.netTakeHomeForClient)}", 40f, y, boldPaint)

        y += 14f
        canvas.drawLine(32f, y, 563f, y, linePaint)
        y += 22f

        // Injuries or Heirs Schedule
        if (c.claimCategory == ClaimCategory.FATAL_DEATH) {
            canvas.drawText("3. LEGAL HEIRS APPORTIONMENT SCHEDULE", 32f, y, sectionPaint)
            if (bundle.heirs.isEmpty()) {
                y += 16f
                canvas.drawText("No legal heirs added yet.", 40f, y, bodyPaint)
            } else {
                bundle.heirs.forEach { heir ->
                    y += 16f
                    val shareAmt = report.projectionA.grossTotal * (heir.sharePercent / 100.0)
                    canvas.drawText("• ${heir.name} (${heir.relation}, Age ${heir.age}) — ${heir.sharePercent.toInt()}% = ${ClientActions.formatInr(shareAmt)}", 40f, y, bodyPaint)
                }
            }
        } else {
            canvas.drawText("3. DOCUMENTED ANATOMICAL INJURIES (${bundle.injuries.size})", 32f, y, sectionPaint)
            if (bundle.injuries.isEmpty()) {
                y += 16f
                canvas.drawText("No injury pins plotted yet.", 40f, y, bodyPaint)
            } else {
                bundle.injuries.forEachIndexed { idx, pin ->
                    y += 16f
                    val side = if (pin.isPosterior) "Posterior" else "Anterior"
                    canvas.drawText("#${idx + 1} [${pin.exhibitTag}] ${pin.regionName} ($side) — ${pin.diagnosis} (${pin.severity})", 40f, y, bodyPaint)
                }
            }
        }

        // Spot Mahazar Reconstruction Summary
        y += 24f
        canvas.drawText("4. SPOT MAHAZAR (Ex.P2) SCENE RECONSTRUCTION (${c.roadLayout})", 32f, y, sectionPaint)
        y += 16f
        canvas.drawText("Notes: ${c.spotMahazarNotes.ifBlank { "Standard road collision sketch." }}", 40f, y, bodyPaint)

        pdf.finishPage(page)

        val dir = File(context.cacheDir, "court_briefs").apply { mkdirs() }
        val safeName = c.mcopNumber.replace(Regex("[^A-Za-z0-9]"), "_")
        val outFile = File(dir, "${safeName}_Brief.pdf")
        FileOutputStream(outFile).use { pdf.writeTo(it) }
        pdf.close()

        ClientActions.shareFile(context, outFile.absolutePath, "${c.mcopNumber} Tribunal Brief")
    }
}
