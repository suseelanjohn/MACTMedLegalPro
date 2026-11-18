package com.kamarajlaw.mactmedlegal.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.kamarajlaw.mactmedlegal.data.ClaimCategory
import com.kamarajlaw.mactmedlegal.data.FullCaseBundle
import com.kamarajlaw.mactmedlegal.domain.CaseAnalysisReport
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale

object CourtPdfGenerator {

    private val inr = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 0
    }

    fun generateAndShareBrief(context: Context, bundle: FullCaseBundle, report: CaseAnalysisReport) {
        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 16f
            isFakeBoldText = true
        }
        val subPaint = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = 11f
        }
        val boldPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 11f
            isFakeBoldText = true
        }

        var y = 44f
        val c = bundle.case
        canvas.drawText("MACT MEDLEGAL VISUAL AID & CALCULATION MEMO", 40f, y, titlePaint)
        y += 20f
        canvas.drawText("${c.mcopNumber}  |  ${c.tribunalName}", 40f, y, boldPaint)
        y += 16f
        canvas.drawText("Petitioner: ${c.petitionerName} (Age ${c.age}, ${c.occupation})  |  Phone: ${c.clientPhone}", 40f, y, subPaint)
        y += 16f
        canvas.drawText("Merits Score: ${report.totalScore}/100 — ${report.fitnessVerdict}", 40f, y, boldPaint)

        y += 26f
        canvas.drawText("PROJECTION A: STRICT STATUTORY TRIBUNAL AWARD", 40f, y, boldPaint)
        y += 16f
        canvas.drawText("Loss of Future Earning / Dependency: ${inr.format(report.projectionA.lossOfEarningOrDependency)}", 48f, y, subPaint)
        y += 15f
        canvas.drawText("Verified Medical Bills: ${inr.format(report.projectionA.verifiedMedicalBills)}", 48f, y, subPaint)
        y += 15f
        if (c.claimCategory == ClaimCategory.FATAL_DEATH) {
            canvas.drawText("Conventional Heads (Consortium + Estate + Funeral): ${inr.format(report.projectionA.conventionalHeads)}", 48f, y, subPaint)
        } else {
            canvas.drawText("Pain, Treatment Loss & Attendant/Nourishment: ${inr.format(report.projectionA.painAndSuffering + report.projectionA.lossOfIncomeDuringTreatment + report.projectionA.attendantAndNourishment)}", 48f, y, subPaint)
        }
        y += 16f
        canvas.drawText("Principal: ${inr.format(report.projectionA.principalAward)}  +  Interest (${c.interestRatePercent}%): ${inr.format(report.projectionA.accruedInterest)}  =  ${inr.format(report.projectionA.grossTotal)}", 48f, y, boldPaint)

        y += 26f
        canvas.drawText("PROJECTION B: EXPENSE-ADJUSTED TARGET & CLIENT SETTLEMENT", 40f, y, boldPaint)
        y += 16f
        canvas.drawText(report.projectionB.adjustmentNote, 48f, y, subPaint)
        y += 16f
        canvas.drawText("Projection B Gross Total: ${inr.format(report.projectionB.grossTotal)}", 48f, y, boldPaint)
        y += 15f
        canvas.drawText("Advocate Fee (${inr.format(report.advocateFeeAmount)}) + Unrecovered Expenses (${inr.format(report.netUnrecoveredExpenses)}) = Collectible: ${inr.format(report.totalCollectibleFromClient)}", 48f, y, subPaint)
        y += 15f
        canvas.drawText("Net Take-Home for Petitioner(s): ${inr.format(report.netTakeHomeForClient)}", 48f, y, boldPaint)

        if (bundle.heirs.isNotEmpty()) {
            y += 26f
            canvas.drawText("LEGAL HEIRS APPORTIONMENT SCHEDULE", 40f, y, boldPaint)
            bundle.heirs.forEach { heir ->
                y += 15f
                val shareAmt = report.projectionA.grossTotal * (heir.sharePercent / 100.0)
                canvas.drawText("• ${heir.name} (${heir.relation}, Age ${heir.age}) — ${heir.sharePercent.toInt()}% = ${inr.format(shareAmt)}", 48f, y, subPaint)
            }
        }

        pdf.finishPage(page)

        val dir = File(context.cacheDir, "court_briefs").apply { mkdirs() }
        val outFile = File(dir, "${c.mcopNumber.replace("/", "_")}_Brief.pdf")
        FileOutputStream(outFile).use { pdf.writeTo(it) }
        pdf.close()

        ClientActions.shareFile(context, outFile.absolutePath, "${c.mcopNumber} Tribunal Brief")
    }
}
