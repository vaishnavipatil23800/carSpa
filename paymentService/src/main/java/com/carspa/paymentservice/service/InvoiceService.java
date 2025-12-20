/**
 * InvoiceService.java — generates a professional PDF invoice using iText 7.
 *
 * Invoice layout:
 *   ┌─────────────────────────────────────────┐
 *   │  CarSpa India Pvt. Ltd.    [Logo text]  │
 *   │  GSTIN: 27AABCC1234D1Z5                 │
 *   │  TAX INVOICE         Invoice #INV-00001 │
 *   ├─────────────────────────────────────────┤
 *   │  Bill To:            Payment Details:   │
 *   │  Vaishnavi Patil     Razorpay: pay_xxx  │
 *   │  vaishnavi@test.com  Date: 2024-12-25   │
 *   ├─────────────────────────────────────────┤
 *   │  Vehicle: MH12AB1234  Centre: Pune Cen  │
 *   ├──────────────┬──────────────────────────┤
 *   │  Description │ Qty  Unit   Amt    Total  │
 *   │  BASIC Wash  │  1  299.00 299.00 299.00  │
 *   ├──────────────┴──────────────────────────┤
 *   │              Sub-total:         299.00   │
 *   │              GST (18%):          53.82   │
 *   │              TOTAL:             352.82   │
 *   ├─────────────────────────────────────────┤
 *   │  Status: PAID           Thank you! 🚗   │
 *   └─────────────────────────────────────────┘
 *
 * Saved to: /tmp/invoices/INV-{paymentId}.pdf
 */
package com.carspa.paymentservice.service;

import com.carspa.paymentservice.model.Payment;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class InvoiceService {

    private static final String INVOICE_DIR     = System.getProperty("java.io.tmpdir") + "/carspa-invoices/";
    private static final DeviceRgb BRAND_BLUE   = new DeviceRgb(0, 102, 204);
    private static final DeviceRgb LIGHT_GREY   = new DeviceRgb(245, 245, 245);
    private static final DateTimeFormatter FMT   = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    @Value("${payment.company-name:CarSpa India Pvt. Ltd.}")
    private String companyName;

    @Value("${payment.company-gstin:27AABCC1234D1Z5}")
    private String companyGstin;

    @Value("${payment.gst-rate:0.18}")
    private double gstRate;

    /**
     * Generates a PDF invoice for the given payment and returns the file path.
     */
    public String generateInvoice(Payment payment) throws IOException {
        // ensure invoice directory exists
        Path invoiceDir = Path.of(INVOICE_DIR);
        Files.createDirectories(invoiceDir);

        String fileName   = "INV-" + String.format("%05d", payment.getId()) + ".pdf";
        String filePath   = INVOICE_DIR + fileName;

        try (PdfWriter writer    = new PdfWriter(filePath);
             PdfDocument pdfDoc  = new PdfDocument(writer);
             Document document   = new Document(pdfDoc, PageSize.A4)) {

            document.setMargins(36, 50, 36, 50);

            PdfFont bold    = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);
            PdfFont regular = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA);

            // ── Header ──
            addHeader(document, bold, regular, payment);

            // ── Divider ──
            document.add(new Paragraph(" "));

            // ── Bill To + Payment Details (2-column) ──
            addBillingSection(document, bold, regular, payment);

            document.add(new Paragraph(" "));

            // ── Vehicle & Centre Info ──
            addVehicleInfo(document, bold, regular, payment);

            document.add(new Paragraph(" "));

            // ── Line items table ──
            addItemsTable(document, bold, regular, payment);

            document.add(new Paragraph(" "));

            // ── Totals ──
            addTotals(document, bold, regular, payment);

            document.add(new Paragraph(" "));

            // ── Footer ──
            addFooter(document, regular, payment);
        }

        log.info("Invoice generated: {}", filePath);
        return filePath;
    }

    // ── private section builders ──

    private void addHeader(Document doc, PdfFont bold, PdfFont regular, Payment payment) {
        // Company name in brand blue
        Paragraph companyPara = new Paragraph(companyName)
            .setFont(bold).setFontSize(18)
            .setFontColor(BRAND_BLUE)
            .setTextAlignment(TextAlignment.CENTER);
        doc.add(companyPara);

        doc.add(new Paragraph("GSTIN: " + companyGstin)
            .setFont(regular).setFontSize(9)
            .setTextAlignment(TextAlignment.CENTER)
            .setFontColor(ColorConstants.GRAY));

        // Horizontal rule
        Table rule = new Table(UnitValue.createPercentArray(new float[]{1}))
            .setWidth(UnitValue.createPercentValue(100));
        rule.addCell(new Cell().setBorderBottom(new SolidBorder(BRAND_BLUE, 2))
            .setBorderLeft(Border.NO_BORDER).setBorderRight(Border.NO_BORDER)
            .setBorderTop(Border.NO_BORDER).setPadding(0));
        doc.add(rule);

        doc.add(new Paragraph(" "));

        // TAX INVOICE heading + invoice number
        Table headerRow = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
            .setWidth(UnitValue.createPercentValue(100));
        headerRow.addCell(noBorderCell(
            new Paragraph("TAX INVOICE").setFont(bold).setFontSize(14)
        ));
        headerRow.addCell(noBorderCell(
            new Paragraph("Invoice #INV-" + String.format("%05d", payment.getId()))
                .setFont(regular).setFontSize(11)
                .setTextAlignment(TextAlignment.RIGHT)
        ));
        doc.add(headerRow);
    }

    private void addBillingSection(Document doc, PdfFont bold, PdfFont regular, Payment payment) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
            .setWidth(UnitValue.createPercentValue(100));

        // Bill To
        String billTo = "Bill To:\n" +
            (payment.getUserName() != null ? payment.getUserName() + "\n" : "") +
            payment.getUserEmail();
        table.addCell(noBorderCell(
            new Paragraph(billTo).setFont(regular).setFontSize(10)
        ));

        // Payment details
        String payDetails = "Payment Details:\n" +
            "Razorpay Payment ID: " + nvl(payment.getRazorpayPaymentId()) + "\n" +
            "Razorpay Order ID:   " + nvl(payment.getRazorpayOrderId()) + "\n" +
            "Date: " + (payment.getUpdatedAt() != null ? payment.getUpdatedAt().format(FMT) : "-");
        table.addCell(noBorderCell(
            new Paragraph(payDetails).setFont(regular).setFontSize(10)
                .setTextAlignment(TextAlignment.RIGHT)
        ));

        doc.add(table);
    }

    private void addVehicleInfo(Document doc, PdfFont bold, PdfFont regular, Payment payment) {
        String info = "Vehicle: " + nvl(payment.getVehicleNumber()) +
                      "     Wash Centre: " + nvl(payment.getWashCentre());
        doc.add(new Paragraph(info).setFont(regular).setFontSize(10)
            .setBackgroundColor(LIGHT_GREY).setPadding(6));
    }

    private void addItemsTable(Document doc, PdfFont bold, PdfFont regular, Payment payment) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{4, 1, 2, 2, 2}))
            .setWidth(UnitValue.createPercentValue(100));

        // Header row
        String[] headers = {"Description", "Qty", "Unit Price (₹)", "Taxable Amt (₹)", "Total (₹)"};
        for (String h : headers) {
            table.addHeaderCell(new Cell()
                .add(new Paragraph(h).setFont(bold).setFontSize(10))
                .setBackgroundColor(BRAND_BLUE)
                .setFontColor(ColorConstants.WHITE)
                .setPadding(6));
        }

        // Single line item
        String description = serviceLabel(payment.getServiceType()) + " Car Wash";
        BigDecimal unitPrice = payment.getAmount();
        BigDecimal taxable   = unitPrice;

        table.addCell(dataCell(description, regular));
        table.addCell(dataCell("1", regular, TextAlignment.CENTER));
        table.addCell(dataCell(fmt(unitPrice), regular, TextAlignment.RIGHT));
        table.addCell(dataCell(fmt(taxable),   regular, TextAlignment.RIGHT));
        table.addCell(dataCell(fmt(taxable),   regular, TextAlignment.RIGHT));

        doc.add(table);
    }

    private void addTotals(Document doc, PdfFont bold, PdfFont regular, Payment payment) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{3, 1}))
            .setWidth(UnitValue.createPercentValue(50))
            .setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.RIGHT);

        addTotalRow(table, regular, "Sub-total:",      fmt(payment.getAmount()));
        addTotalRow(table, regular,
            "GST (" + (int)(gstRate * 100) + "%):", fmt(payment.getGstAmount()));

        // Bold total row
        table.addCell(new Cell().add(new Paragraph("TOTAL (INR):").setFont(bold).setFontSize(11))
            .setBackgroundColor(BRAND_BLUE).setFontColor(ColorConstants.WHITE).setPadding(6)
            .setBorder(Border.NO_BORDER));
        table.addCell(new Cell().add(new Paragraph("₹ " + fmt(payment.getTotalAmount()))
            .setFont(bold).setFontSize(11).setTextAlignment(TextAlignment.RIGHT))
            .setBackgroundColor(BRAND_BLUE).setFontColor(ColorConstants.WHITE).setPadding(6)
            .setBorder(Border.NO_BORDER));

        doc.add(table);
    }

    private void addFooter(Document doc, PdfFont regular, Payment payment) {
        String statusText = "SUCCESS".equals(payment.getStatus()) ? "✓ PAID" : "✗ " + payment.getStatus();
        doc.add(new Paragraph("Payment Status: " + statusText)
            .setFont(regular).setFontSize(10)
            .setFontColor("SUCCESS".equals(payment.getStatus())
                ? new DeviceRgb(0, 150, 0) : ColorConstants.RED));

        doc.add(new Paragraph("Thank you for choosing CarSpa! Drive clean. 🚗")
            .setFont(regular).setFontSize(10)
            .setFontColor(ColorConstants.GRAY)
            .setTextAlignment(TextAlignment.CENTER));
    }

    // ── helpers ──

    private Cell noBorderCell(Paragraph p) {
        return new Cell().add(p).setBorder(Border.NO_BORDER).setPadding(4);
    }

    private Cell dataCell(String text, PdfFont font) {
        return dataCell(text, font, TextAlignment.LEFT);
    }

    private Cell dataCell(String text, PdfFont font, TextAlignment align) {
        return new Cell().add(new Paragraph(text).setFont(font).setFontSize(10)
            .setTextAlignment(align)).setPadding(5);
    }

    private void addTotalRow(Table table, PdfFont regular, String label, String value) {
        table.addCell(new Cell().add(new Paragraph(label).setFont(regular).setFontSize(10))
            .setPadding(4).setBorder(Border.NO_BORDER));
        table.addCell(new Cell().add(new Paragraph(value).setFont(regular).setFontSize(10)
            .setTextAlignment(TextAlignment.RIGHT)).setPadding(4).setBorder(Border.NO_BORDER));
    }

    private String fmt(BigDecimal value) {
        if (value == null) return "0.00";
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String serviceLabel(String serviceType) {
        if (serviceType == null) return "Car Wash";
        return switch (serviceType.toUpperCase()) {
            case "BASIC"       -> "Basic";
            case "PREMIUM"     -> "Premium";
            case "FULL_DETAIL" -> "Full Detail";
            default            -> serviceType;
        };
    }

    private String nvl(String value) {
        return value != null ? value : "N/A";
    }
}