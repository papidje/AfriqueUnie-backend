package friasoft.gn.schoolapp.dto;

import java.util.Map;

public record StudentPaymentStatusDTO(
    Long studentId,
    String lastName,
    String firstName,
    String matricule,
    String phone,
    String insReinsLabel,
    Double insReinsPaid,
    Double insReinsExpected,
    boolean suppliesColumnEnabled,
    boolean hasPaidSupplies,
    Double suppliesExpected,
    Double tuitionPaid,
    Double tuitionExpected,
    Double totalPaid,
    Double totalExpected,
    Double paymentPercentage,
    Map<String, Boolean> monthlyCoverage
) {}

