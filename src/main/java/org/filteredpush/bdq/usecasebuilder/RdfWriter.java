package org.filteredpush.bdq.usecasebuilder;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Serializes an Apache Jena {@link Model} to a local file (or a string) in
 * one of the supported RDF formats.
 *
 * <p>Supported format strings (case-insensitive):</p>
 * <ul>
 *   <li>{@code TURTLE} or {@code TTL} – pretty-printed Turtle (default)</li>
 *   <li>{@code RDF/XML} or {@code RDFXML} – pretty-printed RDF/XML</li>
 *   <li>{@code JSON-LD} or {@code JSONLD} – pretty-printed JSON-LD</li>
 *   <li>{@code N-TRIPLES} or {@code NT} – N-Triples</li>
 * </ul>
 *
 * <p>Unknown format strings fall back to pretty-printed Turtle.</p>
 */
public class RdfWriter {

    private static final Logger logger = LoggerFactory.getLogger(RdfWriter.class);

    private static final Map<String, RDFFormat> FORMAT_MAP = new HashMap<>();

    static {
        FORMAT_MAP.put("TURTLE",    RDFFormat.TURTLE_PRETTY);
        FORMAT_MAP.put("TTL",       RDFFormat.TURTLE_PRETTY);
        FORMAT_MAP.put("RDF/XML",   RDFFormat.RDFXML_PRETTY);
        FORMAT_MAP.put("RDFXML",    RDFFormat.RDFXML_PRETTY);
        FORMAT_MAP.put("JSON-LD",   RDFFormat.JSONLD_PRETTY);
        FORMAT_MAP.put("JSONLD",    RDFFormat.JSONLD_PRETTY);
        FORMAT_MAP.put("N-TRIPLES", RDFFormat.NTRIPLES);
        FORMAT_MAP.put("NT",        RDFFormat.NTRIPLES);
    }

    /**
     * Writes {@code model} to the file at {@code outputPath} using the given
     * format.
     *
     * <p>Any missing intermediate directories are created automatically.</p>
     *
     * @param model      the Jena Model to serialize
     * @param outputPath path to the output file
     * @param format     format string (see class javadoc for valid values)
     * @throws IOException if the file cannot be created or written
     */
    public void write(Model model, String outputPath, String format) throws IOException {
        RDFFormat rdfFormat = resolveFormat(format);

        File outputFile = new File(outputPath);
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new IOException("Could not create directory: " + parentDir);
            }
        }

        try (OutputStream out = new FileOutputStream(outputFile)) {
            RDFDataMgr.write(out, model, rdfFormat);
        }
        logger.info("Wrote {} triples to '{}' in {} format",
                model.size(), outputPath, format.toUpperCase());
    }

    /**
     * Serializes {@code model} to a string in the given format.
     *
     * @param model  the Jena Model to serialize
     * @param format format string (see class javadoc for valid values)
     * @return the RDF serialization as a string
     */
    public String writeToString(Model model, String format) {
        RDFFormat rdfFormat = resolveFormat(format);
        StringWriter sw = new StringWriter();
        RDFDataMgr.write(sw, model, rdfFormat);
        return sw.toString();
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private RDFFormat resolveFormat(String format) {
        if (format == null) {
            return RDFFormat.TURTLE_PRETTY;
        }
        RDFFormat resolved = FORMAT_MAP.get(format.toUpperCase());
        if (resolved == null) {
            logger.warn("Unknown RDF format '{}'; defaulting to TURTLE", format);
            resolved = RDFFormat.TURTLE_PRETTY;
        }
        return resolved;
    }
}
