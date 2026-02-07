# OCR Weighing Ticket Parser (Java)

Parses OCR text (or sample JSON containing OCR text) into normalized fields:
- weighingDate
- vehicleNumber
- grossWeightKg
- tareWeightKg
- netWeightKg

## Requirements
- Java 17+
- Gradle Wrapper included

## Tested Environment
- OS: macOS (primary)
- IDE: Eclipse IDE for Java Developers
- Java: 17 (Temurin)
- Build Tool: Gradle (Wrapper)

> The project is IDE-agnostic and can be executed via Gradle Wrapper on macOS or Windows.

## How to Run

### Input: JSON (sample) or plain text
Linux / macOS:

```bash
./gradlew :app:run --args="samples/sample_01.json ./out.json"
```
Windows:

``` bat
gradlew.bat :app:run --args="samples/sample_01.json out.json"
```
- The output path (second argument) can be absolute or relative.

### Output
Example output JSON:

```
json{
  "weighingDate": "2026-02-02 05:37:55",
  "vehicleNumber": "80구8713",
  "grossWeightKg": 12480,
  "tareWeightKg": 7470,
  "netWeightKg": 5010
} 
```
## Implementation Notes
- Extracts weighing date and vehicle number from raw text first (to avoid losing time tokens during normalization).

- Normalizes text:

    - removes time tokens (e.g., 05:36, 05:36:01)
    - normalizes unit variants to kg
    - removes numeric commas (e.g., 14,080 → 14080)
    - collapses repeated whitespace

- Weight extraction strategy:

    - prioritizes label-based parsing (총중량/차중량/실중량) 
    - falls back to heuristic extraction when labels are missing
    - derives missing values using net = gross - tare when possible

- Fault-tolerant:

    - never crashes on malformed input
    - always produces an output JSON

## Assumptions
- All weight values are expressed in kilograms (kg).

- A valid weighing ticket follows the relationship:

    - grossWeightKg > tareWeightKg

    - netWeightKg = grossWeightKg - tareWeightKg

- OCR text may contain timestamps, line breaks, duplicated tokens, or corrupted spacing.

- Vehicle number and weighing date are typically present near the header area of the ticket.

## Limitations
- License plate patterns are simplified and may not cover all regional formats.

- Date parsing currently supports common numeric formats only (e.g. YYYY-MM-DD HH:mm:ss).

- The parser relies on rule-based heuristics and regular expressions;
no machine learning–based entity recognition is used.

- Severe OCR corruption may still result in missing (null) fields.

## Future Improvements
- Introduce a state-based pipeline:

    - PARSED → VERIFIED → PERSISTED

- Enforce NOT NULL and semantic constraints at the persistence layer
(e.g., database CHECK constraints).

- Improve license plate validation using stricter domain rules.

- Support additional date formats and locale-aware parsing.

- Optionally integrate NLP-based entity recognition (e.g., spaCy) as a secondary strategy.

## Tests

- Unit tests are designed to validate:

    - standard weighing tickets

    - missing or shuffled labels

    - noisy OCR output (spacing, commas, timestamps)

    - vehicle number and date extraction

- Tests can be executed with:

Run tests: 

	./gradlew test 

## Logging

Basic process-level logs are printed to standard output to indicate
execution flow, parsing progress, and output generation.

## Design Rationale

Parsing and validation are intentionally separated.

- The parsing stage focuses on maximum data recovery and allows null values.

- Semantic validation (NOT NULL, consistency checks) is expected to be applied
during a later verification or persistence stage.

This approach prevents silent data corruption while maintaining robustness
against OCR failures.
# ocr-parser-java
