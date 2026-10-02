package uk.gov.moj.cpp.pcfdlrm.validation;

public enum Constants {

    //Email pattern copied from core domain
    EMAIL("^[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?$"),

    PNC_ID_REGEX("^(?!0{4}|0{3}|0{2}}|0{1})([0-9][0-9]{3})([/])([0-9]{7})([a-zA-Z]|[0-9])$"),

    PNC_ID_REGEX_SPI("[0-9A-Za-z'\\.\\-_]{0,13}"),

    CRO_NUMBER_REGEX_SPI("^.{0,12}$"), //As per existing SPI XSD Schema format which does allow empty spaces

    CRO_NUMBER_REGEX_ONE("^([S][F])(\\d{2})([/])([0-9]{6})([a-zA-Z]|[0-9])$"),

    CRO_NUMBER_REGEX_TWO("^(\\d{6})([/])(\\d{2})([a-zA-Z]|[0-9]{1})$"),

    POST_CODE_REGEX("^(([gG][iI][rR] {0,}0[aA]{2})|(([aA][sS][cC][nN]|[sS][tT][hH][lL]|[tT][dD][cC][uU]|[bB][bB][nN][dD]|[bB][iI][qQ][qQ]|[fF][iI][qQ][qQ]|[pP][cC][rR][nN]|[sS][iI][qQ][qQ]|[iT][kK][cC][aA]) {0,}1[zZ]{2})|((([a-pr-uwyzA-PR-UWYZ][a-hk-yxA-HK-XY]?[0-9][0-9]?)|(([a-pr-uwyzA-PR-UWYZ][0-9][a-hjkstuwA-HJKSTUW])|([a-pr-uwyzA-PR-UWYZ][a-hk-yA-HK-Y][0-9][abehmnprv-yABEHMNPRV-Y]))) {0,}[0-9][abd-hjlnp-uw-zABD-HJLNP-UW-Z]{2}))$"),

    // DD-43501 (LIBRA parent/guardian only): single character classes with simple quantifiers, so no catastrophic backtracking
    CP_TELEPHONE("^[0-9+ \\-]{10,}$"),

    LIBRA_GUARDIAN_EMAIL("^[0-9A-Za-z'._-]{1,127}@[0-9A-Za-z'._-]{1,127}$"),

    NO_FIXED_ABODE_POST_CODE("^[zZ][zZ]99 ?[0-9][a-zA-Z]{2}$");


    private String value;

    Constants(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}