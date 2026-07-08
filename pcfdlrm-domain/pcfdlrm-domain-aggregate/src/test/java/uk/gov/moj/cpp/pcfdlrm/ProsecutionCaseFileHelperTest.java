package uk.gov.moj.cpp.pcfdlrm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.BailStatusReferenceData.bailStatusReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Channel.DLRM_MIGRATION;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ObservedEthnicityReferenceData.observedEthnicityReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant.migratedDefendant;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence.migratedOffence;

import uk.gov.moj.cpp.pcfdlrm.domain.DefendantsWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.MigratedDefendantWithOffences;
import uk.gov.moj.cpp.pcfdlrm.domain.MigratedHearingWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;
import uk.gov.moj.cpp.pcfdlrm.refdata.hearing.MigratedHearingRefDataEnricher;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.SelfDefinedInformation;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendant;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedDefendantWithProblem;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedHearing;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedOffence;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.ListedDefendant;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class ProsecutionCaseFileHelperTest {

    @Mock
    private List<MigratedHearingRefDataEnricher> enrichers;

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @Test
    void buildMigratedHearingRefData() {

        CaseDetails caseDetails = CaseDetails.caseDetails().build();

        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        UUID offId11 = UUID.randomUUID();
        final MigratedOffence o11 =  buildMigratedOffence(offId11,"offId11") ;
        UUID offId12 = UUID.randomUUID();
        final MigratedOffence o12 =  buildMigratedOffence(offId12,"offId12") ;
        UUID offId13 = UUID.randomUUID();
        final MigratedOffence o13 =  buildMigratedOffence(offId13,"offId13") ;

        UUID offId21 = UUID.randomUUID();
        final MigratedOffence o21 = buildMigratedOffence(offId21,"offId21") ;
        UUID offId22 = UUID.randomUUID();
        final MigratedOffence o22 =  buildMigratedOffence(offId22,"offId22") ;
        UUID offId23 = UUID.randomUUID();
        final MigratedOffence o23 =  buildMigratedOffence(offId23,"offId23") ;


        UUID offId31 = UUID.randomUUID();
        final MigratedOffence o31 = buildMigratedOffence(offId31,"offId31") ;


        List<MigratedDefendant> migratedDefendants = List.of(
                buildMigratedDefendant(id1, "one-one" ,List.of(o11,o12,o13)),
                buildMigratedDefendant(id2, "two-two" ,List.of(o21,o22,o23)),
                buildMigratedDefendant(id3, "three-three" ,List.of(o31))
        );

        MigratedHearing migratedHearing = MigratedHearing.migratedHearing()
                .withListedDefendants(List.of(ListedDefendant.listedDefendant().withProsecutorDefendantId("one-one").withListedOffences(List.of("offId11","offId12")).build()
                        ,ListedDefendant.listedDefendant().withProsecutorDefendantId("two-two").withListedOffences(List.of("offId21","offId22")).build() ))
                .build();

        MigratedHearingWithReferenceData result = ProsecutionCaseFileHelper
                .buildMigratedHearingRefData(enrichers, caseDetails, migratedHearing, migratedDefendants);

        assertNotNull(result, "Result should not be null");
        final Map<UUID, List<UUID>> defendantWithOffences = result.getMigratedDefendantWithOffences().stream().collect(Collectors.toMap(e -> e.getDefendant().getId(), MigratedDefendantWithOffences::getOffenceids));
        assertEquals(2, defendantWithOffences.size(), "Expected exactly 2 defendants in the result");


        assertAll(
                ()->assertTrue(defendantWithOffences.get(id1).containsAll(List.of(offId11,offId12))),
                ()->assertTrue(defendantWithOffences.get(id2).containsAll(List.of(offId21,offId22)))
        );

    }

    @Test
    void buildMigratedHearingRefDataReturnsEmptyWhenNoListedDefendants() {
        CaseDetails caseDetails = CaseDetails.caseDetails().build();
        UUID id1 = UUID.randomUUID();
        List<MigratedDefendant> migratedDefendants = List.of(
                buildMigratedDefendant(id1, "one-one", List.of(buildMigratedOffence(UUID.randomUUID(), "offId11")))
        );

        MigratedHearing migratedHearing = MigratedHearing.migratedHearing()
                .withListedDefendants(List.of())
                .build();

        MigratedHearingWithReferenceData result = ProsecutionCaseFileHelper
                .buildMigratedHearingRefData(enrichers, caseDetails, migratedHearing, migratedDefendants);

        assertTrue(result.getMigratedDefendantWithOffences().isEmpty());
    }

    @Test
    void buildMigratedHearingRefDataReturnsEmptyWhenListedDefendantIdNotFound() {
        CaseDetails caseDetails = CaseDetails.caseDetails().build();
        UUID id1 = UUID.randomUUID();
        List<MigratedDefendant> migratedDefendants = List.of(
                buildMigratedDefendant(id1, "one-one", List.of(buildMigratedOffence(UUID.randomUUID(), "offId11")))
        );

        MigratedHearing migratedHearing = MigratedHearing.migratedHearing()
                .withListedDefendants(List.of(
                        ListedDefendant.listedDefendant()
                                .withProsecutorDefendantId("unknown-defendant")
                                .withListedOffences(List.of("offId11"))
                                .build()))
                .build();

        MigratedHearingWithReferenceData result = ProsecutionCaseFileHelper
                .buildMigratedHearingRefData(enrichers, caseDetails, migratedHearing, migratedDefendants);

        assertTrue(result.getMigratedDefendantWithOffences().isEmpty());
    }

    @Test
    void buildMigratedHearingRefDataReturnsEmptyWhenListedOffenceDoesNotMatch() {
        CaseDetails caseDetails = CaseDetails.caseDetails().build();
        UUID id1 = UUID.randomUUID();
        List<MigratedDefendant> migratedDefendants = List.of(
                buildMigratedDefendant(id1, "one-one", List.of(
                        buildMigratedOffence(UUID.randomUUID(), "offId11"),
                        buildMigratedOffence(UUID.randomUUID(), "offId12")))
        );

        MigratedHearing migratedHearing = MigratedHearing.migratedHearing()
                .withListedDefendants(List.of(
                        ListedDefendant.listedDefendant()
                                .withProsecutorDefendantId("one-one")
                                .withListedOffences(List.of("offId11", "offId-INVALID"))
                                .build()))
                .build();

        MigratedHearingWithReferenceData result = ProsecutionCaseFileHelper
                .buildMigratedHearingRefData(enrichers, caseDetails, migratedHearing, migratedDefendants);

        assertTrue(result.getMigratedDefendantWithOffences().isEmpty());
    }

    @Test
    void shouldValidateDefendantErrorsWhenCustodyStatusInvalidOnXhibitSetsStatusToUAndPreservesObservedEthnicity() {
        final Integer observedEthnicityCode = 12;

        final MigratedDefendant defendant = migratedDefendant()
                .withDocumentationLanguage("E")
                .withHearingLanguage("E")
                .withIndividual(Individual.individual()
                        .withCustodyStatus("INVALID_STATUS")
                        .withPersonalInformation(PersonalInformation.personalInformation()
                                .withObservedEthnicity(observedEthnicityCode)
                                .build())
                        .withSelfDefinedInformation(SelfDefinedInformation.selfDefinedInformation()
                                .withGender("MALE")
                                .build())
                        .build())
                .build();

        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setObservedEthnicityReferenceData(List.of(
                observedEthnicityReferenceData()
                        .withEthnicityCode(String.valueOf(observedEthnicityCode))
                        .withId(UUID.randomUUID())
                        .withEthnicityDescription("description")
                        .build()
        ));

        final CaseDetails caseDetails = CaseDetails.caseDetails().withCaseId(UUID.randomUUID()).build();
        final DefendantsWithReferenceData defendantsWithReferenceData = new DefendantsWithReferenceData(List.of(defendant));
        defendantsWithReferenceData.setReferenceDataVO(referenceDataVO);
        defendantsWithReferenceData.setCaseDetails(caseDetails);

        when(referenceDataQueryService.retrieveBailStatuses()).thenReturn(List.of(
                bailStatusReferenceData().withStatusCode("U").build()
        ));

        final MigratedDefendantWithProblem result = ProsecutionCaseFileHelper.validateDefendantErrors(
                caseDetails, DLRM_MIGRATION, defendantsWithReferenceData, referenceDataQueryService,
                Stream.builder(), false, "XHIBIT");

        final MigratedDefendant resultDefendant = result.getMigratedDefendants().get(0);
        assertThat(resultDefendant.getIndividual().getCustodyStatus(), is("U"));
        assertThat(resultDefendant.getIndividual().getPersonalInformation().getObservedEthnicity(), is(observedEthnicityCode));
    }

    @Test
    void shouldValidateDefendantErrorsWhenCustodyStatusInvalidOnXhibitAndUStatusFoundAddsUBailStatusToReferenceDataVO() {
        final MigratedDefendant defendant = migratedDefendant()
                .withDocumentationLanguage("E")
                .withHearingLanguage("E")
                .withIndividual(Individual.individual()
                        .withCustodyStatus("INVALID_STATUS")
                        .withPersonalInformation(PersonalInformation.personalInformation().build())
                        .withSelfDefinedInformation(SelfDefinedInformation.selfDefinedInformation()
                                .withGender("MALE")
                                .build())
                        .build())
                .build();

        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        final CaseDetails caseDetails = CaseDetails.caseDetails().withCaseId(UUID.randomUUID()).build();
        final DefendantsWithReferenceData defendantsWithReferenceData = new DefendantsWithReferenceData(List.of(defendant));
        defendantsWithReferenceData.setReferenceDataVO(referenceDataVO);
        defendantsWithReferenceData.setCaseDetails(caseDetails);

        when(referenceDataQueryService.retrieveBailStatuses()).thenReturn(List.of(
                bailStatusReferenceData().withStatusCode("U").build()
        ));

        ProsecutionCaseFileHelper.validateDefendantErrors(
                caseDetails, DLRM_MIGRATION, defendantsWithReferenceData, referenceDataQueryService,
                Stream.builder(), false, "XHIBIT");

        assertThat(referenceDataVO.getBailStatusReferenceData().size(), is(1));
        assertThat(referenceDataVO.getBailStatusReferenceData().get(0).getStatusCode(), is("U"));
    }

    private MigratedOffence buildMigratedOffence(final UUID offenceID, final String prosecutionOffenceId){
        return migratedOffence().withOffenceId(offenceID).withProsecutorOffenceId(prosecutionOffenceId).build();
    }

    private MigratedDefendant buildMigratedDefendant(final UUID defendantId,final  String prosecutiorDefendantId,final  List<MigratedOffence> offences){
        return  migratedDefendant()
                .withProsecutorDefendantId(prosecutiorDefendantId)
                .withId(defendantId)
                .withOffences(offences)
                .build();
    }
}
