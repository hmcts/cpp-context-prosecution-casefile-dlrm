package uk.gov.moj.cpp.pcfdlrm.command.handler;


import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.requireNonNullElse;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.pcfdlrm.aggregate.CaseProcessingArgs;
import uk.gov.moj.cpp.pcfdlrm.aggregate.MigratedCaseFileAggregate;
import uk.gov.moj.cpp.pcfdlrm.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.pcfdlrm.refdata.defendant.DefendantRefDataEnricher;
import uk.gov.moj.cpp.pcfdlrm.refdata.hearing.MigratedHearingRefDataEnricher;
import uk.gov.moj.cpp.pcfdlrm.refdata.proscase.CaseRefDataEnricher;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.DocumentTypeAccessReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.ParentBundleSectionReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.ReceiveMigratedCaseFile;
import uk.gov.moj.cps.pcfdlrm.command.handler.AcceptMigratedCase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.json.JsonValue;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ServiceComponent(COMMAND_HANDLER)
public class MigratedCaseFileHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MigratedCaseFileHandler.class);

    @Inject
    protected EventSource eventSource;

    @Inject
    protected AggregateService aggregateService;

    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Inject
    private Instance<DefendantRefDataEnricher> defendantRefDataEnrichers;

    @Inject
    private Instance<MigratedHearingRefDataEnricher> migratedHearingRefDataEnrichers;

    @Inject
    private Instance<CaseRefDataEnricher> caseRefDataEnrichers;

    @Handles("pcfdlrm.command.receive-migrated-case-file")
    public void receiveMigratedCaseFile(final Envelope<ReceiveMigratedCaseFile> envelope) throws EventStreamException {
        final ReceiveMigratedCaseFile receiveMigratedCaseFile = envelope.payload();
        final UUID caseId = receiveMigratedCaseFile.getMigratedCaseDetails().getCaseDetails().getCaseId();

        ProsecutionWithReferenceData prosecutionWithReferenceData = new ProsecutionWithReferenceData(
                new Prosecution(receiveMigratedCaseFile.getMigratedCaseDetails().getCaseDetails(),
                        requireNonNullElse(receiveMigratedCaseFile.getChannel(), Channel.DLRM_MIGRATION),
                        receiveMigratedCaseFile.getMigratedCaseDetails().getDefendants()));

        prosecutionWithReferenceData.setExternalId(receiveMigratedCaseFile.getSubmissionId());

        final List<DocumentTypeAccessReferenceData> documentTypeAccessReferenceDataList = referenceDataQueryService.retrieveDocumentsTypeAccess();

        final Map<String, ImmutablePair<String, String>> sections = getSections(envelope.metadata(), documentTypeAccessReferenceDataList);

        LOGGER.info("List of Sections :: {}", sections);

        appendEventsToStream(caseId, envelope, migratedCaseFileAggregate ->
                migratedCaseFileAggregate.receiveMigratedCaseFile(new CaseProcessingArgs(receiveMigratedCaseFile,
                        prosecutionWithReferenceData,
                        newArrayList(caseRefDataEnrichers.iterator()),
                        newArrayList(defendantRefDataEnrichers.iterator()),
                        referenceDataQueryService,
                        sections,
                        documentTypeAccessReferenceDataList,
                        newArrayList(migratedHearingRefDataEnrichers.iterator())
                )));
    }

    @Handles("pcfdlrm.command.accept-migrated-case")
    public void handleAcceptMigratedCase(final Envelope<AcceptMigratedCase> envelope) throws EventStreamException {
        final AcceptMigratedCase acceptMigratedCase = envelope.payload();
        final UUID streamId = acceptMigratedCase.getCaseId();
        appendEventsToStream(streamId, envelope, MigratedCaseFileAggregate::acceptMigratedCase);
    }

    private void appendEventsToStream(final UUID streamId,
                                      final Envelope<?> envelope,
                                      final Function<MigratedCaseFileAggregate, Stream<Object>> function) throws EventStreamException {
        EventStream eventStream = eventSource.getStreamById(streamId);
        final MigratedCaseFileAggregate migratedCaseFileAggregate = aggregateService.get(eventStream, MigratedCaseFileAggregate.class);

        final Stream<Object> events = function.apply(migratedCaseFileAggregate);
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);

        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }

    private Map<String, ImmutablePair<String, String>> getSections(final Metadata metadata,
                                                                   final List<DocumentTypeAccessReferenceData> documentTypeAccessReferenceDataList) {

        final Map<String, ImmutablePair<String, String>> pairMap = new HashMap<>();

        final List<ParentBundleSectionReferenceData> parentBundleSectionList = referenceDataQueryService.getAllParentBundleSection(metadata);

        final List<String> cpsBundleCodeList = parentBundleSectionList.stream()
                .map(ParentBundleSectionReferenceData::getCpsBundleCode)
                .toList();

        cpsBundleCodeList.forEach(cpsBundleCode -> {
            if (!pairMap.containsKey(cpsBundleCode)) {
                final ParentBundleSectionReferenceData parentBundleSectionByCpsBundleCode = parentBundleSectionList.stream()
                        .filter(parentBundleSectionReferenceData -> parentBundleSectionReferenceData.getCpsBundleCode().equals(cpsBundleCode))
                        .findFirst().orElse(null);
                if (parentBundleSectionByCpsBundleCode != null && parentBundleSectionByCpsBundleCode.getTargetSectionCode() != null) {
                    final DocumentTypeAccessReferenceData documentTypeAccessReferenceData = findDocumentTypeAccessReferenceData(parentBundleSectionByCpsBundleCode.getTargetSectionCode(), documentTypeAccessReferenceDataList);
                    if (documentTypeAccessReferenceData != null) {
                        pairMap.put(cpsBundleCode, ImmutablePair.of(documentTypeAccessReferenceData.getSectionCode(), documentTypeAccessReferenceData.getSection()));
                    }
                }
            }

        });

        return pairMap;
    }

    private DocumentTypeAccessReferenceData findDocumentTypeAccessReferenceData(final String targetSectionCode, final List<DocumentTypeAccessReferenceData> documentTypeAccessReferenceDataList) {
        return documentTypeAccessReferenceDataList.stream()
                .filter(documentTypeAccessReferenceData -> documentTypeAccessReferenceData.getSectionCode().equals(targetSectionCode))
                .findFirst()
                .orElse(null);
    }

}
