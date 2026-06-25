package uk.gov.moj.cpp.pcfdlrm;

import static java.util.Arrays.stream;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.justice.core.courts.Gender.NOT_KNOWN;
import static uk.gov.moj.cpp.json.schemas.prosecution.casefile.dlrm.events.DefendantValidationPassed.defendantValidationPassed;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.DEFENDANT_CUSTODY_STATUS_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.DEFENDANT_CUSTODY_TIME_LIMIT_IS_MISSING;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.DEFENDANT_GENDER_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.DEFENDANT_NATIONALITY_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.DEFENDANT_OBSERVED_ETHNICITY_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.DEFENDANT_SELF_DEFINED_ETHNICITY_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.DOCUMENTATION_LANGUAGE_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.HEARING_LANGUAGE_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.PARENT_GUARDIAN_GENDER_INVALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.ValidationRuleExecutor.validate;
import static uk.gov.moj.cpp.pcfdlrm.validation.provider.CcProsecutionValidationRuleProvider.getDefendantValidationRules;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Channel.DLRM_MIGRATION;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.DefendantProblem.defendantProblem;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Language.E;
import static uk.gov.moj.cps.prosecution.casefile.dlrm.domain.event.DefendantValidationFailed.defendantValidationFailed;

import uk.gov.justice.core.courts.Gender;
import uk.gov.moj.cpp.pcfdlrm.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.DefendantsWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.MigratedDefendantWithOffences;
import uk.gov.moj.cpp.pcfdlrm.domain.MigratedHearingWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.refdata.defendant.DefendantRefDataEnricher;
import uk.gov.moj.cpp.pcfdlrm.refdata.hearing.MigratedHearingRefDataEnricher;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.BailStatusReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.DefendantProblem;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Language;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ParentGuardianInformation;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.SelfDefinedInformation;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendantWithProblem;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedHearing;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.plea.json.schemas.InitiationCode;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.ListedDefendant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Strings;
import org.apache.commons.collections.CollectionUtils;

@SuppressWarnings({"squid:S1188", "squid:S3776"})
public class ProsecutionCaseFileHelper {

    private static final String DEFENDANT_GENDER = "defendant.gender";
    private static final String PARENT_GUARDIAN_GENDER = "parentguardian.gender";
    private static final String DOCUMENTATION_LANGUAGE = "defendant.documentationLanguage";
    private static final String HEARING_LANGUAGE = "defendant.hearingLanguage";
    private static final String PARENTGUARDIAN_NOT_PROVIDED = "PARENTGUARDIAN_NOT_PROVIDED";
    private static final String DEFENDANT_SELFINFO_NOT_PROVIDED = "DEFENDANT_SELFINFO_NOT_PROVIDED";
    private static final String IN_CUSTODY="C";

    private ProsecutionCaseFileHelper() {
    }

    public static MigratedDefendantWithProblem validateDefendantErrors(final CaseDetails caseDetails,
                                                                       final Channel channel,
                                                                       final DefendantsWithReferenceData defendantsWithReferenceData,
                                                                       final ReferenceDataQueryService referenceDataQueryService,
                                                                       final Stream.Builder<Object> builder,
                                                                       final Boolean isGroupCase,
                                                                       final String migrationSourceSystemName) {
        final List<DefendantProblem> defendantErrors = new ArrayList<>();
        final List<MigratedDefendant> migratedDefendants = new ArrayList<>();

        defendantsWithReferenceData.getDefendants().forEach(defendant -> {
            MigratedDefendant.Builder migratedDefendantBuilder = MigratedDefendant.migratedDefendant()
                    .withValuesFrom(defendant);

            DefendantWithReferenceData defendantWithReferenceData = new DefendantWithReferenceData(defendant, defendantsWithReferenceData.getReferenceDataVO(), defendantsWithReferenceData.getCaseDetails());
            final String defendantInitiationCode = defendant.getInitiationCode();
            final String initiationCode = defendantInitiationCode != null && isValidInitiationCode(defendantInitiationCode) ? defendant.getInitiationCode() : caseDetails.getInitiationCode();

            final List<Problem> defendantProblemList =
                    validate(defendantWithReferenceData, referenceDataQueryService, getDefendantValidationRules(initiationCode, channel, isGroupCase));

            validateGenderAndLanguage(defendant, defendantProblemList);
            validateCustodyTimeLimit(defendant, defendantProblemList);

            if (CollectionUtils.isNotEmpty(defendantProblemList)) {
                defendantErrors.add(defendantProblem()
                        .withProblems(defendantProblemList)
                        .withProsecutorDefendantReference(Strings.isNullOrEmpty(defendant.getProsecutorDefendantReference()) ?
                                defendant.getAsn() : defendant.getProsecutorDefendantReference())
                        .build()
                );
                if (DLRM_MIGRATION.equals(channel)) {
                    builder.add(defendantValidationFailed()
                            .withDefendant(defendant)
                            .withProblems(defendantProblemList)
                            .withCaseId(caseDetails.getCaseId())
                            .withUrn(caseDetails.getProsecutorCaseReference())
                            .withCaseType(caseDetails.getInitiationCode())
                            .withPoliceSystemId(caseDetails.getPoliceSystemId()).build());

                    if ("XHIBIT".equals(migrationSourceSystemName)) {
                        applyRuleToDefendantFields(migratedDefendantBuilder, referenceDataQueryService, defendantWithReferenceData, defendantProblemList);
                    }
                }
            } else {
                if (DLRM_MIGRATION.equals(channel)) {
                    builder.add(defendantValidationPassed()
                            .withDefendantId(defendant.getId())
                            .withCaseId(caseDetails.getCaseId())
                            .build());
                }
            }
            migratedDefendants.add(migratedDefendantBuilder.build());
        });

        return MigratedDefendantWithProblem.migratedDefendantWithProblem()
                .withDefendantProblems(defendantErrors)
                .withMigratedDefendants(migratedDefendants)
                .build();

    }

    public static MigratedHearingWithReferenceData buildMigratedHearingRefData(List<MigratedHearingRefDataEnricher> enrichers, CaseDetails caseDetails, MigratedHearing migratedHearing, List<MigratedDefendant> migratedDefendants) {

        MigratedHearingWithReferenceData data = new MigratedHearingWithReferenceData();
        data.setCaseDetails(caseDetails);
        data.setMigratedHearing(migratedHearing);
        data.setMigratedDefendantWithOffences(getMigratedDefendantWithOffences(migratedHearing.getListedDefendants(), migratedDefendants));
        enrichers.forEach(x -> x.enrich(data));

        return data;
    }

    private static List<MigratedDefendantWithOffences> getMigratedDefendantWithOffences(
            List<ListedDefendant> listedDefendants,
            List<MigratedDefendant> migratedDefendants) {

        Map<String, MigratedDefendant> migratedDefendantMap = migratedDefendants.stream()
                .collect(Collectors.toMap(MigratedDefendant::getProsecutorDefendantId, md -> md));

        List<MigratedDefendantWithOffences> result = listedDefendants.stream()
                .map(listedDefendant -> findMigratedDefendantWithOffences(listedDefendant, migratedDefendantMap))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        return result.size() < listedDefendants.size() ? List.of() : result;
    }

    private static Optional<MigratedDefendantWithOffences> findMigratedDefendantWithOffences(
            ListedDefendant listedDefendant,
            Map<String, MigratedDefendant> migratedDefendantMap) {

        MigratedDefendant migratedDefendant = migratedDefendantMap.get(listedDefendant.getProsecutorDefendantId());
        if (migratedDefendant == null) {
            return Optional.empty();
        }

        List<UUID> offenceIds = migratedDefendant.getOffences().stream()
                .filter(offence -> listedDefendant.getListedOffences().contains(offence.getProsecutorOffenceId()))
                .map(MigratedOffence::getOffenceId)
                .toList();

        if (offenceIds.size() != listedDefendant.getListedOffences().size()) {
            return Optional.empty();
        }

        return Optional.of(new MigratedDefendantWithOffences(migratedDefendant, offenceIds));
    }


    public static DefendantsWithReferenceData buildDefendantWithReferenceData(final ProsecutionWithReferenceData prosecutionWithReferenceData, final List<DefendantRefDataEnricher> defendantRefDataEnrichers,
                                                                              final String migrationSourceSystemName) {

        final DefendantsWithReferenceData defendantsWithReferenceData = prosecutionWithReferenceData.getReferenceDataVO().getProsecutorsReferenceData() == null ?
                new DefendantsWithReferenceData(prosecutionWithReferenceData.getProsecution().getDefendants()) :
                new DefendantsWithReferenceData(prosecutionWithReferenceData.getProsecution().getDefendants(), prosecutionWithReferenceData.getReferenceDataVO().getProsecutorsReferenceData().getShortName());

        defendantsWithReferenceData.setCaseDetails(prosecutionWithReferenceData.getProsecution().getCaseDetails());
        defendantsWithReferenceData.setReferenceDataVO(prosecutionWithReferenceData.getReferenceDataVO());
        defendantsWithReferenceData.setMigrationSourceSystemName(migrationSourceSystemName);
        defendantRefDataEnrichers.forEach(x -> x.enrich(defendantsWithReferenceData));
        return defendantsWithReferenceData;
    }

    private static boolean isValidInitiationCode(final String initiationCode) {
        return stream(InitiationCode.values())
                .anyMatch(code -> initiationCode.equalsIgnoreCase(String.valueOf(code)));
    }

    private static boolean hasProblem(List<Problem> problems, Enum<?> problemCode) {
        return problems.stream().anyMatch(p -> p.getCode().equals(problemCode.name()));
    }

    private static void applyRuleToDefendantFields(MigratedDefendant.Builder migratedDefendantBuilder, final ReferenceDataQueryService referenceDataQueryService, final DefendantWithReferenceData defendantWithReferenceData, final List<Problem> defendantProblemList) {
        applyHearingAndDocumentationLanguageRule(migratedDefendantBuilder, defendantProblemList);
        applyDefendantAndParentGenderRule(migratedDefendantBuilder, defendantProblemList);

        if (hasProblem(defendantProblemList, DEFENDANT_NATIONALITY_INVALID)) {
            buildMigratedDefendant(migratedDefendantBuilder, null,
                    migratedDefendantBuilder.build().getIndividual().getSelfDefinedInformation().getEthnicity(),
                    migratedDefendantBuilder.build().getIndividual().getPersonalInformation().getObservedEthnicity(),
                    migratedDefendantBuilder.build().getIndividual().getCustodyStatus());
        }

        if (hasProblem(defendantProblemList, DEFENDANT_SELF_DEFINED_ETHNICITY_INVALID)) {
            buildMigratedDefendant(migratedDefendantBuilder,
                    migratedDefendantBuilder.build().getIndividual().getSelfDefinedInformation().getNationality(),
                    null,
                    migratedDefendantBuilder.build().getIndividual().getPersonalInformation().getObservedEthnicity(),
                    migratedDefendantBuilder.build().getIndividual().getCustodyStatus());
        }

        if (hasProblem(defendantProblemList, DEFENDANT_OBSERVED_ETHNICITY_INVALID)) {
            buildMigratedDefendant(migratedDefendantBuilder,
                    migratedDefendantBuilder.build().getIndividual().getSelfDefinedInformation().getNationality(),
                    migratedDefendantBuilder.build().getIndividual().getSelfDefinedInformation().getEthnicity(),
                    null,
                    migratedDefendantBuilder.build().getIndividual().getCustodyStatus());
        }

        if (hasProblem(defendantProblemList, DEFENDANT_CUSTODY_STATUS_INVALID)) {
            buildMigratedDefendant(migratedDefendantBuilder,
                    migratedDefendantBuilder.build().getIndividual().getSelfDefinedInformation().getNationality(),
                    migratedDefendantBuilder.build().getIndividual().getSelfDefinedInformation().getEthnicity(),
                    migratedDefendantBuilder.build().getIndividual().getPersonalInformation().getObservedEthnicity(),
                    "U");

            Optional<BailStatusReferenceData> bailStatusReferenceDataOpt = referenceDataQueryService.retrieveBailStatuses().stream()
                    .filter(custodyStatusReferenceData -> "U".equals(custodyStatusReferenceData.getStatusCode()))
                    .findAny();
            bailStatusReferenceDataOpt.ifPresent(bailStatus -> defendantWithReferenceData.getReferenceDataVO().addBailStatusReferenceData(bailStatus));
        }

        migratedDefendantBuilder.build();
    }

    private static void applyHearingAndDocumentationLanguageRule(MigratedDefendant.Builder builder, List<Problem> problems) {
        if (hasProblem(problems, HEARING_LANGUAGE_INVALID)) {
            builder.withHearingLanguage(E.name());
        }
        if (hasProblem(problems, DOCUMENTATION_LANGUAGE_INVALID)) {
            builder.withDocumentationLanguage(E.name());
        }
    }

    private static void applyDefendantAndParentGenderRule(MigratedDefendant.Builder builder, List<Problem> defendantProblemList) {
        if (hasProblem(defendantProblemList, DEFENDANT_GENDER_INVALID)) {
            builder.withIndividual(Individual.individual()
                    .withValuesFrom(builder.build().getIndividual())
                    .withSelfDefinedInformation(SelfDefinedInformation.selfDefinedInformation()
                            .withValuesFrom(builder.build().getIndividual().getSelfDefinedInformation())
                            .withGender(NOT_KNOWN.name())
                            .build())
                    .build());
        }

        if (hasProblem(defendantProblemList, PARENT_GUARDIAN_GENDER_INVALID)) {
            builder.withIndividual(Individual.individual()
                    .withValuesFrom(builder.build().getIndividual())
                    .withParentGuardianInformation(ParentGuardianInformation.parentGuardianInformation()
                            .withValuesFrom(builder.build().getIndividual().getParentGuardianInformation())
                            .withGender(NOT_KNOWN.name())
                            .build())
                    .build());
        }
    }


    private static void buildMigratedDefendant(MigratedDefendant.Builder defendantBuilder, final String nationality, final String ethnicity, final Integer observedEthnicity, final String custodyStatus) {
        final MigratedDefendant currentDefendant = defendantBuilder.build();
        final Individual currentIndividual = currentDefendant.getIndividual();

        defendantBuilder
                .withValuesFrom(currentDefendant)
                .withIndividual(Individual.individual()
                        .withValuesFrom(currentIndividual)
                        .withCustodyStatus(custodyStatus)
                        .withSelfDefinedInformation(SelfDefinedInformation.selfDefinedInformation()
                                .withValuesFrom(currentIndividual.getSelfDefinedInformation())
                                .withNationality(nationality)
                                .withEthnicity(ethnicity)
                                .build())
                        .withPersonalInformation(PersonalInformation.personalInformation()
                                .withValuesFrom(currentIndividual.getPersonalInformation())
                                .withObservedEthnicity(observedEthnicity)
                                .build())
                        .build());
    }

    private static boolean matchGenderEnum(final String gender) {
        if (isNull(gender)) {
            return true;
        }
        try {
            Gender.valueOf(gender.toUpperCase());
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }

    }

    private static boolean matchLanguageEnum(final String language) {
        if (isNull(language)) {
            return true;
        }
        try {
            Language.valueOf(language.toUpperCase());
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static Problem getProblem(final ProblemCode problemCode, final String fieldName, final String gender) {
        return Problem.problem()
                .withCode(problemCode.name())
                .withValues(Collections.singletonList(ProblemValue.problemValue()
                        .withKey(fieldName)
                        .withValue(gender)
                        .build()))
                .build();
    }

    private static void validateGenderAndLanguage(final MigratedDefendant defendant, final List<Problem> defendantProblemList) {
        final String defendantGender = nonNull(defendant.getIndividual()) && nonNull(defendant.getIndividual().getSelfDefinedInformation()) ?
                defendant.getIndividual().getSelfDefinedInformation().getGender() : DEFENDANT_SELFINFO_NOT_PROVIDED;

        final String parentGender = nonNull(defendant.getIndividual()) && nonNull(defendant.getIndividual().getParentGuardianInformation()) ?
                defendant.getIndividual().getParentGuardianInformation().getGender() : PARENTGUARDIAN_NOT_PROVIDED;

        if (matchGenderEnum(defendantGender) && !DEFENDANT_SELFINFO_NOT_PROVIDED.equals(defendantGender)) {
            defendantProblemList.add(getProblem(DEFENDANT_GENDER_INVALID, DEFENDANT_GENDER, defendantGender));
        }

        if (matchGenderEnum(parentGender) && !PARENTGUARDIAN_NOT_PROVIDED.equals(parentGender)) {
            defendantProblemList.add(getProblem(PARENT_GUARDIAN_GENDER_INVALID, PARENT_GUARDIAN_GENDER, parentGender));
        }

        if (matchLanguageEnum(defendant.getDocumentationLanguage())) {
            defendantProblemList.add(getProblem(DOCUMENTATION_LANGUAGE_INVALID, DOCUMENTATION_LANGUAGE, defendant.getDocumentationLanguage()));
        }

        if (matchLanguageEnum(defendant.getHearingLanguage())) {
            defendantProblemList.add(getProblem(HEARING_LANGUAGE_INVALID, HEARING_LANGUAGE, defendant.getHearingLanguage()));
        }
    }

    private static void validateCustodyTimeLimit(final MigratedDefendant defendant, final List<Problem> defendantProblemList) {
        if (nonNull(defendant.getIndividual()) && nonNull(defendant.getIndividual().getCustodyStatus()) &&
                IN_CUSTODY.equalsIgnoreCase(defendant.getIndividual().getCustodyStatus())) {
            if (isNull(defendant.getIndividual().getCustodyTimeLimit())) {
                defendantProblemList.add(getProblem(DEFENDANT_CUSTODY_TIME_LIMIT_IS_MISSING, "defendant.individual.custodyTimeLimit", DEFENDANT_CUSTODY_TIME_LIMIT_IS_MISSING.name()));
            }
        }
    }

}
