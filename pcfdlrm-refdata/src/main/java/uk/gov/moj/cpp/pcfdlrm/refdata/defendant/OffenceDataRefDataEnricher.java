package uk.gov.moj.cpp.pcfdlrm.refdata.defendant;

import static java.util.Objects.isNull;
import static uk.gov.moj.cpp.pcfdlrm.refdata.defendant.OffenceLocationHelper.getOffenceLocation;

import uk.gov.moj.cpp.pcfdlrm.domain.DefendantsWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.google.common.collect.Lists;

public class OffenceDataRefDataEnricher implements DefendantRefDataEnricher {

    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Override
    public void enrich(final List<DefendantsWithReferenceData> defendantsWithReferenceDataList) {
        final Map<String, List<OffenceReferenceData>> offenceReferenceDataMap = new HashMap<>();

        for (final DefendantsWithReferenceData defendantsWithReferenceData : defendantsWithReferenceDataList) {

            final List<MigratedDefendant> defendants = defendantsWithReferenceData.getDefendants();
            final List<MigratedOffence> offences = defendants.stream()
                    .flatMap(defendant -> defendant.getOffences().stream())
                    .toList();

            final List<OffenceReferenceData> offenceReferenceDataList = new ArrayList<>();

            offences.forEach(offence -> {
                final String key = offence.getOffenceCode();
                List<OffenceReferenceData> offenceReferenceData = offenceReferenceDataMap.get(key);
                if (isNull(offenceReferenceData)) {
                    offenceReferenceData = referenceDataQueryService.retrieveOffenceDataList(Lists.newArrayList(offence.getOffenceCode()));
                    offenceReferenceDataMap.put(offence.getOffenceCode(), offenceReferenceData);
                }

                if (!isOffenceRefDataExists(offenceReferenceDataList, offence.getOffenceCode())) {
                    offenceReferenceDataList.addAll(offenceReferenceData);
                }
            });

            final List<MigratedDefendant> newDefendants = new ArrayList<>();
            for (final MigratedDefendant defendant : defendants) {
                final List<MigratedOffence> offencesFromList = defendant
                        .getOffences()
                        .stream()
                        .map(offence -> this.createOffenseWithCustomOffenceLocation(offence, defendantsWithReferenceData, offenceReferenceDataMap))
                        .collect(Collectors.toList());

                newDefendants.add(createDefendantWithOffences(defendant, offencesFromList));

            }
            defendantsWithReferenceData.setDefendants(newDefendants);
            defendantsWithReferenceData.getReferenceDataVO().setOffenceReferenceData(offenceReferenceDataList);

        }

    }

    private MigratedDefendant createDefendantWithOffences(final MigratedDefendant defendant, final List<MigratedOffence> offences) {

        return MigratedDefendant.migratedDefendant()
                .withAddress(defendant.getAddress())
                .withAliasForCorporate(defendant.getAliasForCorporate())
                .withAsn(defendant.getAsn())
                .withCroNumber(defendant.getCroNumber())
                .withDocumentationLanguage(defendant.getDocumentationLanguage())
                .withEmailAddress1(defendant.getEmailAddress1())
                .withEmailAddress2(defendant.getEmailAddress2())
                .withHearingLanguage(defendant.getHearingLanguage())
                .withId(defendant.getId())
                .withIndividual(defendant.getIndividual())
                .withIndividualAliases(defendant.getIndividualAliases())
                .withInitiationCode(defendant.getInitiationCode())
                .withLanguageRequirement(defendant.getLanguageRequirement())
                .withOffences(offences)
                .withOrganisationName(defendant.getOrganisationName())
                .withPncIdentifier(defendant.getPncIdentifier())
                .withProsecutorDefendantId(defendant.getProsecutorDefendantId())
                .withProsecutorDefendantReference(defendant.getProsecutorDefendantReference())
                .withSpecificRequirements(defendant.getSpecificRequirements())
                .withTelephoneNumberBusiness(defendant.getTelephoneNumberBusiness())
                .build();

    }

    private MigratedOffence createOffenseWithCustomOffenceLocation(final MigratedOffence offence, DefendantsWithReferenceData enrichedDefendants, final Map<String, List<OffenceReferenceData>> offenceReferenceDataMap ) {

        return MigratedOffence.migratedOffence()
                .withAlcoholRelatedOffence(offence.getAlcoholRelatedOffence())
                .withAllocationDecision(offence.getAllocationDecision())
                .withAppliedCompensation(offence.getAppliedCompensation())
                .withArrestDate(offence.getArrestDate())
                .withBackDuty(offence.getBackDuty())
                .withBackDutyDateFrom(offence.getBackDutyDateFrom())
                .withBackDutyDateTo(offence.getBackDutyDateTo())
                .withChargeDate(offence.getChargeDate())
                .withMaxPenalty(offence.getMaxPenalty())
                .withOffenceCode(offence.getOffenceCode())
                .withOffenceCommittedDate(offence.getOffenceCommittedDate())
                .withOffenceCommittedEndDate(offence.getOffenceCommittedEndDate())
                .withOffenceDateCode(offence.getOffenceDateCode())
                .withOffenceId(offence.getOffenceId())
                .withOffenceLocation(getOffenceLocation(offence, enrichedDefendants.getProsecutionAuthorityShortName()))
                .withOffenceSequenceNumber(offence.getOffenceSequenceNumber())
                .withOffenceTitle(offence.getOffenceTitle())
                .withOffenceTitleWelsh(offence.getOffenceTitleWelsh())
                .withOffenceWording(offence.getOffenceWording())
                .withOffenceWordingWelsh(offence.getOffenceWordingWelsh())
                .withOtherPartyVictim(offence.getOtherPartyVictim())
                .withPlea(offence.getPlea())
                .withProsecutorOfferAOCP(offence.getProsecutorOfferAOCP())
                .withReferenceData(getReferenceData(offence, offenceReferenceDataMap))
                .withStatementOfFacts(offence.getStatementOfFacts())
                .withStatementOfFactsWelsh(offence.getStatementOfFactsWelsh())
                .withVehicleMake(offence.getVehicleMake())
                .withVehicleRegistrationMark(offence.getVehicleRegistrationMark())
                .withVehicleRelatedOffence(offence.getVehicleRelatedOffence())
                .withVerdict(offence.getVerdict())
                .withConvictingCourtCode(offence.getConvictingCourtCode())
                .withCount(offence.getCount())
                .build();

    }

    private OffenceReferenceData getReferenceData(final MigratedOffence offence, final Map<String, List<OffenceReferenceData>> offenceReferenceDataMap) {
        return Optional.ofNullable(offenceReferenceDataMap.get(offence.getOffenceCode()))
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(0))
                .orElse(null);
    }

    private boolean isOffenceRefDataExists(final List<OffenceReferenceData> offenceReferenceDataList, final String offenceCode) {
        return offenceReferenceDataList.stream().anyMatch(offenceReferenceData -> offenceReferenceData.getCjsOffenceCode().equals(offenceCode));
    }

}