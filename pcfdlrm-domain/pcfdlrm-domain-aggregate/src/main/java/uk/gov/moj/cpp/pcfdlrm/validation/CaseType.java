package uk.gov.moj.cpp.pcfdlrm.validation;

public enum CaseType {
    CHARGE("C"),
    REQUISITION("Q"),
    SJP("J"),
    SUMMONS("S"),
    OTHER("O");

    private final String code;

    CaseType(final String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
