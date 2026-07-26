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

1. **Welcome / Project Setup** – choose an output directory for the exported files.
2. **Define Use Case** – provide a name, description, and fitness-for-use requirements.
3. **Information Elements** – add and categorise the Darwin Core terms your use case relies on (ActedUpon or Consulted).
4. **Select Existing Tests** – browse and search the bundled BDQ test catalog; tick the tests relevant to your use case.
5. **Define New Tests** – author new BDQ test drafts with labels, types, dimensions, and expected response text.
6. **Review & Export** – review a summary of everything and export the output files.

Clicking **Finish** or **Export Now** on the last page writes two files to your chosen output directory:

| File | Contents |
|---|---|
| `usecase_summary.md` | Human-readable Markdown summary of the use case package |
| `project_state.json` | Machine-readable JSON snapshot of the full project state |

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

## Phase 1 scope

The Swing wizard UI shipped in this release covers Phase 1 of the planned roadmap.

### What is included in Phase 1

- Swing wizard shell with card-based page navigation (Back / Next / Finish / Cancel).
- All six wizard pages listed above.
- In-memory domain model: `ProjectState`, `UseCaseDraft`, `InformationElementRef`, `TestDraft`.
- Enumerations: `TestType`, `InfoElementRole`, `ResourceType`.
- `ValidationService` – required-field checks for each page.
- `ExportService` – writes a Markdown summary and a JSON state file.
- `TestCatalogService` + bundled CSV catalog of representative BDQ tests for the selection page.
- Unit tests for model, validation, and export services.

### What is not yet in Phase 1 (planned for later phases)

- Gap analysis matrix (requirement ↔ test coverage).
- Structured expected-response clause builder (if/then/otherwise DSL).
- Source authority / parameter editor.
- Conformance test case (CSV) generator.
- RDF/Turtle export.
- Richer ontology validation.
- Refresh of the test catalog from a remote source.

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
    TestType.java
    InfoElementRole.java
    ResourceType.java
  service/
    ValidationService.java
    ExportService.java
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
      ReviewExportPage.java
src/main/resources/
  catalog/bdqtest_catalog.csv     Bundled BDQ test catalog
```

---

## Running the tests

```bash
mvn test
```
