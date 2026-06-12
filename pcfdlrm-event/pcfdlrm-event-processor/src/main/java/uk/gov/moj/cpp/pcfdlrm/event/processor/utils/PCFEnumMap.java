package uk.gov.moj.cpp.pcfdlrm.event.processor.utils;

import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.json.schemas.Language;

import java.util.EnumMap;
import java.util.Map;

@SuppressWarnings({"squid:S1118"})
public final class PCFEnumMap {

    public static Map<Language, HearingLanguage> getLanguageToDocumentationLanguageNeeds() {
        return getHearingPCFToProgressionMap();
    }

    public static Map<Language, HearingLanguage> getLanguageToHearingLanguageNeeds() {
        return getHearingPCFToProgressionMap();
    }

    public static Map<Language, HearingLanguage> getHearingPCFToProgressionMap() {
        final EnumMap<Language, HearingLanguage> hearingPCFToProgressionMap = new EnumMap<>(Language.class);
        hearingPCFToProgressionMap.put(Language.E, HearingLanguage.ENGLISH);
        hearingPCFToProgressionMap.put(Language.W, HearingLanguage.WELSH);
        return hearingPCFToProgressionMap;
    }

}
