package friasoft.gn.schoolapp.service.document;

import friasoft.gn.schoolapp.dto.FinancePaymentDtos.PaymentReceiptView;
import friasoft.gn.schoolapp.dto.FinancePaymentDtos.ReceiptLine;
import friasoft.gn.schoolapp.entity.school.School;
import friasoft.gn.schoolapp.entity.school.Payment;
import friasoft.gn.schoolapp.entity.school.Student;
import friasoft.gn.schoolapp.repository.IStudentRepository;
import friasoft.gn.schoolapp.service.FinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentReceiptPdfService {

    private static final DateTimeFormatter DT_FR =
        DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm", Locale.FRANCE);
    private static final NumberFormat AMOUNT_FR = NumberFormat.getNumberInstance(Locale.FRANCE);

    private final FinanceService financeService;
    private final IStudentRepository studentRepository;
    private final SchoolDocumentBranding schoolDocumentBranding;
    private final PdfService pdfService;

    public byte[] buildReceiptPdf(Long studentId, String reference, boolean duplicate) {
        PaymentReceiptView view = financeService.getReceipt(studentId, reference, duplicate);
        Student student = studentRepository.findByIdWithParentsAndClass(studentId)
            .orElseThrow(() -> new IllegalStateException("Élève introuvable."));
        if (student.getSchoolClass() == null || student.getSchoolClass().getYear() == null
            || student.getSchoolClass().getYear().getSchool() == null) {
            throw new IllegalStateException("Contexte école introuvable.");
        }
        School school = student.getSchoolClass().getYear().getSchool();

        Map<String, Object> model = new HashMap<>();
        schoolDocumentBranding.putSchoolOnModel(school, model);

        model.put("receiptReference", view.receiptReference());
        model.put("paymentDate", view.paymentDate() != null ? DT_FR.format(view.paymentDate()) : "—");
        model.put("studentName", nullToDash(view.studentName()));
        model.put("matricule", nullToDash(view.matricule()));
        model.put("schoolYearLabel", nullToDash(view.schoolYearLabel()));
        model.put("paymentMode", paymentModeFr(view.paymentMode()));
        model.put("recordedBy", nullToDash(view.recordedBy()));
        model.put("totalAmount", formatAmount(view.totalCollected(), view.currency()));
        model.put("totalInWords", GnfAmountInWords.format(view.totalCollected() != null ? view.totalCollected() : 0d));
        model.put("duplicate", view.duplicate());
        model.put("balanceRemaining", formatAmount(view.balanceRemaining(), view.currency()));
        model.put("receiptLineRows", buildLineRows(view));

        return pdfService.renderFromTemplate("documents/recu-paiement", model);
    }

    private static List<Map<String, String>> buildLineRows(PaymentReceiptView view) {
        String cur = view.currency() != null ? view.currency() : "GNF";
        List<Map<String, String>> rows = new ArrayList<>();
        for (ReceiptLine line : view.lines()) {
            Map<String, String> row = new HashMap<>();
            row.put("label", receiptLineLabel(line));
            row.put("amount", formatAmount(line.amount(), cur));
            rows.add(row);
        }
        return rows;
    }

    private static String receiptLineLabel(ReceiptLine line) {
        if (line == null) {
            return "—";
        }
        String type = line.paymentType() != null ? line.paymentType().trim() : "";
        if (type.isEmpty()) {
            return "—";
        }
        if (Payment.PaymentType.SCOLARITE.name().equals(type)) {
            if (line.tuitionMonthLabel() != null && !line.tuitionMonthLabel().isBlank()) {
                return "Scolarité — " + line.tuitionMonthLabel().trim();
            }
            return "Scolarité";
        }
        try {
            return switch (Payment.PaymentType.valueOf(type)) {
                case INSCRIPTION -> "Inscription";
                case REINSCRIPTION -> "Réinscription";
                case FOURNITURES -> "Fournitures";
                case SCOLARITE -> "Scolarité";
            };
        } catch (IllegalArgumentException e) {
            return type;
        }
    }

    private static String paymentModeFr(String mode) {
        if (mode == null || mode.isBlank()) {
            return "Espèces";
        }
        try {
            return switch (Payment.PaymentMode.valueOf(mode.trim().toUpperCase(Locale.ROOT))) {
                case ESPECES -> "Espèces";
                case ORANGE_MONEY -> "Orange Money";
                case MOOV_MONEY -> "Moov Money";
                case VIREMENT -> "Virement bancaire";
            };
        } catch (IllegalArgumentException e) {
            return mode;
        }
    }

    private static String formatAmount(Double amount, String currency) {
        if (amount == null) {
            return "0";
        }
        boolean gnf = currency == null || "GNF".equalsIgnoreCase(currency.trim());
        if (gnf) {
            return AMOUNT_FR.format(Math.round(amount)) + " GNF";
        }
        return AMOUNT_FR.format(amount) + " " + (currency != null ? currency : "");
    }

    private static String nullToDash(String s) {
        if (s == null || s.isBlank()) {
            return "—";
        }
        return s.trim();
    }
}
