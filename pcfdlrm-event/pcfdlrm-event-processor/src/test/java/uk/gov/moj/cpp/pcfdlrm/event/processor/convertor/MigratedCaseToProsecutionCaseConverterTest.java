package uk.gov.moj.cpp.pcfdlrm.event.processor.convertor;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import java.util.Collections;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static uk.gov.moj.cpp.pcfdlrm.event.MigratedCaseFileReceived.migratedCaseFileReceived;
import static uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Channel.DLRM_MIGRATION;
import uk.gov.justice.core.courts.CourtReferral;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.InitiateCourtProceedings;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.moj.cpp.pcfdlrm.domain.MigratedHearingWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.domain.ReferenceDataVO;
import uk.gov.moj.cpp.pcfdlrm.event.processor.utils.CaseReceivedHelper;
import uk.gov.moj.cpp.pcfdlrm.event.MigratedCaseFileReceived;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedCaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedMaterial;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.ReceiveMigratedCaseFile;

import java.util.ArrayList;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MigratedCaseToProsecutionCaseConverterTest {

    @InjectMocks
    MigratedCaseToProsecutionCaseConverter migratedCaseToProsecutionCaseConverter;

    @Mock
    private ProsecutionCaseFileMigratedDefendantToCCDefendantConverter prosecutionCaseFileDefendantToCCDefendantConverter;

    @Mock
    private ProsecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter prosecutionCaseFileInitialHearingToCCHearingRequestConverter;

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    UUID caseID = randomUUID();
    private final String courtId = randomUUID().toString();

    @Test
    void shouldConvertSjpProsecutionToCCCase() {

        final var materials = CaseReceivedHelper.getMaterials(caseID);
        final var migratedCaseDetails = CaseReceivedHelper.getMigratedCaseDetails(caseID);
        final ReceiveMigratedCaseFile receiveMigratedCaseFile = new ReceiveMigratedCaseFile(DLRM_MIGRATION, materials, migratedCaseDetails, randomUUID());
        final ReferenceDataVO referenceDataVO = CaseReceivedHelper.buildReferenceDataWithOffenceAndModeOfTrialWithCourtOrganisation("Either Way", courtId);
        // Create a properly populated MigratedHearingWithReferenceData
        MigratedHearingWithReferenceData hearingWithReferenceData = new MigratedHearingWithReferenceData();
        hearingWithReferenceData.setMigratedDefendantWithOffences(new ArrayList<>()); // Empty list instead of null

        final MigratedCaseFileReceived migratedCaseFileReceived = migratedCaseFileReceived().withMigratedCaseSubmission(receiveMigratedCaseFile).withReferenceDataVO(referenceDataVO)
                .withMigratedHearingWithReferenceData(singletonList(hearingWithReferenceData))
                .build();


        final InitiateCourtProceedings convertedCourtProceedings = migratedCaseToProsecutionCaseConverter.convert(migratedCaseFileReceived);
        final ProsecutionCase convertedProsecutionCase = convertedCourtProceedings.getInitiateCourtProceedings().getProsecutionCases().get(0);
        final CaseDetails caseDetails = receiveMigratedCaseFile.getMigratedCaseDetails().getCaseDetails();

        assertThat(convertedProsecutionCase.getInitiationCode().toString(), equalTo(caseDetails.getInitiationCode()));
        assertThat(convertedProsecutionCase.getSummonsCode(), equalTo(caseDetails.getSummonsCode()));
        assertThat(convertedProsecutionCase.getOriginatingOrganisation(), equalTo(caseDetails.getOriginatingOrganisation()));
        assertThat(convertedProsecutionCase.getCpsOrganisation(), equalTo(caseDetails.getCpsOrganisation()));
        assertThat(convertedProsecutionCase.getMigrationSourceSystem().getMigrationSourceSystemName(), equalTo("LIBRA"));
        assertThat(convertedProsecutionCase.getSendingCourt().getId().toString(), equalTo(courtId));

    }

    @Test
    void shouldConvertWithNoDefendants() {
        final var materials = CaseReceivedHelper.getMaterials(caseID);
        final var migratedCaseDetails = CaseReceivedHelper.getMigratedCaseDetailsWithNoDefendants(caseID);
        final ReceiveMigratedCaseFile receiveMigratedCaseFile = new ReceiveMigratedCaseFile(DLRM_MIGRATION, materials, migratedCaseDetails, randomUUID());
        final ReferenceDataVO referenceDataVO = CaseReceivedHelper.buildReferenceDataWithOffenceAndModeOfTrialWithCourtOrganisation("Either Way", courtId);

        MigratedHearingWithReferenceData hearingWithReferenceData = new MigratedHearingWithReferenceData();
        hearingWithReferenceData.setMigratedDefendantWithOffences(new ArrayList<>());

        final MigratedCaseFileReceived migratedCaseFileReceived = migratedCaseFileReceived()
                .withMigratedCaseSubmission(receiveMigratedCaseFile)
                .withReferenceDataVO(referenceDataVO)
                .withMigratedHearingWithReferenceData(singletonList(hearingWithReferenceData))
                .build();

        when(prosecutionCaseFileDefendantToCCDefendantConverter.convert(any(), any()))
                .thenReturn(new ArrayList<>());

        when(prosecutionCaseFileInitialHearingToCCHearingRequestConverter.convert(any(), any()))
                .thenReturn(new ArrayList<>());

        final InitiateCourtProceedings convertedCourtProceedings = migratedCaseToProsecutionCaseConverter.convert(migratedCaseFileReceived);
        final ProsecutionCase convertedProsecutionCase = convertedCourtProceedings.getInitiateCourtProceedings().getProsecutionCases().get(0);

        assertThat(convertedProsecutionCase.getDefendants().size(), equalTo(0));
        assertThat(convertedProsecutionCase.getStatementOfFacts(), equalTo(""));
        assertThat(convertedProsecutionCase.getStatementOfFactsWelsh(), equalTo(""));
    }

    @Test
    void shouldConvertWithMultipleDefendants() {
        final var materials = CaseReceivedHelper.getMaterials(caseID);
        final var migratedCaseDetails = CaseReceivedHelper.getMigratedCaseDetailsWithMultipleDefendants(caseID);
        final ReceiveMigratedCaseFile receiveMigratedCaseFile = new ReceiveMigratedCaseFile(DLRM_MIGRATION, materials, migratedCaseDetails, randomUUID());
        final ReferenceDataVO referenceDataVO = CaseReceivedHelper.buildReferenceDataWithOffenceAndModeOfTrialWithCourtOrganisation("Either Way", courtId);

        MigratedHearingWithReferenceData hearingWithReferenceData = new MigratedHearingWithReferenceData();
        hearingWithReferenceData.setMigratedDefendantWithOffences(new ArrayList<>());

        final MigratedCaseFileReceived migratedCaseFileReceived = migratedCaseFileReceived()
                .withMigratedCaseSubmission(receiveMigratedCaseFile)
                .withReferenceDataVO(referenceDataVO)
                .withMigratedHearingWithReferenceData(singletonList(hearingWithReferenceData))
                .build();

        when(prosecutionCaseFileDefendantToCCDefendantConverter.convert(any(), any()))
                .thenReturn(asList(
                        Defendant.defendant().withId(randomUUID()).build(),
                        Defendant.defendant().withId(randomUUID()).build()
                ));

        when(prosecutionCaseFileInitialHearingToCCHearingRequestConverter.convert(any(), any()))
                .thenReturn(new ArrayList<>());

        final InitiateCourtProceedings convertedCourtProceedings = migratedCaseToProsecutionCaseConverter.convert(migratedCaseFileReceived);
        final ProsecutionCase convertedProsecutionCase = convertedCourtProceedings.getInitiateCourtProceedings().getProsecutionCases().get(0);

        assertThat(convertedProsecutionCase.getDefendants().size(), equalTo(2));
        assertThat(convertedProsecutionCase.getStatementOfFacts(), equalTo("facts"));
        assertThat(convertedProsecutionCase.getStatementOfFactsWelsh(), equalTo("welsh-facts"));
    }

    @Test
    void shouldConvertWithNoCaseMarkers() {
        final var materials = CaseReceivedHelper.getMaterials(caseID);
        final var migratedCaseDetails = CaseReceivedHelper.getMigratedCaseDetailsWithNoCaseMarkers(caseID);
        final ReceiveMigratedCaseFile receiveMigratedCaseFile = new ReceiveMigratedCaseFile(DLRM_MIGRATION, materials, migratedCaseDetails, randomUUID());
        final ReferenceDataVO referenceDataVO = CaseReceivedHelper.buildReferenceDataWithOffenceAndModeOfTrialWithCourtOrganisation("Either Way", courtId);

        MigratedHearingWithReferenceData hearingWithReferenceData = new MigratedHearingWithReferenceData();
        hearingWithReferenceData.setMigratedDefendantWithOffences(new ArrayList<>());

        final MigratedCaseFileReceived migratedCaseFileReceived = migratedCaseFileReceived()
                .withMigratedCaseSubmission(receiveMigratedCaseFile)
                .withReferenceDataVO(referenceDataVO)
                .withMigratedHearingWithReferenceData(singletonList(hearingWithReferenceData))
                .build();

        when(prosecutionCaseFileDefendantToCCDefendantConverter.convert(any(), any()))
                .thenReturn(singletonList(Defendant.defendant().withId(randomUUID()).build()));
        when(prosecutionCaseFileInitialHearingToCCHearingRequestConverter.convert(any(), any()))
                .thenReturn(new ArrayList<>());

        final InitiateCourtProceedings convertedCourtProceedings = migratedCaseToProsecutionCaseConverter.convert(migratedCaseFileReceived);
        final ProsecutionCase convertedProsecutionCase = convertedCourtProceedings.getInitiateCourtProceedings().getProsecutionCases().get(0);

        assertThat(convertedProsecutionCase.getCaseMarkers(), equalTo(null));
    }

    @Test
    void shouldConvertWithNoHearings() {
        final var materials = CaseReceivedHelper.getMaterials(caseID);
        final var migratedCaseDetails = CaseReceivedHelper.getMigratedCaseDetails(caseID);
        final ReceiveMigratedCaseFile receiveMigratedCaseFile = new ReceiveMigratedCaseFile(DLRM_MIGRATION, materials, migratedCaseDetails, randomUUID());
        final ReferenceDataVO referenceDataVO = CaseReceivedHelper.buildReferenceDataWithOffenceAndModeOfTrialWithCourtOrganisation("Either Way", courtId);

        final MigratedCaseFileReceived migratedCaseFileReceived = migratedCaseFileReceived()
                .withMigratedCaseSubmission(receiveMigratedCaseFile)
                .withReferenceDataVO(referenceDataVO)
                .withMigratedHearingWithReferenceData(new ArrayList<>())
                .build();

        when(prosecutionCaseFileDefendantToCCDefendantConverter.convert(any(), any()))
                .thenReturn(singletonList(Defendant.defendant().withId(randomUUID()).build()));
        when(prosecutionCaseFileInitialHearingToCCHearingRequestConverter.convert(any(), any()))
                .thenReturn(new ArrayList<>());

        final InitiateCourtProceedings convertedCourtProceedings = migratedCaseToProsecutionCaseConverter.convert(migratedCaseFileReceived);
        final CourtReferral courtReferral = convertedCourtProceedings.getInitiateCourtProceedings();

        assertThat(courtReferral.getListHearingRequests(), equalTo(null));
    }

    @Test
    void shouldConvertWithMultipleHearings() {
        final var materials = CaseReceivedHelper.getMaterials(caseID);
        final var migratedCaseDetails = CaseReceivedHelper.getMigratedCaseDetails(caseID);
        final ReceiveMigratedCaseFile receiveMigratedCaseFile = new ReceiveMigratedCaseFile(DLRM_MIGRATION, materials, migratedCaseDetails, randomUUID());
        final ReferenceDataVO referenceDataVO = CaseReceivedHelper.buildReferenceDataWithOffenceAndModeOfTrialWithCourtOrganisation("Either Way", courtId);

        MigratedHearingWithReferenceData hearing1 = new MigratedHearingWithReferenceData();
        hearing1.setMigratedDefendantWithOffences(new ArrayList<>());

        MigratedHearingWithReferenceData hearing2 = new MigratedHearingWithReferenceData();
        hearing2.setMigratedDefendantWithOffences(new ArrayList<>());

        final MigratedCaseFileReceived migratedCaseFileReceived = migratedCaseFileReceived()
                .withMigratedCaseSubmission(receiveMigratedCaseFile)
                .withReferenceDataVO(referenceDataVO)
                .withMigratedHearingWithReferenceData(asList(hearing1, hearing2))
                .build();

        final InitiateCourtProceedings convertedCourtProceedings = migratedCaseToProsecutionCaseConverter.convert(migratedCaseFileReceived);
        final CourtReferral courtReferral = convertedCourtProceedings.getInitiateCourtProceedings();

        assertThat(courtReferral.getProsecutionCases().size(), equalTo(1));
    }

    @Test
    void shouldConvertWithDifferentInitiationCode() {
        final var materials = CaseReceivedHelper.getMaterials(caseID);
        final var migratedCaseDetails = CaseReceivedHelper.getMigratedCaseDetailsWithDifferentInitiationCode(caseID);
        final ReceiveMigratedCaseFile receiveMigratedCaseFile = new ReceiveMigratedCaseFile(DLRM_MIGRATION, materials, migratedCaseDetails, randomUUID());
        final ReferenceDataVO referenceDataVO = CaseReceivedHelper.buildReferenceDataWithOffenceAndModeOfTrialWithCourtOrganisation("Either Way", courtId);

        MigratedHearingWithReferenceData hearingWithReferenceData = new MigratedHearingWithReferenceData();
        hearingWithReferenceData.setMigratedDefendantWithOffences(new ArrayList<>());

        final MigratedCaseFileReceived migratedCaseFileReceived = migratedCaseFileReceived()
                .withMigratedCaseSubmission(receiveMigratedCaseFile)
                .withReferenceDataVO(referenceDataVO)
                .withMigratedHearingWithReferenceData(singletonList(hearingWithReferenceData))
                .build();

        final InitiateCourtProceedings convertedCourtProceedings = migratedCaseToProsecutionCaseConverter.convert(migratedCaseFileReceived);
        final ProsecutionCase convertedProsecutionCase = convertedCourtProceedings.getInitiateCourtProceedings().getProsecutionCases().get(0);

        assertThat(convertedProsecutionCase.getInitiationCode().toString(), equalTo("J"));
    }

    @Test
    void shouldConvertWithEmptyMaterials() {
        final var materials = Collections.<MigratedMaterial>emptyList();
        final var migratedCaseDetails = CaseReceivedHelper.getMigratedCaseDetails(caseID);
        final ReceiveMigratedCaseFile receiveMigratedCaseFile = new ReceiveMigratedCaseFile(DLRM_MIGRATION, materials, migratedCaseDetails, randomUUID());
        final ReferenceDataVO referenceDataVO = CaseReceivedHelper.buildReferenceDataWithOffenceAndModeOfTrialWithCourtOrganisation("Either Way", courtId);

        MigratedHearingWithReferenceData hearingWithReferenceData = new MigratedHearingWithReferenceData();
        hearingWithReferenceData.setMigratedDefendantWithOffences(new ArrayList<>());

        final MigratedCaseFileReceived migratedCaseFileReceived = migratedCaseFileReceived()
                .withMigratedCaseSubmission(receiveMigratedCaseFile)
                .withReferenceDataVO(referenceDataVO)
                .withMigratedHearingWithReferenceData(singletonList(hearingWithReferenceData))
                .build();


        when(prosecutionCaseFileDefendantToCCDefendantConverter.convert(any(), any()))
                .thenReturn(singletonList(
                        Defendant.defendant().withId(randomUUID()).build()
                ));

        when(prosecutionCaseFileInitialHearingToCCHearingRequestConverter.convert(any(), any()))
                .thenReturn(new ArrayList<>());

        final InitiateCourtProceedings convertedCourtProceedings = migratedCaseToProsecutionCaseConverter.convert(migratedCaseFileReceived);
        final ProsecutionCase convertedProsecutionCase = convertedCourtProceedings.getInitiateCourtProceedings().getProsecutionCases().get(0);

        assertThat(convertedProsecutionCase.getId(), equalTo(caseID));
        assertThat(convertedProsecutionCase.getDefendants().size(), equalTo(1));
    }

    @Test
    void shouldConvertWithDifferentMigrationSourceSystem() {
        final var materials = CaseReceivedHelper.getMaterials(caseID);
        final var migratedCaseDetails = CaseReceivedHelper.getMigratedCaseDetailsWithDifferentSourceSystem(caseID);
        final ReceiveMigratedCaseFile receiveMigratedCaseFile = new ReceiveMigratedCaseFile(DLRM_MIGRATION, materials, migratedCaseDetails, randomUUID());
        final ReferenceDataVO referenceDataVO = CaseReceivedHelper.buildReferenceDataWithOffenceAndModeOfTrialWithCourtOrganisation("Either Way", courtId);

        MigratedHearingWithReferenceData hearingWithReferenceData = new MigratedHearingWithReferenceData();
        hearingWithReferenceData.setMigratedDefendantWithOffences(new ArrayList<>());

        final MigratedCaseFileReceived migratedCaseFileReceived = migratedCaseFileReceived()
                .withMigratedCaseSubmission(receiveMigratedCaseFile)
                .withReferenceDataVO(referenceDataVO)
                .withMigratedHearingWithReferenceData(singletonList(hearingWithReferenceData))
                .build();

        final InitiateCourtProceedings convertedCourtProceedings = migratedCaseToProsecutionCaseConverter.convert(migratedCaseFileReceived);
        final ProsecutionCase convertedProsecutionCase = convertedCourtProceedings.getInitiateCourtProceedings().getProsecutionCases().get(0);

        assertThat(convertedProsecutionCase.getMigrationSourceSystem().getMigrationSourceSystemName(), equalTo("XHIBIT"));
    }

    @Test
    void shouldConvertWithNullDates() {
        final var materials = CaseReceivedHelper.getMaterials(caseID);
        final var migratedCaseDetails = CaseReceivedHelper.getMigratedCaseDetailsWithNullDates(caseID);
        final ReceiveMigratedCaseFile receiveMigratedCaseFile = new ReceiveMigratedCaseFile(DLRM_MIGRATION, materials, migratedCaseDetails, randomUUID());
        final ReferenceDataVO referenceDataVO = CaseReceivedHelper.buildReferenceDataWithOffenceAndModeOfTrialWithCourtOrganisation("Either Way", courtId);

        MigratedHearingWithReferenceData hearingWithReferenceData = new MigratedHearingWithReferenceData();
        hearingWithReferenceData.setMigratedDefendantWithOffences(new ArrayList<>());

        final MigratedCaseFileReceived migratedCaseFileReceived = migratedCaseFileReceived()
                .withMigratedCaseSubmission(receiveMigratedCaseFile)
                .withReferenceDataVO(referenceDataVO)
                .withMigratedHearingWithReferenceData(singletonList(hearingWithReferenceData))
                .build();

        final InitiateCourtProceedings convertedCourtProceedings = migratedCaseToProsecutionCaseConverter.convert(migratedCaseFileReceived);
        final ProsecutionCase convertedProsecutionCase = convertedCourtProceedings.getInitiateCourtProceedings().getProsecutionCases().get(0);

        assertThat(convertedProsecutionCase.getCommittalDate(), equalTo(null));
        assertThat(convertedProsecutionCase.getDateOfSendingCase(), equalTo(null));
    }

    @Test
    void shouldConvertWithNullContactInformation() {
        final var materials = CaseReceivedHelper.getMaterials(caseID);
        final var migratedCaseDetails = CaseReceivedHelper.getMigratedCaseDetails(caseID);
        final ReceiveMigratedCaseFile receiveMigratedCaseFile = new ReceiveMigratedCaseFile(DLRM_MIGRATION, materials, migratedCaseDetails, randomUUID());
        final ReferenceDataVO referenceDataVO = CaseReceivedHelper.buildReferenceDataWithNullContactInfo(courtId);

        MigratedHearingWithReferenceData hearingWithReferenceData = new MigratedHearingWithReferenceData();
        hearingWithReferenceData.setMigratedDefendantWithOffences(new ArrayList<>());

        final MigratedCaseFileReceived migratedCaseFileReceived = migratedCaseFileReceived()
                .withMigratedCaseSubmission(receiveMigratedCaseFile)
                .withReferenceDataVO(referenceDataVO)
                .withMigratedHearingWithReferenceData(singletonList(hearingWithReferenceData))
                .build();

        final InitiateCourtProceedings convertedCourtProceedings = migratedCaseToProsecutionCaseConverter.convert(migratedCaseFileReceived);
        final ProsecutionCase convertedProsecutionCase = convertedCourtProceedings.getInitiateCourtProceedings().getProsecutionCases().get(0);

        assertThat(convertedProsecutionCase.getProsecutionCaseIdentifier().getContact(), equalTo(null));
    }

    @Test
    void shouldConvertWithNullOrganizationUnitData() {
        final var materials = CaseReceivedHelper.getMaterials(caseID);
        final var migratedCaseDetails = CaseReceivedHelper.getMigratedCaseDetails(caseID);
        final ReceiveMigratedCaseFile receiveMigratedCaseFile = new ReceiveMigratedCaseFile(DLRM_MIGRATION, materials, migratedCaseDetails, randomUUID());
        final ReferenceDataVO referenceDataVO = CaseReceivedHelper.buildReferenceDataWithNullOrganizationUnit();

        MigratedHearingWithReferenceData hearingWithReferenceData = new MigratedHearingWithReferenceData();
        hearingWithReferenceData.setMigratedDefendantWithOffences(new ArrayList<>());

        final MigratedCaseFileReceived migratedCaseFileReceived = migratedCaseFileReceived()
                .withMigratedCaseSubmission(receiveMigratedCaseFile)
                .withReferenceDataVO(referenceDataVO)
                .withMigratedHearingWithReferenceData(singletonList(hearingWithReferenceData))
                .build();

        final InitiateCourtProceedings convertedCourtProceedings = migratedCaseToProsecutionCaseConverter.convert(migratedCaseFileReceived);
        final ProsecutionCase convertedProsecutionCase = convertedCourtProceedings.getInitiateCourtProceedings().getProsecutionCases().get(0);

        assertThat(convertedProsecutionCase.getReceivingCourt(), equalTo(null));
        assertThat(convertedProsecutionCase.getSendingCourt(), equalTo(null));
    }

    @Test
    void shouldConvertWithNoSendingCourt() {
        final var materials = CaseReceivedHelper.getMaterials(caseID);
        final var migratedCaseDetails = CaseReceivedHelper.getMigratedCaseDetails(caseID);
        MigratedCaseDetails migratedCaseDetailsWithoutSendingCourt= MigratedCaseDetails.migratedCaseDetails()
                .withValuesFrom(migratedCaseDetails)
                .withCaseDetails(CaseDetails.caseDetails()
                        .withValuesFrom(migratedCaseDetails.getCaseDetails())
                        .withSendingCourt(null)
                        .build())
                .build();

        final ReceiveMigratedCaseFile receiveMigratedCaseFile = new ReceiveMigratedCaseFile(DLRM_MIGRATION, materials, migratedCaseDetailsWithoutSendingCourt, randomUUID());
        final ReferenceDataVO referenceDataVO = CaseReceivedHelper.buildReferenceDataWithOffenceAndModeOfTrialWithCourtOrganisation("Either Way", courtId);
        referenceDataVO.setSendingCourtOrganisationUnit(null);

        MigratedHearingWithReferenceData hearingWithReferenceData = new MigratedHearingWithReferenceData();
        hearingWithReferenceData.setMigratedDefendantWithOffences(new ArrayList<>());

        final MigratedCaseFileReceived migratedCaseFileReceived = migratedCaseFileReceived()
                .withMigratedCaseSubmission(receiveMigratedCaseFile)
                .withReferenceDataVO(referenceDataVO)
                .withMigratedHearingWithReferenceData(singletonList(hearingWithReferenceData))
                .build();

        when(prosecutionCaseFileDefendantToCCDefendantConverter.convert(any(), any()))
                .thenReturn(singletonList(Defendant.defendant().withId(randomUUID()).build()));
        when(prosecutionCaseFileInitialHearingToCCHearingRequestConverter.convert(any(), any()))
                .thenReturn(new ArrayList<>());

        final InitiateCourtProceedings convertedCourtProceedings = migratedCaseToProsecutionCaseConverter.convert(migratedCaseFileReceived);
        final ProsecutionCase convertedProsecutionCase = convertedCourtProceedings.getInitiateCourtProceedings().getProsecutionCases().get(0);

        assertThat(convertedProsecutionCase.getSendingCourt(), equalTo(null));
    }
}