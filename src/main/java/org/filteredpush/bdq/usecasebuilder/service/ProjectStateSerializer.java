package org.filteredpush.bdq.usecasebuilder.service;

import org.filteredpush.bdq.usecasebuilder.model.ConformanceRow;
import org.filteredpush.bdq.usecasebuilder.model.InfoElementRole;
import org.filteredpush.bdq.usecasebuilder.model.InformationElementRef;
import org.filteredpush.bdq.usecasebuilder.model.ProjectState;
import org.filteredpush.bdq.usecasebuilder.model.RequirementCoverage;
import org.filteredpush.bdq.usecasebuilder.model.ResourceType;
import org.filteredpush.bdq.usecasebuilder.model.TestDraft;
import org.filteredpush.bdq.usecasebuilder.model.TestType;
import org.filteredpush.bdq.usecasebuilder.model.UseCaseDraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Saves and loads a {@link ProjectState} as a simple Java {@link Properties} file.
 *
 * <p>Persists: output directory, use-case name/description/fitness/scope-note,
 * information elements (with roles), selected existing test IRIs, new test drafts
 * (core fields + conformance rows), requirement coverage rows (gap analysis), additional
 * vocabulary URI, and additional RDF source.</p>
 *
 * <p>The file format is UTF-8 {@link Properties} with keys such as:</p>
 * <pre>
 * outputDirectory=/path/to/output
 * usecase.name=BDQ_QualityControl
 * usecase.description=Checks for record quality ...
 * ie.count=2
 * ie.0.qname=dwc:scientificName
 * ie.0.role=ACTED_UPON
 * ie.1.qname=dwc:taxonRank
 * ie.1.role=CONSULTED
 * sel.count=1
 * sel.0=https://rs.tdwg.org/bdq/terms/some-test-uuid
 * draft.count=1
 * draft.0.label=VALIDATION_SCIENTIFICNAME_NOTEMPTY
 * draft.0.prefLabel=Validation Scientific Name Not Empty
 * draft.0.type=VALIDATION
 * draft.0.resourceType=SINGLE_RECORD
 * draft.0.actedUpon=dwc:scientificName
 * draft.0.consulted=
 * draft.0.dimension=Completeness
 * draft.0.criterion=
 * draft.0.useCaseReference=
 * draft.0.expectedResponse=NOT_EMPTY if dwc:scientificName is bdqval:NotEmpty; otherwise EMPTY
 * draft.0.notes=
 * draft.0.hasSourceAuthority=false
 * draft.0.hasParameters=false
 * draft.0.conf.col.count=4
 * draft.0.conf.col.0=Label
 * draft.0.conf.col.1=Response.status
 * draft.0.conf.col.2=Response.result
 * draft.0.conf.col.3=Response.comment
 * draft.0.conf.row.count=1
 * draft.0.conf.row.0.cell.0=Starter
 * draft.0.conf.row.0.cell.1=RUN_HAS_RESULT
 * draft.0.conf.row.0.cell.2=COMPLIANT
 * draft.0.conf.row.0.cell.3=
 * coverage.count=1
 * coverage.0.requirementId=R1
 * coverage.0.requirementSummary=Records must have a valid scientificName
 * coverage.0.informationElements=dwc:scientificName
 * coverage.0.linkedExistingCount=0
 * coverage.0.linkedNewCount=0
 * coverage.0.rationale=
 * coverage.0.notes=
 * vocab.uri=https://example.org/vocab/terms.rdf
 * rdf.source=/path/to/extra.ttl
 * </pre>
 */
public class ProjectStateSerializer {

    private static final Logger logger = LoggerFactory.getLogger(ProjectStateSerializer.class);

    private static final String KEY_OUTPUT_DIR        = "outputDirectory";
    private static final String KEY_UC_NAME           = "usecase.name";
    private static final String KEY_UC_DESCRIPTION    = "usecase.description";
    private static final String KEY_UC_FITNESS        = "usecase.fitnessRequirementsText";
    private static final String KEY_UC_SCOPE_NOTE     = "usecase.scopeNote";
    private static final String KEY_IE_COUNT          = "ie.count";
    private static final String KEY_IE_PREFIX         = "ie.";
    private static final String KEY_SEL_COUNT         = "sel.count";
    private static final String KEY_SEL_PREFIX        = "sel.";
    private static final String KEY_DRAFT_COUNT       = "draft.count";
    private static final String KEY_DRAFT_PREFIX      = "draft.";
    private static final String KEY_COVERAGE_COUNT    = "coverage.count";
    private static final String KEY_COVERAGE_PREFIX   = "coverage.";
    private static final String KEY_VOCAB_URI         = "vocab.uri";
    private static final String KEY_RDF_SOURCE        = "rdf.source";

    /**
     * Saves the given {@link ProjectState} to a properties file.
     *
     * @param state the state to serialise
     * @param file  the target file (will be created or overwritten)
     * @throws IOException if the file cannot be written
     */
    public void save(ProjectState state, File file) throws IOException {
        Properties props = new Properties();
        if (state.getOutputDirectory() != null) {
            props.setProperty(KEY_OUTPUT_DIR, state.getOutputDirectory());
        }
        UseCaseDraft uc = state.getUseCaseDraft();
        if (uc != null) {
            if (uc.getName() != null) {
                props.setProperty(KEY_UC_NAME, uc.getName());
            }
            if (uc.getDescription() != null) {
                props.setProperty(KEY_UC_DESCRIPTION, uc.getDescription());
            }
            if (uc.getFitnessRequirementsText() != null) {
                props.setProperty(KEY_UC_FITNESS, uc.getFitnessRequirementsText());
            }
            if (uc.getScopeNote() != null) {
                props.setProperty(KEY_UC_SCOPE_NOTE, uc.getScopeNote());
            }
        }
        // Save information elements with roles
        int ieCount = state.getInformationElements().size();
        props.setProperty(KEY_IE_COUNT, String.valueOf(ieCount));
        for (int i = 0; i < ieCount; i++) {
            InformationElementRef ref = state.getInformationElements().get(i);
            props.setProperty(KEY_IE_PREFIX + i + ".qname",
                    ref.getQname() != null ? ref.getQname() : "");
            props.setProperty(KEY_IE_PREFIX + i + ".role",
                    ref.getRole() != null ? ref.getRole().name() : InfoElementRole.ACTED_UPON.name());
        }
        // Save selected existing test IRIs
        List<String> selectedIris = state.getSelectedExistingTestIris();
        props.setProperty(KEY_SEL_COUNT, String.valueOf(selectedIris.size()));
        for (int i = 0; i < selectedIris.size(); i++) {
            props.setProperty(KEY_SEL_PREFIX + i, selectedIris.get(i));
        }
        // Save draft tests
        int draftCount = state.getNewTestDrafts().size();
        props.setProperty(KEY_DRAFT_COUNT, String.valueOf(draftCount));
        for (int i = 0; i < draftCount; i++) {
            TestDraft draft = state.getNewTestDrafts().get(i);
            String pfx = KEY_DRAFT_PREFIX + i + ".";
            props.setProperty(pfx + "label",       nvl(draft.getLabel()));
            props.setProperty(pfx + "prefLabel",   nvl(draft.getPrefLabel()));
            props.setProperty(pfx + "type",        draft.getType() != null ? draft.getType().name() : "");
            props.setProperty(pfx + "resourceType",
                    draft.getResourceType() != null ? draft.getResourceType().name() : "");
            props.setProperty(pfx + "actedUpon",   String.join(",", draft.getActedUponElements()));
            props.setProperty(pfx + "consulted",   String.join(",", draft.getConsultedElements()));
            props.setProperty(pfx + "dimension",   nvl(draft.getDimension()));
            props.setProperty(pfx + "criterion",   nvl(draft.getCriterionOrEnhancement()));
            props.setProperty(pfx + "useCaseReference",  nvl(draft.getUseCaseReference()));
            props.setProperty(pfx + "expectedResponse", nvl(draft.getExpectedResponse()));
            props.setProperty(pfx + "notes",       nvl(draft.getNotes()));
            props.setProperty(pfx + "hasSourceAuthority",
                    String.valueOf(draft.isHasSourceAuthority()));
            props.setProperty(pfx + "hasParameters",
                    String.valueOf(draft.isHasParameters()));
            // Save conformance rows
            List<ConformanceRow> confRows = draft.getConformanceRows();
            if (!confRows.isEmpty()) {
                // Collect column names from first row (all rows share the same columns)
                List<String> cols = new ArrayList<>(confRows.get(0).getValues().keySet());
                props.setProperty(pfx + "conf.col.count", String.valueOf(cols.size()));
                for (int c = 0; c < cols.size(); c++) {
                    props.setProperty(pfx + "conf.col." + c, cols.get(c));
                }
                props.setProperty(pfx + "conf.row.count", String.valueOf(confRows.size()));
                for (int r = 0; r < confRows.size(); r++) {
                    ConformanceRow confRow = confRows.get(r);
                    for (int c = 0; c < cols.size(); c++) {
                        String cellKey = pfx + "conf.row." + r + ".cell." + c;
                        props.setProperty(cellKey, nvl(confRow.getValues().get(cols.get(c))));
                    }
                }
            } else {
                props.setProperty(pfx + "conf.col.count", "0");
                props.setProperty(pfx + "conf.row.count", "0");
            }
        }
        // Save requirement coverage rows (gap analysis)
        List<RequirementCoverage> coverageRows = state.getRequirementCoverageRows();
        props.setProperty(KEY_COVERAGE_COUNT, String.valueOf(coverageRows.size()));
        for (int i = 0; i < coverageRows.size(); i++) {
            RequirementCoverage rc = coverageRows.get(i);
            String pfx = KEY_COVERAGE_PREFIX + i + ".";
            props.setProperty(pfx + "requirementId",      nvl(rc.getRequirementId()));
            props.setProperty(pfx + "requirementSummary", nvl(rc.getRequirementSummary()));
            props.setProperty(pfx + "informationElements",nvl(rc.getInformationElements()));
            props.setProperty(pfx + "rationale",          nvl(rc.getPartialCoverageRationale()));
            props.setProperty(pfx + "notes",              nvl(rc.getNotes()));
            // Linked existing tests (IRIs, tab-separated)
            List<String> linkedExisting = rc.getLinkedExistingTests();
            props.setProperty(pfx + "linkedExistingCount",
                    String.valueOf(linkedExisting.size()));
            for (int j = 0; j < linkedExisting.size(); j++) {
                props.setProperty(pfx + "linkedExisting." + j, linkedExisting.get(j));
            }
            // Linked new tests (labels, tab-separated)
            List<String> linkedNew = rc.getLinkedNewTests();
            props.setProperty(pfx + "linkedNewCount", String.valueOf(linkedNew.size()));
            for (int j = 0; j < linkedNew.size(); j++) {
                props.setProperty(pfx + "linkedNew." + j, linkedNew.get(j));
            }
        }
        if (state.getAdditionalVocabUri() != null) {
            props.setProperty(KEY_VOCAB_URI, state.getAdditionalVocabUri());
        }
        if (state.getAdditionalRdfSource() != null) {
            props.setProperty(KEY_RDF_SOURCE, state.getAdditionalRdfSource());
        }
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            props.store(writer, "BDQ Use Case Builder project file");
        }
        logger.info("Saved project state to: {}", file.getAbsolutePath());
    }

    /**
     * Loads a {@link ProjectState} from a properties file previously written by
     * {@link #save(ProjectState, File)}.
     *
     * @param file the file to read
     * @return a new {@link ProjectState} populated from the file
     * @throws IOException if the file cannot be read
     */
    public ProjectState load(File file) throws IOException {
        Properties props = new Properties();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            props.load(reader);
        }
        ProjectState state = new ProjectState();
        String outputDir = props.getProperty(KEY_OUTPUT_DIR);
        if (outputDir != null) {
            state.setOutputDirectory(outputDir);
        }
        UseCaseDraft uc = new UseCaseDraft();
        String ucName = props.getProperty(KEY_UC_NAME);
        if (ucName != null) {
            uc.setName(ucName);
        }
        String ucDesc = props.getProperty(KEY_UC_DESCRIPTION);
        if (ucDesc != null) {
            uc.setDescription(ucDesc);
        }
        String ucFitness = props.getProperty(KEY_UC_FITNESS);
        if (ucFitness != null && !ucFitness.trim().isEmpty()) {
            uc.setFitnessRequirementsText(ucFitness);
        }
        String ucScopeNote = props.getProperty(KEY_UC_SCOPE_NOTE);
        if (ucScopeNote != null && !ucScopeNote.trim().isEmpty()) {
            uc.setScopeNote(ucScopeNote);
        }
        state.setUseCaseDraft(uc);

        // Load information elements (new format: ie.N.qname / ie.N.role)
        // Also supports legacy format: ie.N (qname only, role defaults to ACTED_UPON)
        String countStr = props.getProperty(KEY_IE_COUNT, "0");
        int count = 0;
        try {
            count = Integer.parseInt(countStr.trim());
        } catch (NumberFormatException e) {
            logger.warn("Invalid ie.count value '{}'; assuming 0", countStr);
        }
        for (int i = 0; i < count; i++) {
            // Try new format first, fall back to legacy
            String qname = props.getProperty(KEY_IE_PREFIX + i + ".qname");
            if (qname == null) {
                qname = props.getProperty(KEY_IE_PREFIX + i); // legacy key
            }
            if (qname != null && !qname.trim().isEmpty()) {
                String roleStr = props.getProperty(KEY_IE_PREFIX + i + ".role");
                InfoElementRole role = InfoElementRole.ACTED_UPON;
                if (roleStr != null) {
                    try {
                        role = InfoElementRole.valueOf(roleStr.trim());
                    } catch (IllegalArgumentException ex) {
                        logger.warn("Unknown IE role '{}'; defaulting to ACTED_UPON", roleStr);
                    }
                }
                state.addInformationElement(new InformationElementRef(qname.trim(), role));
            }
        }

        // Load draft tests
        String draftCountStr = props.getProperty(KEY_DRAFT_COUNT, "0");
        int draftCount = 0;
        try {
            draftCount = Integer.parseInt(draftCountStr.trim());
        } catch (NumberFormatException e) {
            logger.warn("Invalid draft.count value '{}'; assuming 0", draftCountStr);
        }
        for (int i = 0; i < draftCount; i++) {
            String pfx = KEY_DRAFT_PREFIX + i + ".";
            TestDraft draft = new TestDraft();
            draft.setLabel(props.getProperty(pfx + "label", ""));
            draft.setPrefLabel(props.getProperty(pfx + "prefLabel", ""));
            String typeStr = props.getProperty(pfx + "type", "");
            if (!typeStr.isEmpty()) {
                try {
                    draft.setType(TestType.valueOf(typeStr.trim()));
                } catch (IllegalArgumentException ex) {
                    logger.warn("Unknown TestType '{}' for draft {}; skipping type", typeStr, i);
                }
            }
            String rtStr = props.getProperty(pfx + "resourceType", "");
            if (!rtStr.isEmpty()) {
                try {
                    draft.setResourceType(ResourceType.valueOf(rtStr.trim()));
                } catch (IllegalArgumentException ex) {
                    logger.warn("Unknown ResourceType '{}' for draft {}; skipping", rtStr, i);
                }
            }
            String actedUpon = props.getProperty(pfx + "actedUpon", "");
            for (String ie : actedUpon.split(",")) {
                String trimmed = ie.trim();
                if (!trimmed.isEmpty()) {
                    draft.addActedUponElement(trimmed);
                }
            }
            String consulted = props.getProperty(pfx + "consulted", "");
            for (String ie : consulted.split(",")) {
                String trimmed = ie.trim();
                if (!trimmed.isEmpty()) {
                    draft.addConsultedElement(trimmed);
                }
            }
            draft.setDimension(emptyToNull(props.getProperty(pfx + "dimension", "")));
            draft.setCriterionOrEnhancement(emptyToNull(props.getProperty(pfx + "criterion", "")));
            draft.setUseCaseReference(emptyToNull(props.getProperty(pfx + "useCaseReference", "")));
            draft.setExpectedResponse(emptyToNull(props.getProperty(pfx + "expectedResponse", "")));
            draft.setNotes(emptyToNull(props.getProperty(pfx + "notes", "")));
            draft.setHasSourceAuthority(Boolean.parseBoolean(
                    props.getProperty(pfx + "hasSourceAuthority", "false")));
            draft.setHasParameters(Boolean.parseBoolean(
                    props.getProperty(pfx + "hasParameters", "false")));
            // Load conformance rows
            String confColCountStr = props.getProperty(pfx + "conf.col.count", "0");
            int confColCount = 0;
            try {
                confColCount = Integer.parseInt(confColCountStr.trim());
            } catch (NumberFormatException ex) {
                logger.warn("Invalid conf.col.count for draft {}; skipping conformance rows", i);
            }
            if (confColCount > 0) {
                List<String> cols = new ArrayList<>();
                for (int c = 0; c < confColCount; c++) {
                    cols.add(props.getProperty(pfx + "conf.col." + c, "Col" + c));
                }
                String confRowCountStr = props.getProperty(pfx + "conf.row.count", "0");
                int confRowCount = 0;
                try {
                    confRowCount = Integer.parseInt(confRowCountStr.trim());
                } catch (NumberFormatException ex) {
                    logger.warn("Invalid conf.row.count for draft {}; skipping conformance rows", i);
                }
                List<ConformanceRow> confRows = new ArrayList<>();
                for (int r = 0; r < confRowCount; r++) {
                    ConformanceRow confRow = new ConformanceRow();
                    for (int c = 0; c < cols.size(); c++) {
                        String cellKey = pfx + "conf.row." + r + ".cell." + c;
                        confRow.put(cols.get(c), props.getProperty(cellKey, ""));
                    }
                    confRows.add(confRow);
                }
                draft.setConformanceRows(confRows);
            }
            state.addNewTestDraft(draft);
        }

        // Load selected existing test IRIs
        String selCountStr = props.getProperty(KEY_SEL_COUNT, "0");
        int selCount = 0;
        try {
            selCount = Integer.parseInt(selCountStr.trim());
        } catch (NumberFormatException e) {
            logger.warn("Invalid sel.count value '{}'; assuming 0", selCountStr);
        }
        for (int i = 0; i < selCount; i++) {
            String iri = props.getProperty(KEY_SEL_PREFIX + i, "");
            if (!iri.trim().isEmpty()) {
                state.addSelectedExistingTest(iri.trim());
            }
        }

        // Load requirement coverage rows (gap analysis)
        String coverageCountStr = props.getProperty(KEY_COVERAGE_COUNT, "0");
        int coverageCount = 0;
        try {
            coverageCount = Integer.parseInt(coverageCountStr.trim());
        } catch (NumberFormatException e) {
            logger.warn("Invalid coverage.count value '{}'; assuming 0", coverageCountStr);
        }
        List<RequirementCoverage> loadedCoverageRows = new ArrayList<>();
        for (int i = 0; i < coverageCount; i++) {
            String pfx = KEY_COVERAGE_PREFIX + i + ".";
            RequirementCoverage rc = new RequirementCoverage();
            rc.setRequirementId(emptyToNull(props.getProperty(pfx + "requirementId", "")));
            rc.setRequirementSummary(emptyToNull(props.getProperty(pfx + "requirementSummary", "")));
            rc.setInformationElements(emptyToNull(props.getProperty(pfx + "informationElements", "")));
            rc.setPartialCoverageRationale(emptyToNull(props.getProperty(pfx + "rationale", "")));
            rc.setNotes(emptyToNull(props.getProperty(pfx + "notes", "")));
            // Linked existing tests
            String linkedExCountStr = props.getProperty(pfx + "linkedExistingCount", "0");
            int linkedExCount = 0;
            try { linkedExCount = Integer.parseInt(linkedExCountStr.trim()); } catch (NumberFormatException ex) { /* skip */ }
            for (int j = 0; j < linkedExCount; j++) {
                String iri = props.getProperty(pfx + "linkedExisting." + j, "");
                if (!iri.trim().isEmpty()) {
                    rc.getLinkedExistingTests().add(iri.trim());
                }
            }
            // Linked new tests
            String linkedNewCountStr = props.getProperty(pfx + "linkedNewCount", "0");
            int linkedNewCount = 0;
            try { linkedNewCount = Integer.parseInt(linkedNewCountStr.trim()); } catch (NumberFormatException ex) { /* skip */ }
            for (int j = 0; j < linkedNewCount; j++) {
                String label = props.getProperty(pfx + "linkedNew." + j, "");
                if (!label.trim().isEmpty()) {
                    rc.getLinkedNewTests().add(label.trim());
                }
            }
            loadedCoverageRows.add(rc);
        }
        if (!loadedCoverageRows.isEmpty()) {
            state.setRequirementCoverageRows(loadedCoverageRows);
        }

        String vocabUri = props.getProperty(KEY_VOCAB_URI);
        if (vocabUri != null && !vocabUri.trim().isEmpty()) {
            state.setAdditionalVocabUri(vocabUri.trim());
        }
        String rdfSource = props.getProperty(KEY_RDF_SOURCE);
        if (rdfSource != null && !rdfSource.trim().isEmpty()) {
            state.setAdditionalRdfSource(rdfSource.trim());
        }
        logger.info("Loaded project state from: {}", file.getAbsolutePath());
        return state;
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }

    private static String emptyToNull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }
}
