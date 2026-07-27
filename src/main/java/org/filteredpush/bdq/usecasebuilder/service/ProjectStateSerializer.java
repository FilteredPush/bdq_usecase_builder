package org.filteredpush.bdq.usecasebuilder.service;

import org.filteredpush.bdq.usecasebuilder.model.InformationElementRef;
import org.filteredpush.bdq.usecasebuilder.model.InfoElementRole;
import org.filteredpush.bdq.usecasebuilder.model.ProjectState;
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
import java.util.Properties;

/**
 * Saves and loads a {@link ProjectState} as a simple Java {@link Properties} file.
 *
 * <p>Only the most portable parts of the state are serialised: the output
 * directory, use-case name and description, information elements, additional
 * vocabulary URI, and additional RDF source.  New-test drafts and gap-analysis
 * results are not persisted (they are built interactively and can be lengthy).</p>
 *
 * <p>The file format is UTF-8 {@link Properties} with keys such as:</p>
 * <pre>
 * outputDirectory=/path/to/output
 * usecase.name=BDQ_QualityControl
 * usecase.description=Checks for record quality ...
 * ie.count=2
 * ie.0=dwc:scientificName
 * ie.1=dwc:taxonRank
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
        int count = state.getInformationElements().size();
        props.setProperty(KEY_IE_COUNT, String.valueOf(count));
        for (int i = 0; i < count; i++) {
            InformationElementRef ref = state.getInformationElements().get(i);
            props.setProperty(KEY_IE_PREFIX + i,
                    ref.getQname() != null ? ref.getQname() : "");
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
        String countStr = props.getProperty(KEY_IE_COUNT, "0");
        int count = 0;
        try {
            count = Integer.parseInt(countStr.trim());
        } catch (NumberFormatException e) {
            logger.warn("Invalid ie.count value '{}'; assuming 0", countStr);
        }
        for (int i = 0; i < count; i++) {
            String qname = props.getProperty(KEY_IE_PREFIX + i);
            if (qname != null && !qname.trim().isEmpty()) {
                InformationElementRef ref = new InformationElementRef(
                        qname.trim(), InfoElementRole.ACTED_UPON);
                state.addInformationElement(ref);
            }
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
}
