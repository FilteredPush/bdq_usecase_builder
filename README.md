# bdq_usecase_builder

Application to build an RDF description of a use case with a set of included tests using the bdqffdq vocabulary.

## Overview

`bdq_usecase_builder` is a Java application that helps you create BDQ (Biodiversity Data Quality) use cases and associated tests following the [BDQ Framework](https://github.com/tdwg/bdq). The application offers two modes:

1. **Console wizard** – a classic text-based menu (the original interface).
2. **Swing wizard UI** – a graphical guided authoring workbench (Phase 3, latest).

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

The workbench opens a large desktop window (default 1400×900, resizable; size and position are remembered across sessions) and guides you through:

1. **Welcome / Project Setup** – default output is `output/` under the launch directory; review or change it.
2. **Define Use Case** – provide a name, description, and fitness-for-use requirements.
3. **Information Elements** – pick and categorise Darwin Core, Audiovisual Core, and custom vocabulary terms (ActedUpon or Consulted).
4. **Select Existing Tests** – tests are filtered by selected information elements by default (with optional show-all); selecting tests can backfill additional information elements.
5. **Define New Tests** – author information-element-driven test drafts, with criterion/enhancement vocab picklists and structured expected-response clause building (ordered IF/THEN + final ELSE). Each test can have **multiple** ActedUpon and Consulted information elements. Labels and preferred labels are auto-suggested from the test type, IE, and criterion (with override support).
6. **Authorities & Parameters** – define `hasAuthoritiesDefaults` and parameter defaults with structured editors and validation.
7. **Gap Analysis Matrix** – redesigned two-pane model: left shows requirements/IEs, right shows available and linked tests. Explicit **Add Existing Test / Add New Draft Test / Remove Link / Add All / Remove All** buttons. Color-coded rows (green = Covered, amber = Partially Covered, red = Gap). Search/filter for tests. Coverage counts shown at row and overall level.
8. **Conformance CSV Data** – generate and edit conformance starter rows from expected-response clauses.
9. **Review, Validate & Export** – run SHACL-aligned pre-export validation, choose export mode, and export.

### Phase sidebar (cyclical navigation)

The left sidebar lists all nine phases with their current completion status:

| Status | Meaning |
|---|---|
| Not started | The page has not been visited yet |
| In progress | The page has been visited but may be incomplete |
| Ready | Required data is filled in |
| Needs attention | There are gaps or issues |

Click any phase button to jump directly to that phase. Navigating backward or jumping never loses state. Going forward with **Next** validates required fields.

### Export output files

Clicking **Export Now** (Turtle) or **Export (Markdown + JSON)** on the Review page writes to the configured output directory:

| File | Contents |
|---|---|
| `usecase_new.ttl` | RDF/Turtle – new use case + new tests only (Minimal mode) |
| `usecase_with_existing.ttl` | RDF/Turtle – new use case + new tests + selected existing test stubs (Include Existing mode) |
| `validation_report.md` | Human-readable SHACL-aligned validation report |
| `usecase_summary.md` | Markdown summary of the use case package (including gap matrix) |
| `project_state.json` | JSON snapshot of the full project state |
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

## Phase 3 scope

Phase 3 delivers a guided-but-cyclical authoring workbench with improved usability, multi-IE test authoring, SHACL-aware Turtle export, and robust acceptance tests.

### Guided cyclical workflow (B1)

- Nine tutorial-aligned phases with completion status indicators in the sidebar.
- Users can navigate backward/forward freely and jump to any phase without losing state.
- Phase status: **Not started / In progress / Ready / Needs attention**.
- Going backward or jumping saves state from the current page first.

### Window size and preferences (B2)

- Default window size 1400×900 (resizable).
- Window size and position persisted via Java `Preferences` API and restored on next launch.

### Evaluation matrix redesign (B3)

The gap analysis matrix was redesigned to be much more usable:

- **Two-pane model**: left = requirements/IE rows (matrix); right = available and linked tests.
- **Explicit buttons**: Add Existing Test →, Add New Draft Test →, ← Remove Link, Add All →, ← Remove All.
- **Color-coded rows**: green (Covered), amber (Partially Covered), red (Gap).
- **Search/filter**: separate search fields for the requirements table and for available tests.
- **Coverage summary**: shows `X/N covered | K gap(s)` with color indicator.

### Multi-valued information elements (B4)

`TestDraft` now supports:
- `actedUponElements` – list of ActedUpon terms (previously single-valued).
- `consultedElements` – list of Consulted terms.
- `getAllInformationElements()` – combined unique list for convenience.
- Legacy `informationElement` field preserved for backward compatibility.

### Convention-aware label suggestions (B5)

`LabelSuggestionService` auto-suggests:
- `rdfs:label` following `TESTTYPE_INFORMATIONELEMENT_CRITERION` pattern (e.g., `VALIDATION_SCIENTIFICNAME_NOTEMPTY`).
- `skos:prefLabel` following `{TypeName} {localName} {criterion}` pattern.

Suggestions update when upstream fields (type, IE, criterion) change, **unless** the user has manually overridden the field (tracked by `labelUserOverridden` / `prefLabelUserOverridden` flags). Set the override flag to `true` to lock the value; clear it to resume auto-suggestion.

### Expected response builder tokens (B6)

The `ExpectedResponseClause` supports structured clause composition for common BDQ patterns:

| Pattern | Status value |
|---|---|
| External prerequisites not met | `EXTERNAL_PREREQUISITES_NOT_MET` |
| Internal prerequisites not met | `INTERNAL_PREREQUISITES_NOT_MET` |
| Compliant | `RUN_HAS_RESULT` + result `COMPLIANT` |
| Not compliant | `RUN_HAS_RESULT` + result `NOT_COMPLIANT` |
| Amended / Not amended | `AMENDED` / `NOT_AMENDED` |

Use `getAllInformationElements()` on the draft to populate token pickers for clause conditions.

### RDF/Turtle export (B8)

`TurtleExportService` supports two modes:

**Minimal mode** (`usecase_new.ttl`):
- Includes only the newly authored Use Case resource, its Policy, and newly authored tests.
- Policy still asserts `bdqffdq:includedInPolicy` for selected existing tests, but Minimal mode does not serialize existing-test stubs.

**Include Existing mode** (`usecase_with_existing.ttl`):
- Includes everything from Minimal mode, plus stubs for all selected existing tests.
- Existing test stubs include type and label from the bundled catalog when available.

The Turtle output uses stable namespace prefixes (`bdqffdq:`, `bdqtest:`, `dwc:`, etc.) and is serialized by Apache Jena.

Export mapping is constrained to a closed set of bdqffdq predicates/classes to prevent vocabulary drift. Key mappings:

- Use case requirements: `bdqffdq:hasFitnessRequirements` (not `hasFitnessForUsePurpose`).
- Policy membership: `bdqffdq:includedInPolicy` (not `includesInPolicy`).
- Test dimension: `bdqffdq:hasDataQualityDimension` (not `hasDimension`).
- Information-element links:
  - `bdqffdq:hasActedUponInformationElement`
  - `bdqffdq:hasConsultedInformationElement`
  - each role node uses one or more `bdqffdq:composedOf` triples.
- Specification linkage:
  - `Need <- forX - Method - hasSpecification -> Specification`
  - `Need` does not directly use `hasSpecification`.
- Expected response remains on specification via `bdqffdq:hasExpectedResponse`.
- `bdqffdq:hasAuthoritiesDefaults` is omitted when empty.

`bdqffdq:hasFitnessRequirements` export formatting rules:

- Always serialized as a **single-line** string.
- Always contains `<ul><li>…</li></ul>`.
- No newline characters.
- No HTML tags other than `ul` and `li` (disallowed tags are stripped/normalized at export time).

### SHACL-aligned pre-export validation (B9)

`ShaclValidationService` checks the project state against bdqffdq-aligned constraints:

**Blocking errors** (prevent export by default):
- Use case has no name (`rdfs:label`).
- A new test has no type (`bdqffdq:DataQualityNeed` subclass).
- A new test has neither `rdfs:label` nor `skos:prefLabel`.

**Warnings** (informational; do not block export):
- Use case has no description.
- Use case has no fitness-for-use requirements text.
- No information elements defined.
- A test has no information elements, no expected response, no dimension, or no criterion/enhancement.
- No tests defined or selected.

The **Review, Validate & Export** page shows:
- A **Run Validation** button that displays findings in categorized form.
- The **Export Now** button is blocked when blocking errors exist (unless the override checkbox is enabled).
- A **validation_report.md** file is written alongside the Turtle output.

---

## Multi-information-element test authoring

When defining a new test, you can assign **multiple** ActedUpon and Consulted information elements:

```java
TestDraft draft = new TestDraft();
draft.addActedUponElement("dwc:scientificName");
draft.addActedUponElement("dwc:kingdom");
draft.addConsultedElement("dwc:taxonRank");
// Get all IEs (deduplicated)
List<String> all = draft.getAllInformationElements(); // [scientificName, kingdom, taxonRank]
```

The Turtle export maps selected terms into role-specific `bdqffdq:InformationElement` nodes (`ActedUpon` / `Consulted`) and serializes each selected term with `bdqffdq:composedOf`.

---

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

## Phase 2 scope (prior release)

The Swing wizard UI included the Phase 1 baseline plus Phase 2 enhancements:
- Gap-analysis matrix page.
- Expected-response clause builder.
- Authority/parameter editor.
- Conformance CSV generator.
- Vocabulary picklists (bdqcrit, bdqenh, bdqval, bdquc, bdqdim).
- Darwin Core, Audiovisual Core, and user-configurable custom vocabulary pickers.
- Guidance text on every page.

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
    TestDraft.java                Phase 3: multi-valued actedUpon/consulted, label override flags
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
    ValidationService.java        Updated for multi-valued IE check
    ExportService.java
    GapAnalysisService.java
    ExpectedResponseClauseService.java
    ConformanceCsvService.java
    VocabularyService.java
    InformationElementTermService.java
    LabelSuggestionService.java   Phase 3: convention-aware auto-suggestions
    TurtleExportService.java      Phase 3: RDF/Turtle export (Minimal + Include-Existing)
    ShaclValidationService.java   Phase 3: SHACL-aligned pre-export validation
  catalog/
    TestCatalogEntry.java
    TestCatalogService.java
  ui/
    WizardFrame.java              Phase 3: phase sidebar, 1400×900 default, prefs persistence
    WizardController.java         Phase 3: jumpToPage() for cyclical navigation
    WizardPage.java               Phase 3: CompletionStatus enum, markVisited(), getCompletionStatus()
    pages/
      WelcomePage.java
      UseCasePage.java
      InformationElementsPage.java
      ExistingTestsPage.java
      NewTestPage.java
      ParameterDefaultsPage.java
      GapAnalysisPage.java        Phase 3: two-pane redesign, search/filter, bulk ops, color coding
      ConformanceDataPage.java
      ReviewExportPage.java       Phase 3: SHACL validation UI, Turtle export mode toggle
src/main/resources/
  catalog/bdqtest_catalog.csv     Bundled BDQ test catalog
  catalog/vocabulary/*.csv        Bundled controlled vocabulary picklists
```

---

## Running the tests

```bash
mvn test
```
