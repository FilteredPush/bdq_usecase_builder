# bdq_usecase_builder

Application to build an RDF description of a use case with a set of included tests using the bdqffdq vocabulary.

## Overview

`bdq_usecase_builder` is a Java application that helps you create BDQ (Biodiversity Data Quality) use cases and associated tests following the [BDQ Framework](https://github.com/tdwg/bdq). The application offers two modes:

1. **Console wizard** – a classic text-based menu (the original interface).
2. **Swing wizard UI** – a graphical step-by-step wizard (Phase 1 MVP, new in this release).

---

## Running the application

### Prerequisites

- Java 11 or later
- Maven 3.8+ (for building from source)

### Build

```bash
mvn package
```

This produces a shaded (fat) jar at `target/bdq-usecase-builder-<version>-SNAPSHOT.jar`.

### Launch the Swing wizard (recommended)

```bash
java -jar target/bdq-usecase-builder-*-SNAPSHOT.jar --gui
```

The wizard will open a desktop window and guide you through:

1. **Welcome / Project Setup** – default output is `output/` under the launch directory; review or change it.
2. **Define Use Case** – provide a name, description, and fitness-for-use requirements.
3. **Information Elements** – pick and categorise Darwin Core, Audiovisual Core, and custom vocabulary terms (ActedUpon or Consulted).
4. **Select Existing Tests** – tests are filtered by selected information elements by default (with optional show-all); selecting tests can backfill additional information elements.
5. **Define New Tests** – author information-element-driven test drafts, with criterion/enhancement vocab picklists and structured expected-response clause building (ordered IF/THEN + final ELSE).
6. **Authorities & Parameters** – define `hasAuthoritiesDefaults` and parameter defaults with structured editors and validation.
7. **Gap Analysis Matrix** – link requirements/information elements to existing and new tests, track coverage status (`Covered`, `Partially Covered`, `Gap`), and record rationale.
8. **Conformance CSV Data** – generate and edit conformance starter rows from expected-response clauses.
9. **Review & Export** – review a summary of everything and export the output files.

Clicking **Finish** or **Export Now** on the last page writes:

| File | Contents |
|---|---|
| `usecase_summary.md` | Human-readable Markdown summary of the use case package (including gap matrix) |
| `project_state.json` | Machine-readable JSON snapshot of the full project state |
| `conformance_*.csv` | One conformance CSV per drafted new test |
| `conformance_all_tests.csv` | Combined conformance CSV across drafted new tests |

### Launch the console wizard

Run the application without `--gui` to use the original console interface:

```bash
java -jar target/bdq-usecase-builder-*-SNAPSHOT.jar [OPTIONS]
```

Options:

```
-c, --config <file>     Path to configuration file (default: config.properties)
-i, --input <file|IRI>  Additional RDF input file or IRI to load (repeatable)
-o, --output <file>     Output file path (default: usecase-output.ttl)
-f, --format <fmt>      Output RDF format: TURTLE, RDF/XML, JSON-LD, N-TRIPLES
-h, --help              Print help and exit
    --gui               Launch the Swing wizard UI
```

---

## Phase 2 scope

The Swing wizard UI now includes the Phase 1 baseline plus Phase 2 enhancements.

### What is included

- Swing wizard shell with card-based page navigation (Back / Next / Finish / Cancel).
- All nine wizard pages listed above.
- Output directory defaults to `<launch-directory>/output` and can be changed in the welcome page.
- In-memory domain model: `ProjectState`, `UseCaseDraft`, `InformationElementRef`, `TestDraft`.
- Enumerations: `TestType`, `InfoElementRole`, `ResourceType`.
- `ValidationService` – required-field checks for each page.
- `ExportService` – writes a Markdown summary and a JSON state file.
- `TestCatalogService` + bundled CSV catalog of representative BDQ tests for the selection page.
- `VocabularyService` + bundled local controlled vocabularies: `bdqdim`, `bdqcrit`, `bdqenh`, `bdqval`, `bdquc`, `dwc`, `ac`.
- Information-element picker supports Darwin Core, Audiovisual Core, and user-configurable custom vocabularies.
- Expanded contextual guidance text/tooltips on each wizard page (what, why, conventions).
- Existing-test selection is information-element aware, with optional all-tests view and selected-test IE backfill into the information-elements step.
- New-test authoring includes information-element coverage indicators and structured expected-response clause helpers (ordered, editable clauses).
- Dedicated gap-analysis matrix page with requirement-to-test mapping and coverage counts.
- Dedicated authority/parameter editor with validation for URI-only, URI+API, and regex conventions.
- Dedicated conformance CSV editor with generated starter rows plus editable edge-case rows.
- Unit tests for model, validation, and export services.

### Still planned for later phases

- RDF/Turtle export.
- Richer ontology validation.
- Refresh of the test catalog from a remote source.

## Custom vocabulary configuration

You can add local, user-specific vocabulary picklists for the information-element step.

1. Edit (or create) `vocab/custom-vocabularies.properties` in your launch/working directory.
2. Add one property per vocabulary using comma-separated qualified terms:

```properties
myvocab=myvocab:termOne,myvocab:termTwo
institution=inst:collectionCode,inst:recordQuality
```

3. Restart the wizard; these terms are merged into the information-element term picker.

The repository includes a starter file at `vocab/custom-vocabularies.properties`.

---

## Project structure

```
src/main/java/org/filteredpush/bdq/usecasebuilder/
  BdqUsecaseBuilder.java          Main entry point (--gui flag added)
  Configuration.java
  RdfLoader.java
  RdfWriter.java
  UsecaseModel.java
  UsecaseWizard.java              Original console wizard (unchanged)
  vocab/BdqFfdq.java
  model/
    ProjectState.java
    UseCaseDraft.java
    InformationElementRef.java
    TestDraft.java
    ExpectedResponseClause.java
    AuthorityDefault.java
    AuthorityPatternType.java
    ParameterDefinition.java
    RequirementCoverage.java
    ConformanceRow.java
    TestType.java
    InfoElementRole.java
    ResourceType.java
  service/
    ValidationService.java
    ExportService.java
    GapAnalysisService.java
    ExpectedResponseClauseService.java
    ConformanceCsvService.java
    VocabularyService.java
    InformationElementTermService.java
  catalog/
    TestCatalogEntry.java
    TestCatalogService.java
  ui/
    WizardFrame.java
    WizardController.java
    WizardPage.java
    pages/
      WelcomePage.java
      UseCasePage.java
      InformationElementsPage.java
      ExistingTestsPage.java
      NewTestPage.java
      ParameterDefaultsPage.java
      GapAnalysisPage.java
      ConformanceDataPage.java
      ReviewExportPage.java
src/main/resources/
  catalog/bdqtest_catalog.csv     Bundled BDQ test catalog
  catalog/vocabulary/*.csv        Bundled controlled vocabulary picklists
```

---

## Running the tests

```bash
mvn test
```
