package uk.gov.moj.cpp.pcfdlrm.event.processor.convertor;

import static java.util.Arrays.asList;
import static java.util.List.of;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.pcfdlrm.event.processor.utils.CaseReceivedHelper.buildOffences;
import static uk.gov.moj.cpp.pcfdlrm.event.processor.utils.CaseReceivedHelper.buildReferenceDataIncludingDvlaCode;
import static uk.gov.moj.cpp.pcfdlrm.event.processor.utils.CaseReceivedHelper.buildReferenceDataWithGuiltyPlea;
import static uk.gov.moj.cpp.pcfdlrm.event.processor.utils.CaseReceivedHelper.buildReferenceDataWithNotGuiltyPlea;
import static uk.gov.moj.cpp.pcfdlrm.event.processor.utils.CaseReceivedHelper.buildReferenceDataWithOffenceAndModeOfTrial;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.AlcoholLevelMethodReferenceData.alcoholLevelMethodReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.AlcoholRelatedOffence.alcoholRelatedOffence;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Channel.DLRM_MIGRATION;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Channel.MCC;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Channel.SPI;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OffenceReferenceData.offenceReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OrganisationUnitReferenceData.organisationUnitReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedAllocationDecision.migratedAllocationDecision;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence.migratedOffence;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedPlea.migratedPlea;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedVerdict.migratedVerdict;

import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.moj.cpp.pcfdlrm.domain.OffenceIdsWithCourtHearingLocation;
import uk.gov.moj.cpp.pcfdlrm.domain.ParamsVO;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.OrganisationUnitReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.PleaReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.VehicleRelatedOffence;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.VerdictReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedPlea;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedVerdict;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class ProsecutionCaseFileMigratedOffenceToCourtsOffenceConverterTest {

    private final String EITHER_WAY = "Either Way";
    private final String SUMMARY = "Summary";
    private final String XHIBIT = "XHIBIT";
    private static final String INVALID = "INVALID";
    private final String INDICTABLE = "Indictable";
    private static final String GUILTY = "GUILTY";
    private static final String GUILTY_CATEGORY = "Guilty";
    private final String NOTGUILTY_PLEA_VALUE = "NOTGUILTY";
    private final String NJOJ_VERDICT_CODE = "NJOJ";
    private final String OFFENCE_CODE_TVL_ABC = "TVL-ABC";
    private static final String NOT_GUILTY = "NOT GUILTY";
    private final String OUCODE_VALID = "C01BL00";

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @InjectMocks
    private ProsecutionCaseFileMigratedOffenceToCourtsOffenceConverter converter;

    @Test
    void shouldBuildAllocationDecisionWithSummaryOnlyWhenOffenceMoTIsSummaryAndNoAllocationDecision() {
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(SUMMARY);
        final UUID offenceId = randomUUID();

        final List<MigratedOffence> offences = of(migratedOffence()
                .withOffenceId(offenceId)
                .withOffenceCode(OFFENCE_CODE_TVL_ABC)
                .withCount(3)
                .withOffenceCommittedDate(LocalDate.now())
                .withReferenceData(offenceReferenceData()
                        .withModeOfTrialDerived(SUMMARY)
                        .build())
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setReferenceDataVO(referenceDataVO);
        paramsVO.setInitiationCode(InitiationCode.O.name());
        paramsVO.setMigrationSourceSystemName(XHIBIT);

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);

        final uk.gov.justice.core.courts.Offence offence = coreOffences.get(0);
        assertThat(offence.getId(), is(offenceId));
        assertThat(offence.getCount(), is(3));
        assertThat(coreOffences.get(0).getAllocationDecision(), is(notNullValue()));
        assertThat(coreOffences.get(0).getAllocationDecision().getMotReasonId(), is(notNullValue()));
    }

    @Test
    void shouldNotSetAllocationDecisionWhenOffenceMoTIsOtherAndNoAllocationDecision() {
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(SUMMARY);
        final UUID offenceId = randomUUID();

        final List<MigratedOffence> offences = of(migratedOffence()
                .withOffenceId(offenceId)
                .withOffenceCode(OFFENCE_CODE_TVL_ABC)
                .withOffenceCommittedDate(LocalDate.now())
                .withReferenceData(offenceReferenceData()
                        .withModeOfTrialDerived(INDICTABLE)
                        .build())
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setReferenceDataVO(referenceDataVO);
        paramsVO.setChannel(MCC);
        paramsVO.setInitiationCode(InitiationCode.O.name());
        paramsVO.setMigrationSourceSystemName(XHIBIT);

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);
        assertThat(coreOffences.get(0).getAllocationDecision(), is(nullValue()));
    }

    @Test
    void shouldSetAllocationDecisionWhenOffenceIsSummaryOnlyAndAllocationDecisionIsIndictable() {
        UUID motReasonId = randomUUID();
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(SUMMARY, motReasonId.toString());
        final UUID offenceId = randomUUID();
        final String offenceCode = OFFENCE_CODE_TVL_ABC;

        final List<MigratedOffence> offences = of(migratedOffence()
                .withOffenceId(offenceId)
                .withOffenceCode(offenceCode)
                .withOffenceCommittedDate(LocalDate.now())
                .withAllocationDecision(migratedAllocationDecision()
                        .withMotReasonId(motReasonId)
                        .build())
                .withReferenceData(offenceReferenceData()
                        .withModeOfTrialDerived(SUMMARY)
                        .build())
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setReferenceDataVO(referenceDataVO);
        paramsVO.setInitiationCode(InitiationCode.J.name());
        paramsVO.setMigrationSourceSystemName(XHIBIT);

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);
        assertThat(coreOffences.get(0).getAllocationDecision(), is(notNullValue()));
        assertThat(coreOffences.get(0).getAllocationDecision().getMotReasonId(), is(motReasonId));
    }

    @Test
    void shouldSetAllocationDecisionWhenOffenceIsIndictableAndAllocationDecisionIsSummary() {
        final UUID motReasonId = randomUUID();
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(SUMMARY, motReasonId.toString());
        final UUID offenceId = randomUUID();

        final List<MigratedOffence> offences = of(migratedOffence()
                .withOffenceId(offenceId)
                .withOffenceCode(OFFENCE_CODE_TVL_ABC)
                .withOffenceCommittedDate(LocalDate.now())
                .withAllocationDecision(migratedAllocationDecision()
                        .withMotReasonId(UUID.fromString(referenceDataVO.getModeOfTrialReasonsReferenceData().get(0).getId()))
                        .build())
                .withReferenceData(offenceReferenceData()
                        .withModeOfTrialDerived(INDICTABLE)
                        .build())
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setReferenceDataVO(referenceDataVO);
        paramsVO.setInitiationCode(InitiationCode.J.name());
        paramsVO.setMigrationSourceSystemName(XHIBIT);

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);
        assertThat(coreOffences.get(0).getAllocationDecision(), is(notNullValue()));
        assertThat(coreOffences.get(0).getAllocationDecision().getMotReasonId(), is(UUID.fromString(referenceDataVO.getModeOfTrialReasonsReferenceData().get(0).getId())));
    }

    @Test
    void shouldSetAllocationDecisionWhenOffenceIsSummaryAndAllocationDecisionIsSummary() {
        final UUID motReasonId = randomUUID();
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(SUMMARY, motReasonId.toString());
        final UUID offenceId = randomUUID();

        final List<MigratedOffence> offences = of(migratedOffence()
                .withOffenceId(offenceId)
                .withOffenceCode(OFFENCE_CODE_TVL_ABC)
                .withOffenceCommittedDate(LocalDate.now())
                .withAllocationDecision(migratedAllocationDecision()
                        .withMotReasonId(UUID.fromString(referenceDataVO.getModeOfTrialReasonsReferenceData().get(0).getId()))
                        .build())
                .withReferenceData(offenceReferenceData()
                        .withModeOfTrialDerived(INDICTABLE)
                        .build())
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setReferenceDataVO(referenceDataVO);
        paramsVO.setInitiationCode(InitiationCode.J.name());
        paramsVO.setMigrationSourceSystemName(XHIBIT);

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);
        assertThat(coreOffences.get(0).getAllocationDecision(), is(notNullValue()));
        assertThat(coreOffences.get(0).getAllocationDecision().getMotReasonId(), is(UUID.fromString(referenceDataVO.getModeOfTrialReasonsReferenceData().get(0).getId())));
    }

    @Test
    void shouldSetAllocationDecisionWhenOffenceIsIndictableAndAllocationDecisionIsIndictable() {
        final UUID motReasonId = randomUUID();
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(SUMMARY, motReasonId.toString());
        final UUID offenceId = randomUUID();

        final List<MigratedOffence> offences = of(migratedOffence()
                .withOffenceId(offenceId)
                .withOffenceCode(OFFENCE_CODE_TVL_ABC)
                .withOffenceCommittedDate(LocalDate.now())
                .withAllocationDecision(migratedAllocationDecision()
                        .withMotReasonId(motReasonId)
                        .build())
                .withReferenceData(offenceReferenceData()
                        .withModeOfTrialDerived(INDICTABLE)
                        .build())
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setReferenceDataVO(referenceDataVO);
        paramsVO.setInitiationCode(InitiationCode.J.name());
        paramsVO.setMigrationSourceSystemName(XHIBIT);

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);
        assertThat(coreOffences.get(0).getAllocationDecision(), is(notNullValue()));
        assertThat(coreOffences.get(0).getAllocationDecision().getMotReasonId(), is(motReasonId));
    }

    @ParameterizedTest
    @CsvSource({"G", "IG"})
    void shouldSetConvictionDateAsPleaDateWhenPleaIsGuilty(String pleaCode) {
        final UUID offenceId = randomUUID();
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithGuiltyPlea(EITHER_WAY, randomUUID().toString(), offenceId.toString());
        final String convictionDate = LocalDate.now().minusDays(3).toString();
        final List<MigratedOffence> offences = of(migratedOffence()
                .withOffenceId(offenceId)
                .withOffenceCode(OFFENCE_CODE_TVL_ABC)
                .withOffenceCommittedDate(LocalDate.now())
                .withReferenceData(offenceReferenceData()
                        .withModeOfTrialDerived(SUMMARY)
                        .build())
                .withPlea(migratedPlea()
                        .withId(randomUUID())
                        .withPleaDate(LocalDate.now().minusDays(3))
                        .build())
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setReferenceDataVO(referenceDataVO);
        paramsVO.setInitiationCode(InitiationCode.J.name());
        paramsVO.setMigrationSourceSystemName(XHIBIT);

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);
        assertThat(coreOffences.get(0).getConvictionDate(), is(convictionDate));
    }

    @Test
    void convertToCourtsOffenceWithDVLACode() {
        final ReferenceDataVO referenceDataVO = buildReferenceDataIncludingDvlaCode();
        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName(XHIBIT);
        paramsVO.setReferenceDataVO(referenceDataVO);

        when(referenceDataQueryService.retrieveAlcoholLevelMethods()).thenReturn(asList(alcoholLevelMethodReferenceData().withMethodCode("A").withMethodDescription("Blood").build(),
                alcoholLevelMethodReferenceData().withMethodCode("B").withMethodDescription("Breath").build()));

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(
                buildOffences(), paramsVO);

        assertThat(coreOffences.get(0).getDvlaOffenceCode(), is("dvlaCode"));
    }

    @Test
    void convertToCourtsOffenceWithNoDVLACode() {
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(EITHER_WAY);
        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName(XHIBIT);
        paramsVO.setReferenceDataVO(referenceDataVO);

        when(referenceDataQueryService.retrieveAlcoholLevelMethods()).thenReturn(asList(alcoholLevelMethodReferenceData().withMethodCode("A").withMethodDescription("Blood").build(),
                alcoholLevelMethodReferenceData().withMethodCode("B").withMethodDescription("Breath").build()));

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(
                buildOffences(), paramsVO);

        assertNull(coreOffences.get(0).getDvlaOffenceCode());
    }


    @Test
    void convertToCourtsOffenceWithMaxPenalty() {
        final ReferenceDataVO referenceDataVO = buildReferenceDataIncludingDvlaCode();
        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName(XHIBIT);
        paramsVO.setReferenceDataVO(referenceDataVO);

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(
                buildOffences(), paramsVO);

        assertThat(coreOffences.get(0).getMaxPenalty(), is("Max Penalty"));
    }

    @Test
    void shouldConvertOffences() {
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(EITHER_WAY);
        final UUID offenceId = randomUUID();
        final LocalDate arrestDate = LocalDate.now().minusDays(3);
        final LocalDate chargeDate = LocalDate.now().minusDays(2);
        final String offenceCode = OFFENCE_CODE_TVL_ABC;
        final int offenceSequenceNumber = 2;
        final LocalDate committedDate = LocalDate.now();
        final String offenceDefinitionId = "d8c63737-3c60-496b-94bb-30faa761f00a";

        final LocalDate offenceCommittedEndDate = LocalDate.now().plusDays(2);
        final LocalDate laidDate = LocalDate.now().plusDays(1);
        final String offenceWording = "offence wording";
        final String offenceWordingWelsh = "offence wording welsh";
        final Integer offenceDateCode = 3;
        final int alcoholLevelAmount = 5;
        final LocalDate pleaDate = LocalDate.now().minusDays(1);
        final LocalDate verdictDate = LocalDate.now().minusDays(2);
        final UUID guiltyVerdictId = randomUUID();
        final List<MigratedOffence> offences = of(migratedOffence()
                .withOffenceId(offenceId)
                .withArrestDate(arrestDate)
                .withChargeDate(chargeDate)
                .withOffenceCode(offenceCode)
                .withVehicleRelatedOffence(new VehicleRelatedOffence("OTHER", "L"))
                .withAlcoholRelatedOffence(alcoholRelatedOffence()
                        .withAlcoholLevelMethod("A")
                        .withAlcoholLevelAmount(alcoholLevelAmount).build())
                .withOffenceSequenceNumber(offenceSequenceNumber)
                .withOffenceCommittedDate(committedDate)
                .withOffenceCommittedEndDate(offenceCommittedEndDate)
                .withOffenceWording(offenceWording)
                .withOffenceWordingWelsh(offenceWordingWelsh)
                .withOffenceDateCode(offenceDateCode)
                .withPlea(migratedPlea()
                        .withId(randomUUID())
                        .withPleaDate(pleaDate)
                        .build())
                .withVerdict(migratedVerdict()
                        .withId(guiltyVerdictId)
                        .withVerdictDate(verdictDate)
                        .build())
                .withReferenceData(offenceReferenceData()
                        .withModeOfTrialDerived(SUMMARY)
                        .build())
                .withMaxPenalty("Max Penalty")
                .build());

        final Map<UUID, Map<UUID, PleaReferenceData>> pleaReferenceDataMap = new HashMap<>();
        pleaReferenceDataMap.put(randomUUID(), Map.of(offenceId, PleaReferenceData.pleaReferenceData().
                withPleaValue(NOTGUILTY_PLEA_VALUE).withPleaTypeCode("G").build()));

        referenceDataVO.setPleaReferenceDataMap(pleaReferenceDataMap);

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName(XHIBIT);
        paramsVO.setReferenceDataVO(referenceDataVO);

        final Map<UUID, Map<UUID, VerdictReferenceData>> verdictReferenceDataMap = new HashMap<>();
        verdictReferenceDataMap.put(randomUUID(), Map.of(offenceId, VerdictReferenceData.verdictReferenceData()
                .withVerdictCode(NJOJ_VERDICT_CODE)
                .withCategory(GUILTY_CATEGORY)
                .withCategoryType(GUILTY)
                .build()));

        referenceDataVO.setVerdictReferenceDataMap(verdictReferenceDataMap);

        when(referenceDataQueryService.retrieveAlcoholLevelMethods()).thenReturn(asList(alcoholLevelMethodReferenceData().withMethodCode("A").withMethodDescription("Blood").build(),
                alcoholLevelMethodReferenceData().withMethodCode("B").withMethodDescription("Breath").build()));


        List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);

        verify(referenceDataQueryService, never()).retrieveOrganisationUnits(anyString());

        assertThat(coreOffences.size(), is(1));
        final uk.gov.justice.core.courts.Offence offence = coreOffences.get(0);
        assertThat(offence.getId(), is(offenceId));
        assertThat(offence.getArrestDate(), is(arrestDate.toString()));
        assertThat(offence.getChargeDate(), is(chargeDate.toString()));
        assertThat(offence.getCount(), is(0));
        assertThat(offence.getOffenceCode(), is(offenceCode));
        assertThat(offence.getOffenceDefinitionId(), is(fromString(offenceDefinitionId)));
        assertThat(offence.getOffenceFacts().getAlcoholReadingAmount(), is(alcoholLevelAmount));
        assertThat(offence.getOffenceFacts().getAlcoholReadingMethodCode(), is("A"));
        assertThat(offence.getOffenceFacts().getAlcoholReadingMethodDescription(), is("Blood"));
        assertThat(offence.getOffenceFacts().getVehicleRegistration(), is("L"));
        assertThat(offence.getOffenceTitle(), is("Offence Tittle"));
        assertThat(offence.getOffenceTitleWelsh(), is("Offence Tittle Welsh"));
        assertThat(offence.getOrderIndex(), is(offenceSequenceNumber));
        assertThat(offence.getStartDate(), is(committedDate.toString()));
        assertThat(offence.getEndDate(), is(offenceCommittedEndDate.toString()));
        assertNull(offence.getLaidDate());
        assertThat(offence.getWording(), is(offenceWording));
        assertThat(offence.getWordingWelsh(), is(offenceWordingWelsh));
        assertThat(offence.getModeOfTrial(), is("Either Way"));
        assertThat(offence.getOffenceLegislation(), is("offenceLegalisation"));
        assertThat(offence.getOffenceLegislationWelsh(), is("offenceLegalisationWelsh"));
        assertThat(offence.getOffenceDateCode(), is(offenceDateCode));
        assertNull(offence.getCommittingCourt());

        assertThat(offence.getPlea().getOffenceId(), is(offence.getId()));
        assertThat(offence.getPlea().getPleaDate(), is(pleaDate.toString()));
        assertThat(offence.getPlea().getPleaValue(), is(NOTGUILTY_PLEA_VALUE));
        assertThat(offence.getVerdict().getOffenceId(), is(offence.getId()));
        assertThat(offence.getVerdict().getVerdictDate(), is(verdictDate.toString()));
        assertThat(offence.getVerdict().getVerdictType().getCategory(), is(GUILTY_CATEGORY));
        assertThat(offence.getVerdict().getVerdictType().getCategoryType(), is(GUILTY));
        assertThat(offence.getMaxPenalty(), is("Max Penalty"));
    }

    @Test
    void shouldConvertOffencesWithCommittingCourtWhenCaseIsMccAndTrialAndCourtLocationExists() {
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(EITHER_WAY);
        final String courtReceivedFromCode = "B01KR00";
        final String courtCentreId = randomUUID().toString();
        final UUID offenceId = randomUUID();
        final String oucodeL1Code = "B";
        final String oucodeL3Code = "KR";
        final String oucodeL3Name = "Barkingside Magistrates' Court";
        final LocalDate committedDate = LocalDate.now();

        final String summaryOnly = "Summary only";
        final List<MigratedOffence> offences = of(migratedOffence()
                .withOffenceId(offenceId)
                .withOffenceCode(OFFENCE_CODE_TVL_ABC)
                .withOffenceCommittedDate(committedDate)
                .withReferenceData(offenceReferenceData()
                        .withModeOfTrial(summaryOnly)
                        .build())
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName(XHIBIT);
        paramsVO.setReferenceDataVO(referenceDataVO);
        paramsVO.setReceivedFromCourtOUCode(courtReceivedFromCode);
        paramsVO.setChannel(MCC);
        paramsVO.setInitiationCode(InitiationCode.O.name());

        when(referenceDataQueryService.retrieveOrganisationUnits(eq(courtReceivedFromCode)))
                .thenReturn(List.of(organisationUnitReferenceData()
                        .withId(courtCentreId)
                        .withOucodeL1Code(oucodeL1Code)
                        .withOucodeL3Code(oucodeL3Code)
                        .withOucodeL3Name(oucodeL3Name)
                        .build()));
        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);

        verify(referenceDataQueryService).retrieveOrganisationUnits(courtReceivedFromCode);

        final uk.gov.justice.core.courts.Offence offence = coreOffences.get(0);
        assertThat(offence.getId(), is(offenceId));

        assertThat(offence.getCommittingCourt().getCourtCentreId(), is(fromString(courtCentreId)));
        assertThat(offence.getCommittingCourt().getCourtHouseType(), is(JurisdictionType.MAGISTRATES));
        assertThat(offence.getCommittingCourt().getCourtHouseCode(), is(oucodeL3Code));
        assertThat(offence.getCommittingCourt().getCourtHouseName(), is(oucodeL3Name));
        assertThat(offence.getCommittingCourt().getCourtHouseShortName(), is(oucodeL3Name));
    }

    @Test
    void shouldSetConvictionDateBasedOnGuiltyPleaDate() {
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(EITHER_WAY);
        final UUID offenceId = randomUUID();
        final LocalDate pleaDate = LocalDate.now().minusDays(1);
        final Map<UUID, Map<UUID, PleaReferenceData>> pleaReferenceDataMap = new HashMap<>();
        pleaReferenceDataMap.put(randomUUID(), Map.of(offenceId, PleaReferenceData.pleaReferenceData().
                withPleaValue(GUILTY).withPleaTypeCode("G").withPleaTypeGuiltyFlag("Yes").build()));

        referenceDataVO.setPleaReferenceDataMap(pleaReferenceDataMap);

        final List<MigratedOffence> offences = of(migratedOffence()
                .withOffenceId(offenceId)
                .withOffenceCode(OFFENCE_CODE_TVL_ABC)
                .withOffenceCommittedDate(LocalDate.now())
                .withConvictingCourtCode(OUCODE_VALID)
                .withPlea(migratedPlea()
                        .withId(randomUUID())
                        .withPleaDate(pleaDate)
                        .build())
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setReferenceDataVO(referenceDataVO);

        paramsVO.setChannel(MCC);
        paramsVO.setInitiationCode(InitiationCode.O.name());
        paramsVO.setMigrationSourceSystemName("LIBRA");
        OrganisationUnitReferenceData organisationUnitReferenceData = OrganisationUnitReferenceData.organisationUnitReferenceData()
                .withId("89592405-c29b-3706-b1d3-b1dd3a08b227")
                .withOucode(OUCODE_VALID)
                .withOucodeL3Code("BL")
                .withOucodeL3Name("Blackfriars Crown Court")
                .withOucodeL1Code("C")
                .withOucodeL1Name("Crown Courts")
                .withOucodeL2Code("1")
                .withOucodeL2Name("London")
                .withLja("2570")
                .withOucodeEffectiveFromDate("2016-01-01")
                .withAddress1("1-15 Pocock Street")
                .withAddress2("London")
                .withPostcode("SE1 0BJ")
                .withOucodeL3WelshName("Llys Y Goron Blackfriars")
                .withDefaultStartTime("10:00:00")
                .withDefaultDurationHrs("07:00:00")
                .withEmail("London.crowncourt@cps.gov.uk")
                .withCourtLocationCode("0428")
                .withAdditionalProperty("pecsContractorEmail", "PECSBlackfriarsCrown@serco.com")
                .withAdditionalProperty("divisionCode", "1")
                .withAdditionalProperty("courtId", "428")
                .build();

        when(referenceDataQueryService.retrieveOrganisationUnits(OUCODE_VALID)).thenReturn(List.of(organisationUnitReferenceData));

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);

        final uk.gov.justice.core.courts.Offence offence = coreOffences.get(0);
        assertThat(offence.getId(), is(offenceId));
        assertThat(offence.getConvictionDate(), is(pleaDate.toString()));
    }

    @Test
    void shouldNotSetConvictionDateWhenNotGuiltyPleaAndVerdict() {
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(EITHER_WAY);
        final UUID offenceId = randomUUID();
        final LocalDate pleaDate = LocalDate.now().minusDays(1);
        final LocalDate verdictDate = LocalDate.now().minusDays(1);
        final String offenceCode = OFFENCE_CODE_TVL_ABC;
        final UUID guiltyVerdictId = randomUUID();

        final List<MigratedOffence> offences = of(migratedOffence()
                .withOffenceId(offenceId)
                .withOffenceCode(offenceCode)
                .withOffenceCommittedDate(LocalDate.now())
                .withPlea(migratedPlea()
                        .withId(randomUUID())
                        .withPleaDate(pleaDate)
                        .build())
                .withVerdict(migratedVerdict()
                        .withId(guiltyVerdictId)
                        .withVerdictDate(verdictDate)
                        .build())
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName(XHIBIT);
        paramsVO.setReferenceDataVO(referenceDataVO);
        paramsVO.setChannel(MCC);
        paramsVO.setInitiationCode(InitiationCode.O.name());

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);

        final uk.gov.justice.core.courts.Offence offence = coreOffences.get(0);
        assertThat(offence.getId(), is(offenceId));
        assertNull(offence.getConvictionDate());
    }

    @Test
    void shouldSetConvictionDateFromVerdictWhenGuiltyVerdict() {
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(EITHER_WAY);
        final UUID offenceId = randomUUID();
        final LocalDate verdictDate = LocalDate.now().minusDays(1);
        final String offenceCode = OFFENCE_CODE_TVL_ABC;
        final UUID guiltyVerdictId = randomUUID();

        final List<MigratedOffence> offences = of(migratedOffence()
                .withOffenceId(offenceId)
                .withOffenceCode(offenceCode)
                .withOffenceCommittedDate(LocalDate.now())
                .withVerdict(migratedVerdict()
                        .withId(guiltyVerdictId)
                        .withVerdictDate(verdictDate)
                        .build())
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName(XHIBIT);
        paramsVO.setReferenceDataVO(referenceDataVO);
        paramsVO.setChannel(MCC);
        paramsVO.setInitiationCode(InitiationCode.O.name());

        final Map<UUID, Map<UUID, VerdictReferenceData>> verdictReferenceDataMap = new HashMap<>();
        verdictReferenceDataMap.put(randomUUID(), Map.of(offenceId, VerdictReferenceData.verdictReferenceData()
                .withVerdictCode("G")
                .withCategory(GUILTY_CATEGORY)
                .withCategoryType(GUILTY)
                .build()));

        referenceDataVO.setVerdictReferenceDataMap(verdictReferenceDataMap);

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);

        final uk.gov.justice.core.courts.Offence offence = coreOffences.get(0);
        assertThat(offence.getId(), is(offenceId));
        assertThat(offence.getConvictionDate(), is(verdictDate.toString()));
    }

    @Test
    void shouldNotSetConvictionDateWhenNonMCCChannel() {
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(EITHER_WAY);
        final UUID offenceId = randomUUID();
        final LocalDate pleaDate = LocalDate.now().minusDays(1);
        final String offenceCode = OFFENCE_CODE_TVL_ABC;

        final List<MigratedOffence> offences = of(migratedOffence()
                .withOffenceId(offenceId)
                .withOffenceCode(offenceCode)
                .withOffenceCommittedDate(LocalDate.now())
                .withPlea(migratedPlea()
                        .withId(randomUUID())
                        .withPleaDate(pleaDate)
                        .build())
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName(XHIBIT);
        paramsVO.setReferenceDataVO(referenceDataVO);
        paramsVO.setChannel(SPI);
        paramsVO.setInitiationCode(InitiationCode.O.name());

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);

        final uk.gov.justice.core.courts.Offence offence = coreOffences.get(0);
        assertThat(offence.getId(), is(offenceId));
        assertNull(offence.getConvictionDate());
    }

    @Test
    void shouldNotSetConvictionDateWhenNotAnOtherInitiationType() {
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(EITHER_WAY);
        final UUID offenceId = randomUUID();
        final LocalDate pleaDate = LocalDate.now().minusDays(1);
        final String offenceCode = OFFENCE_CODE_TVL_ABC;

        final List<MigratedOffence> offences = of(migratedOffence()
                .withOffenceId(offenceId)
                .withOffenceCode(offenceCode)
                .withOffenceCommittedDate(LocalDate.now())
                .withPlea(migratedPlea()
                        .withId(randomUUID())
                        .withPleaDate(pleaDate)
                        .build())
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName(XHIBIT);
        paramsVO.setReferenceDataVO(referenceDataVO);
        paramsVO.setChannel(MCC);
        paramsVO.setInitiationCode(InitiationCode.S.name());

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);

        final uk.gov.justice.core.courts.Offence offence = coreOffences.get(0);
        assertThat(offence.getId(), is(offenceId));
        assertNull(offence.getConvictionDate());
    }

    @Test
    void shouldNullifyVerdictAndAD() {

        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(EITHER_WAY);
        final UUID offenceId = randomUUID();
        final LocalDate pleaDate = LocalDate.now().minusDays(1);

        final List<MigratedOffence> offences = of(migratedOffence()
                .withOffenceId(offenceId)
                .withOffenceCode(OFFENCE_CODE_TVL_ABC)
                .withOffenceCommittedDate(LocalDate.now())
                .withPlea(MigratedPlea.migratedPlea()
                        .withId(randomUUID())
                        .withPleaDate(pleaDate)
                        .build())
                .withVerdict(MigratedVerdict.migratedVerdict().build())
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName(XHIBIT);
        paramsVO.setReferenceDataVO(referenceDataVO);
        paramsVO.setChannel(DLRM_MIGRATION);
        paramsVO.setInitiationCode(InitiationCode.S.name());

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);

        final uk.gov.justice.core.courts.Offence offence = coreOffences.get(0);
        assertThat(offence.getId(), is(offenceId));
        assertNull(offence.getVerdict());
    }

    @Test
    void shouldNotNullifyVerdictAndAD() {

        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(EITHER_WAY);
        final UUID offenceId = randomUUID();
        final LocalDate pleaDate = LocalDate.now().minusDays(1);

        final List<MigratedOffence> offences = of(migratedOffence()
                .withOffenceId(offenceId)
                .withOffenceCode(OFFENCE_CODE_TVL_ABC)
                .withOffenceCommittedDate(LocalDate.now())
                .withPlea(migratedPlea()
                        .withId(randomUUID())
                        .withPleaDate(pleaDate)
                        .build())
                .withVerdict(migratedVerdict()
                        .withId(randomUUID())
                        .withVerdictDate(LocalDate.now())
                        .build())
                .build());

        final Map<UUID, Map<UUID, PleaReferenceData>> pleaReferenceDataMap = new HashMap<>();
        pleaReferenceDataMap.put(randomUUID(), Map.of(offenceId, PleaReferenceData.pleaReferenceData().
                withPleaValue("NOT_GUILTY").withPleaTypeCode("NG").build()));


        final Map<UUID, Map<UUID, VerdictReferenceData>> verdictReferenceDataMap = new HashMap<>();
        verdictReferenceDataMap.put(randomUUID(), Map.of(offenceId, VerdictReferenceData.verdictReferenceData()
                .withVerdictCode(NJOJ_VERDICT_CODE)
                .build()));

        referenceDataVO.setPleaReferenceDataMap(pleaReferenceDataMap);
        referenceDataVO.setVerdictReferenceDataMap(verdictReferenceDataMap);

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName(XHIBIT);
        paramsVO.setReferenceDataVO(referenceDataVO);
        paramsVO.setChannel(DLRM_MIGRATION);
        paramsVO.setInitiationCode(InitiationCode.S.name());

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);

        final uk.gov.justice.core.courts.Offence offence = coreOffences.get(0);
        assertThat(offence.getId(), is(offenceId));
        assertNotNull(offence.getVerdict());
    }

    @Test
    void shouldNotNullifyVerdictWhenNoPlea() {

        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(EITHER_WAY);
        final UUID offenceId = randomUUID();

        final List<MigratedOffence> offences = of(migratedOffence()
                .withOffenceId(offenceId)
                .withOffenceCode(OFFENCE_CODE_TVL_ABC)
                .withOffenceCommittedDate(LocalDate.now())
                .withVerdict(migratedVerdict()
                        .withId(randomUUID())
                        .withVerdictDate(LocalDate.now())
                        .build())
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName(XHIBIT);
        paramsVO.setReferenceDataVO(referenceDataVO);
        paramsVO.setChannel(DLRM_MIGRATION);
        paramsVO.setInitiationCode(InitiationCode.S.name());

        final Map<UUID, Map<UUID, VerdictReferenceData>> verdictReferenceDataMap = new HashMap<>();
        verdictReferenceDataMap.put(randomUUID(), Map.of(offenceId, VerdictReferenceData.verdictReferenceData()
                .withVerdictCode(NJOJ_VERDICT_CODE)
                .build()));

        referenceDataVO.setVerdictReferenceDataMap(verdictReferenceDataMap);


        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);

        final uk.gov.justice.core.courts.Offence offence = coreOffences.get(0);
        assertThat(offence.getId(), is(offenceId));
        assertNotNull(offence.getVerdict());
    }

    @Test
    void shouldSetPleaDateToCurrentDateForNotGuiltyPleaIfPleaDateIsMissing() {
        final UUID offenceId = randomUUID();
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithNotGuiltyPlea(EITHER_WAY, randomUUID().toString(), offenceId.toString());

        final List<MigratedOffence> offences = of(migratedOffence()
                .withOffenceId(offenceId)
                .withOffenceCode(OFFENCE_CODE_TVL_ABC)
                .withOffenceCommittedDate(LocalDate.now())
                .withPlea(migratedPlea()
                        .withId(randomUUID())
                        .build())
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName(XHIBIT);
        paramsVO.setReferenceDataVO(referenceDataVO);
        paramsVO.setChannel(MCC);
        paramsVO.setInitiationCode(InitiationCode.S.name());

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);

        final uk.gov.justice.core.courts.Offence offence = coreOffences.get(0);
        assertThat(offence.getId(), is(offenceId));
        assertThat(offence.getPlea().getPleaValue(), is("Not Guilty"));
        assertThat(offence.getPlea().getPleaDate(), is(LocalDate.now().toString()));
    }

    @Test
    void shouldHandleNullOffenceWordingForXhibitSystem() {
        final UUID offenceId = randomUUID();
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(EITHER_WAY);

        final List<MigratedOffence> offences = of(migratedOffence()
                .withOffenceId(offenceId)
                .withOffenceCode(OFFENCE_CODE_TVL_ABC)
                .withOffenceCommittedDate(LocalDate.now())
                .withOffenceWording(null)
                .withReferenceData(offenceReferenceData()
                        .withModeOfTrialDerived(SUMMARY)
                        .build())
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName(XHIBIT);
        paramsVO.setReferenceDataVO(referenceDataVO);

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);

        final uk.gov.justice.core.courts.Offence offence = coreOffences.get(0);
        assertThat(offence.getId(), is(offenceId));
        assertThat(offence.getWording(), is("See indictment or charge sheet for particulars"));
    }

    @Test
    void shouldHandleEmptyOffenceWordingForXhibitSystem() {
        final UUID offenceId = randomUUID();
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(EITHER_WAY);

        final List<MigratedOffence> offences = of(migratedOffence()
                .withOffenceId(offenceId)
                .withOffenceCode(OFFENCE_CODE_TVL_ABC)
                .withOffenceCommittedDate(LocalDate.now())
                .withOffenceWording("")
                .withReferenceData(offenceReferenceData()
                        .withModeOfTrialDerived(SUMMARY)
                        .build())
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName(XHIBIT);
        paramsVO.setReferenceDataVO(referenceDataVO);

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);

        final uk.gov.justice.core.courts.Offence offence = coreOffences.get(0);
        assertThat(offence.getId(), is(offenceId));
        assertThat(offence.getWording(), is("See indictment or charge sheet for particulars"));
    }

    @Test
    void shouldHandleWelshWordingForXhibitSystem() {
        final UUID offenceId = randomUUID();
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(EITHER_WAY);

        final List<MigratedOffence> offences = of(migratedOffence()
                .withOffenceId(offenceId)
                .withOffenceCode(OFFENCE_CODE_TVL_ABC)
                .withOffenceCommittedDate(LocalDate.now())
                .withOffenceWording("Test wording")
                .withOffenceWordingWelsh("Test wording Welsh")
                .withReferenceData(offenceReferenceData()
                        .withModeOfTrialDerived(SUMMARY)
                        .build())
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName(XHIBIT);
        paramsVO.setReferenceDataVO(referenceDataVO);

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);

        final uk.gov.justice.core.courts.Offence offence = coreOffences.get(0);
        assertThat(offence.getId(), is(offenceId));
        assertThat(offence.getWordingWelsh(), is("Test wording Welsh"));
    }

    @Test
    void shouldHandleNullWelshWordingForXhibitSystem() {
        final UUID offenceId = randomUUID();
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(EITHER_WAY);

        final List<MigratedOffence> offences = of(migratedOffence()
                .withOffenceId(offenceId)
                .withOffenceCode(OFFENCE_CODE_TVL_ABC)
                .withOffenceCommittedDate(LocalDate.now())
                .withOffenceWording("Test wording")
                .withOffenceWordingWelsh(null)
                .withReferenceData(offenceReferenceData()
                        .withModeOfTrialDerived(SUMMARY)
                        .build())
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName(XHIBIT);
        paramsVO.setReferenceDataVO(referenceDataVO);

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);

        final uk.gov.justice.core.courts.Offence offence = coreOffences.get(0);
        assertThat(offence.getId(), is(offenceId));
        assertThat(offence.getWordingWelsh(), is("Test wording"));
    }


    @Test
    void shouldHandleNullOffenceFacts() {
        final UUID offenceId = randomUUID();
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(EITHER_WAY);

        final List<MigratedOffence> offences = of(migratedOffence()
                .withOffenceId(offenceId)
                .withOffenceCode(OFFENCE_CODE_TVL_ABC)
                .withOffenceCommittedDate(LocalDate.now())
                .withReferenceData(offenceReferenceData()
                        .withModeOfTrialDerived(SUMMARY)
                        .build())
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName(XHIBIT);
        paramsVO.setReferenceDataVO(referenceDataVO);

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);

        final uk.gov.justice.core.courts.Offence offence = coreOffences.get(0);
        assertThat(offence.getId(), is(offenceId));
        assertNull(offence.getOffenceFacts());
    }

    static Stream<Arguments> pleaReferenceDataProvider() {
        return Stream.of(
                Arguments.of(
                        "G",
                        PleaReferenceData.pleaReferenceData()
                                .withPleaValue(GUILTY)
                                .withPleaTypeCode("G")
                                .withPleaTypeGuiltyFlag("YES")
                                .build()
                ),
                Arguments.of(
                        "NG",
                        PleaReferenceData.pleaReferenceData()
                                .withPleaValue(NOT_GUILTY)
                                .withPleaTypeCode("NG")
                                .withPleaTypeGuiltyFlag("NO")
                                .build()
                )
        );
    }

    static Stream<Arguments> pleaAndVerdictReferenceDataProvider() {
        return Stream.of(
                Arguments.of(
                        "G",
                        "C01BL00",
                        PleaReferenceData.pleaReferenceData()
                                .withPleaValue(GUILTY)
                                .withPleaTypeCode("G")
                                .withPleaTypeGuiltyFlag("YES")
                                .build(),
                        null
                ),
                Arguments.of(
                        "NG",
                        INVALID,
                        PleaReferenceData.pleaReferenceData()
                                .withPleaValue(NOT_GUILTY)
                                .withPleaTypeCode("NG")
                                .withPleaTypeGuiltyFlag("NO")
                                .build(),
                        VerdictReferenceData.verdictReferenceData()
                                .withCategory(GUILTY_CATEGORY)
                                .withCategoryType(GUILTY)
                                .withVerdictCode("RTG")
                                .build()
                ),
                Arguments.of(
                        "NG",
                        "C01BL00",
                        PleaReferenceData.pleaReferenceData()
                                .withPleaValue(NOT_GUILTY)
                                .withPleaTypeCode("NG")
                                .withPleaTypeGuiltyFlag("NO")
                                .build(),
                        null
                ), Arguments.of(
                        "NG",
                        INVALID,
                        PleaReferenceData.pleaReferenceData()
                                .withPleaValue(NOT_GUILTY)
                                .withPleaTypeCode("NG")
                                .withPleaTypeGuiltyFlag("NO")
                                .build(),
                        null
                )
        );
    }
    @ParameterizedTest
    @MethodSource("pleaReferenceDataProvider")
    void shouldHandleInvalidOuCodeForConvictingCourt(String code, PleaReferenceData pleaReferenceData) {
        final UUID offenceId = randomUUID();
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(EITHER_WAY);

        final List<MigratedOffence> offences = of(migratedOffence()
                .withOffenceId(offenceId)
                .withOffenceCode(OFFENCE_CODE_TVL_ABC)
                .withOffenceCommittedDate(LocalDate.now())
                .withConvictingCourtCode(INVALID)
                .withPlea(migratedPlea()
                        .withId(randomUUID())
                        .withPleaDate(LocalDate.now().minusDays(1))
                        .build())
                .build());

        final Map<UUID, Map<UUID, PleaReferenceData>> pleaReferenceDataMap = new HashMap<>();
        pleaReferenceDataMap.put(randomUUID(), Map.of(offenceId, pleaReferenceData));

        referenceDataVO.setPleaReferenceDataMap(pleaReferenceDataMap);

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName(XHIBIT);
        paramsVO.setReferenceDataVO(referenceDataVO);

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);

        final uk.gov.justice.core.courts.Offence offence = coreOffences.get(0);
        assertThat(offence.getId(), is(offenceId));
        assertNull(offence.getConvictingCourt());
    }

    @ParameterizedTest
    @MethodSource("pleaReferenceDataProvider")
    void shouldHandleInvalidOuCodeForConvictingCourtPlea(String code, PleaReferenceData pleaReferenceData) {
        final UUID offenceId = randomUUID();
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(EITHER_WAY);

        final String VERDICT_CODE_RTG = "RTG";
        final List<MigratedOffence> offences = of(migratedOffence()
                .withOffenceId(offenceId)
                .withOffenceCode(OFFENCE_CODE_TVL_ABC)
                .withOffenceCommittedDate(LocalDate.now())
                .withConvictingCourtCode(INVALID)
                .withPlea(migratedPlea()
                        .withId(randomUUID())
                        .withPleaDate(LocalDate.now().minusDays(1))
                        .build())
                .withVerdict(MigratedVerdict.migratedVerdict()
                        .withId(randomUUID())
                        .build())
                .build());

        final Map<UUID, Map<UUID, PleaReferenceData>> pleaReferenceDataMap = new HashMap<>();
        pleaReferenceDataMap.put(randomUUID(), Map.of(offenceId, pleaReferenceData));

        final Map<UUID, Map<UUID, VerdictReferenceData>> verdictReferenceDataMap = new HashMap<>();
        verdictReferenceDataMap.put(randomUUID(), Map.of(offenceId, VerdictReferenceData.verdictReferenceData()
                        .withCategory(GUILTY_CATEGORY)
                        .withCategoryType(GUILTY)
                        .withVerdictCode(VERDICT_CODE_RTG)
                .build()));
        referenceDataVO.setVerdictReferenceDataMap(verdictReferenceDataMap);

        referenceDataVO.setPleaReferenceDataMap(pleaReferenceDataMap);
        OrganisationUnitReferenceData organisationUnitReferenceData = OrganisationUnitReferenceData.organisationUnitReferenceData()
                .withId("89592405-c29b-3706-b1d3-b1dd3a08b227")
                .withOucode(OUCODE_VALID)
                .withOucodeL3Code("BL")
                .withOucodeL3Name("Blackfriars Crown Court")
                .withOucodeL1Code("C")
                .withOucodeL1Name("Crown Courts")
                .withOucodeL2Code("1")
                .withOucodeL2Name("London")
                .withLja("2570")
                .withOucodeEffectiveFromDate("2016-01-01")
                .withAddress1("1-15 Pocock Street")
                .withAddress2("London")
                .withPostcode("SE1 0BJ")
                .withOucodeL3WelshName("Llys Y Goron Blackfriars")
                .withDefaultStartTime("10:00:00")
                .withDefaultDurationHrs("07:00:00")
                .withEmail("London.crowncourt@cps.gov.uk")
                .withCourtLocationCode("0428")
                .withAdditionalProperty("pecsContractorEmail", "PECSBlackfriarsCrown@serco.com")
                .withAdditionalProperty("divisionCode", "1")
                .withAdditionalProperty("courtId", "428")
                .build();

        when(referenceDataQueryService.retrieveOrganisationUnits(OUCODE_VALID)).thenReturn(List.of(organisationUnitReferenceData));
        when(referenceDataQueryService.retrieveOrganisationUnits(INVALID)).thenReturn(null);


        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName(XHIBIT);
        paramsVO.setReferenceDataVO(referenceDataVO);
        paramsVO.addOffenceIdsWithCourtHearingLocation(new OffenceIdsWithCourtHearingLocation(List.of(offenceId), OUCODE_VALID));



        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);

        final uk.gov.justice.core.courts.Offence offence = coreOffences.get(0);
        assertThat(offence.getId(), is(offenceId));
        assertNotNull(offence.getConvictingCourt());
    }

    @ParameterizedTest
    @MethodSource("pleaAndVerdictReferenceDataProvider")
    void shouldHandleAllForConvictingCourt(String pleaCode,String convictingCourt,  PleaReferenceData pleaReferenceData, VerdictReferenceData verdictReferenceData) {
        final UUID offenceId = randomUUID();
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(EITHER_WAY);
        final String offenceCode = OFFENCE_CODE_TVL_ABC;

        final List<MigratedOffence> offences = of(migratedOffence()
                .withOffenceId(offenceId)
                .withOffenceCode(offenceCode)
                .withOffenceCommittedDate(LocalDate.now())
                .withConvictingCourtCode(convictingCourt)
                .withPlea(migratedPlea()
                        .withId(randomUUID())
                        .withPleaDate(LocalDate.now().minusDays(1))
                        .build())
                .withVerdict(nonNull(verdictReferenceData) ? MigratedVerdict.migratedVerdict()
                        .withId(randomUUID())
                        .build() : null)
                .build());

        final Map<UUID, Map<UUID, PleaReferenceData>> pleaReferenceDataMap = new HashMap<>();
        pleaReferenceDataMap.put(randomUUID(), Map.of(offenceId, pleaReferenceData));

        if(nonNull(verdictReferenceData)){
            final Map<UUID, Map<UUID, VerdictReferenceData>> verdictReferenceDataMap = new HashMap<>();
            verdictReferenceDataMap.put(randomUUID(), Map.of(offenceId, verdictReferenceData));
            referenceDataVO.setVerdictReferenceDataMap(verdictReferenceDataMap);
        }



        referenceDataVO.setPleaReferenceDataMap(pleaReferenceDataMap);
        OrganisationUnitReferenceData organisationUnitReferenceData = OrganisationUnitReferenceData.organisationUnitReferenceData()
                .withId("89592405-c29b-3706-b1d3-b1dd3a08b227")
                .withOucode(OUCODE_VALID)
                .withOucodeL3Code("BL")
                .withOucodeL3Name("Blackfriars Crown Court")
                .withOucodeL1Code("C")
                .withOucodeL1Name("Crown Courts")
                .withOucodeL2Code("1")
                .withOucodeL2Name("London")
                .withLja("2570")
                .withOucodeEffectiveFromDate("2016-01-01")
                .withAddress1("1-15 Pocock Street")
                .withAddress2("London")
                .withPostcode("SE1 0BJ")
                .withOucodeL3WelshName("Llys Y Goron Blackfriars")
                .withDefaultStartTime("10:00:00")
                .withDefaultDurationHrs("07:00:00")
                .withEmail("London.crowncourt@cps.gov.uk")
                .withCourtLocationCode("0428")
                .withAdditionalProperty("pecsContractorEmail", "PECSBlackfriarsCrown@serco.com")
                .withAdditionalProperty("divisionCode", "1")
                .withAdditionalProperty("courtId", "428")
                .build();



        if(!OUCODE_VALID.equalsIgnoreCase(convictingCourt)){
            when(referenceDataQueryService.retrieveOrganisationUnits(INVALID)).thenReturn(null);
        }

        if (OUCODE_VALID.equalsIgnoreCase(convictingCourt) ||  (INVALID.equalsIgnoreCase(convictingCourt)  && ("NG".equalsIgnoreCase(pleaCode) && Objects.nonNull(verdictReferenceData)))){
            when(referenceDataQueryService.retrieveOrganisationUnits(OUCODE_VALID)).thenReturn(List.of(organisationUnitReferenceData));
        }


        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName(XHIBIT);
        paramsVO.setReferenceDataVO(referenceDataVO);
        paramsVO.addOffenceIdsWithCourtHearingLocation(new OffenceIdsWithCourtHearingLocation(List.of(offenceId), OUCODE_VALID));


        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);

        final uk.gov.justice.core.courts.Offence offence = coreOffences.get(0);
        assertThat(offence.getId(), is(offenceId));
        if(INVALID.equalsIgnoreCase(convictingCourt)  && ("NG".equalsIgnoreCase(pleaCode) && Objects.isNull(verdictReferenceData))){
            assertNull(offence.getConvictingCourt());
        } else {
            assertNotNull(offence.getConvictingCourt());
        }
    }

    @Test
    void shouldHandleNullVerdictReferenceDataMap() {
        final UUID offenceId = randomUUID();
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(EITHER_WAY);

        final List<MigratedOffence> offences = of(migratedOffence()
                .withOffenceId(offenceId)
                .withOffenceCode(OFFENCE_CODE_TVL_ABC)
                .withOffenceCommittedDate(LocalDate.now())
                .withVerdict(migratedVerdict()
                        .withId(randomUUID())
                        .withVerdictDate(LocalDate.now().minusDays(1))
                        .build())
                .build());

        referenceDataVO.setVerdictReferenceDataMap(null);

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName(XHIBIT);
        paramsVO.setReferenceDataVO(referenceDataVO);

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);

        final uk.gov.justice.core.courts.Offence offence = coreOffences.get(0);
        assertThat(offence.getId(), is(offenceId));
        assertNull(offence.getVerdict());
    }

    @Test
    void shouldHandleNullPleaReferenceDataMap() {
        final UUID offenceId = randomUUID();
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(EITHER_WAY);

        final List<MigratedOffence> offences = of(migratedOffence()
                .withOffenceId(offenceId)
                .withOffenceCode(OFFENCE_CODE_TVL_ABC)
                .withOffenceCommittedDate(LocalDate.now())
                .withPlea(migratedPlea()
                        .withId(randomUUID())
                        .withPleaDate(LocalDate.now().minusDays(1))
                        .build())
                .build());

        referenceDataVO.setPleaReferenceDataMap(null);

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName(XHIBIT);
        paramsVO.setReferenceDataVO(referenceDataVO);

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);

        final uk.gov.justice.core.courts.Offence offence = coreOffences.get(0);
        assertThat(offence.getId(), is(offenceId));
        assertNull(offence.getPlea());
    }

    @Test
    void shouldHandleNullAlcoholRelatedOffence() {
        final UUID offenceId = randomUUID();
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(EITHER_WAY);
        final String offenceCode = OFFENCE_CODE_TVL_ABC;

        final List<MigratedOffence> offences = of(migratedOffence()
                .withOffenceId(offenceId)
                .withOffenceCode(offenceCode)
                .withOffenceCommittedDate(LocalDate.now())
                .withAlcoholRelatedOffence(null)
                .withReferenceData(offenceReferenceData()
                        .withModeOfTrialDerived(SUMMARY)
                        .build())
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName(XHIBIT);
        paramsVO.setReferenceDataVO(referenceDataVO);


        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);

        final uk.gov.justice.core.courts.Offence offence = coreOffences.get(0);
        assertThat(offence.getId(), is(offenceId));
        assertNull(offence.getOffenceFacts());
    }

    @Test
    void shouldHandleNullVehicleRelatedOffence() {
        final UUID offenceId = randomUUID();
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(EITHER_WAY);
        final String offenceCode = OFFENCE_CODE_TVL_ABC;

        final List<MigratedOffence> offences = of(migratedOffence()
                .withOffenceId(offenceId)
                .withOffenceCode(offenceCode)
                .withOffenceCommittedDate(LocalDate.now())
                .withVehicleRelatedOffence(null)
                .withReferenceData(offenceReferenceData()
                        .withModeOfTrialDerived(SUMMARY)
                        .build())
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setMigrationSourceSystemName(XHIBIT);
        paramsVO.setReferenceDataVO(referenceDataVO);

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);

        final uk.gov.justice.core.courts.Offence offence = coreOffences.get(0);
        assertThat(offence.getId(), is(offenceId));
        assertNull(offence.getOffenceFacts());
    }

}