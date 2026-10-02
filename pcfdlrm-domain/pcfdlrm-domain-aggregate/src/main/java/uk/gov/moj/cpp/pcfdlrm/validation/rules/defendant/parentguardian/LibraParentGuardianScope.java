package uk.gov.moj.cpp.pcfdlrm.validation.rules.defendant.parentguardian;

import static uk.gov.moj.cpp.pcfdlrm.validation.CaseType.CHARGE;
import static uk.gov.moj.cpp.pcfdlrm.validation.CaseType.REQUISITION;
import static uk.gov.moj.cpp.pcfdlrm.validation.CaseType.SUMMONS;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Channel.DLRM_MIGRATION;

import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Channel;

import java.util.Set;

/**
 * DD-43501: the LIBRA parent/guardian rules, sanitiser and rejection apply only to DLRM migration cases from
 * LIBRA (exact match, like {@code isXhibit}) whose resolved initiation code is Summons, Charge or
 * Requisition. SJP ({@code J}), Remittance ({@code R}) and Other keep today's behaviour.
 */
public final class LibraParentGuardianScope {

    private static final String LIBRA = "LIBRA";
    private static final Set<String> IN_SCOPE_INITIATION_CODES = Set.of(SUMMONS.getCode(), CHARGE.getCode(), REQUISITION.getCode());

    private LibraParentGuardianScope() {
    }

    public static boolean applies(final Channel channel, final String migrationSourceSystemName, final String initiationCode) {
        return DLRM_MIGRATION.equals(channel)
                && LIBRA.equals(migrationSourceSystemName)
                && initiationCode != null
                && IN_SCOPE_INITIATION_CODES.contains(initiationCode);
    }
}
