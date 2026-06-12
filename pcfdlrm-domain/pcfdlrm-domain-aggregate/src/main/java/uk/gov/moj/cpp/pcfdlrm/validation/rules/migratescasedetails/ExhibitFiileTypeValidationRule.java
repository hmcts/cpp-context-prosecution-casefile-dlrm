package uk.gov.moj.cpp.pcfdlrm.validation.rules.migratescasedetails;


import static java.util.Objects.isNull;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.INVALID_FILE_TYPE_FOR_XHIBIT;
import static uk.gov.moj.cpp.pcfdlrm.validation.ProblemCode.INVALID_FILE_TYPE_FOR_XHIBIT_MIGRATION;
import static uk.gov.moj.cpp.pcfdlrm.validation.Problems.newProblem;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.pcfdlrm.domain.MigratedMaterialsWithOriginatingSystem;
import uk.gov.moj.cpp.pcfdlrm.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.FieldName;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationResult;
import uk.gov.moj.cpp.pcfdlrm.validation.rules.ValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.dlrm.migrated.json.schemas.MigratedMaterial;

import java.util.List;

public class ExhibitFiileTypeValidationRule implements ValidationRule<MigratedMaterialsWithOriginatingSystem, ReferenceDataQueryService> {

    private static final String XHIBIT = "XHIBIT";
    private static final String PDF_EXTENSION = ".pdf";
    private static final String EXHIBIT_FILE_TYPE = "99";

    @Override
    public ValidationResult validate(MigratedMaterialsWithOriginatingSystem input, ReferenceDataQueryService context) {
        
        if (!isXhibitSystem(input)) {
            return VALID;
        }

        List<MigratedMaterial> materials = input.getMaterials();
        
        if (isNull(materials) || materials.isEmpty()) {
            return VALID;
        }

        for (MigratedMaterial material : materials) {
            String fileName = material.getFileName();
            if (isNull(fileName) || !fileName.toLowerCase().endsWith(PDF_EXTENSION)) {
                return newValidationResult(of(newProblem(INVALID_FILE_TYPE_FOR_XHIBIT,
                        FieldName.FILE_TYPE.getValue(), ofNullable(fileName).orElse(""))));
            }

            String fileType = material.getFileType();
            if (isNull(fileType) || !EXHIBIT_FILE_TYPE.equals(fileType)) {
                return newValidationResult(of(newProblem(INVALID_FILE_TYPE_FOR_XHIBIT_MIGRATION,
                        FieldName.FILE_TYPE.getValue(), ofNullable(fileType).orElse(""))));
            }
        }

        return VALID;
    }

    private boolean isXhibitSystem(MigratedMaterialsWithOriginatingSystem input) {
        return input.getMigrationSourceSystemName() != null 
            && XHIBIT.equals(input.getMigrationSourceSystemName());
    }
}
