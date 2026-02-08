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

## Sample Data

`samples/*.json` files are simplified examples derived from OCR provider outputs.

- Only text is used; other OCR metadata is ignored.

- Assumes raw OCR text is sufficient for this task.

## Working Directory & Path Resolution

Relative input/output paths are resolved from the project root.

The Gradle run task explicitly sets: workingDir = rootProject.projectDir

## How to Run

### Input
#### Sample: Noisy OCR Input

The following example demonstrates how the parser handles realistic OCR noise,
including timestamps mixed with weights, inconsistent spacing, and numeric commas.

Input (raw OCR text excerpt)
```text
 계 량 증 명 서
계량일자: 2026-02-02 0016
차량번호: 8713
거 래 처: 곰욕환경폐기물

품종명랑 05:26:18 12,480 kg
명:
중 량:
05:36:01 7,470 kg
실 중 량: 5,010 kg

* 위와 같이 계량하였음을 확인함.
동우바이오(주)
2026-02-02 05:37:55
```

#### Why this is challenging

- Time tokens (e.g., 05:26:18, 05:36:01) appear immediately before weight values
and must be removed without discarding the actual numeric weights.

- Thousand separators and spacing (12,480 kg, 7,470 kg) are inconsistently formatted
and require normalization before numeric parsing.

- Labels and values are separated across lines, making simple line-based parsing unreliable.

- This example highlights why the parser prioritizes label-based extraction, normalization, and consistency checks rather than naive pattern matching.

#### JSON (sample) or plain text
Linux / macOS:

```bash
./gradlew :app:run --args="samples/sample_01.json ./out.json"
```
Windows:

``` bat
gradlew.bat :app:run --args="samples/sample_01.json out.json"
```
> The output path (second argument) can be absolute or relative.
> Relative paths are resolved from the project root.

### Output
Example output JSON:

```json
{
  "weighingDate": "2026-02-02 05:37:55",
  "vehicleNumber": "80구8713",
  "grossWeightKg": 12480,
  "tareWeightKg": 7470,
  "netWeightKg": 5010
} 
```

#### Output Schema

The parser always produces a JSON file with the following schema:

| Field | Type | Nullable | Description |
|------|------|----------|-------------|
| weighingDate | String | Yes | Format: `yyyy-MM-dd HH:mm:ss` |
| vehicleNumber | String | Yes | License plate text as extracted |
| grossWeightKg | Integer | Yes | Unit: kilograms (kg) |
| tareWeightKg | Integer | Yes | Unit: kilograms (kg) |
| netWeightKg | Integer | Yes | Unit: kilograms (kg) |

In a valid weighing ticket, weight fields are
*expected* to be present, but may be `null`
when reliable extraction is not possible.

#### Weight Consistency Policy

If grossWeightKg and tareWeightKg are present,
netWeightKg may be derived as gross - tare when missing.

If extracted values violate basic consistency
(e.g. gross ≤ tare),
the affected fields are left as null
rather than force-corrected.

## Error Handling & Exit Codes

| Situation | Behavior | Exit Code |
|----------|----------|-----------|
| Missing arguments | Prints usage message | 1 |
| Input file not found | Prints error to stderr | 1 |
| Parsing failure / malformed OCR | Writes JSON with `null` fields | 0 |

This ensures the tool is safe to batch-run
without unexpected crashes.

## Tests

- Unit tests are designed to validate:

    - standard weighing tickets

    - missing or shuffled labels

    - noisy OCR output (spacing, commas, timestamps)

    - vehicle number and date extraction

- Tests can be executed with:

Run tests: 
```bash
	./gradlew test 
```

## Logging

Basic process-level logs are printed to standard output to indicate
execution flow, parsing progress, and output generation.

### Logging Example
```text
[START] OCR Weighing Ticket Parser
[INPUT]  file = samples/sample_01.json
[OUTPUT] file = ./out.json
[INFO]   OCR text extracted (length=283)
[INFO]   Weights resolved using label-based strategy
Saved: ./out.json
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
 
### Parsing & Resolution Policy

**Priority order**
1. Label-based extraction (총중량 / 차중량 / 실중량)
2. Heuristic numeric extraction (pattern-based kg detection)
3. Arithmetic derivation (`net = gross - tare`, when safe)

**Conflict resolution**
- Explicitly labeled values take precedence over heuristics.
- When multiple numeric candidates exist, the most plausible
  combination satisfying `gross > tare` and
  `net = gross - tare` is selected.
- Ambiguous or contradictory data is not guessed.

**Null tolerance**
- The parsing stage prioritizes maximum data recovery.
- Missing or uncertain fields may remain `null`.
- Semantic validation is deferred to a later stage
  (verification or persistence).

## Limitations
- License plate patterns are simplified and may not cover all regional formats.

- Date parsing currently supports common numeric formats only (e.g. YYYY-MM-DD HH:mm:ss).

- The parser relies on rule-based heuristics and regular expressions;
no machine learning–based entity recognition is used.

- Severe OCR corruption may still result in missing (null) fields.

## Design Rationale

Parsing and validation are intentionally separated.

- The parsing stage focuses on maximum data recovery and allows null values.

- Semantic validation (NOT NULL, consistency checks) is expected to be applied
during a later verification or persistence stage.

This approach prevents silent data corruption while maintaining robustness
against OCR failures.

## Assumptions
- All weight values are expressed in kilograms (kg).

- A valid weighing ticket follows the relationship:

    - grossWeightKg > tareWeightKg

    - netWeightKg = grossWeightKg - tareWeightKg

- OCR text may contain timestamps, line breaks, duplicated tokens, or corrupted spacing.

- Vehicle number and weighing date are typically present near the header area of the ticket.


## Future Improvements
- Introduce a state-based pipeline:

    - PARSED → VERIFIED → PERSISTED

- Enforce NOT NULL and semantic constraints at the persistence layer
(e.g., database CHECK constraints).

- Improve license plate validation using stricter domain rules.

- Support additional date formats and locale-aware parsing.

- Optionally integrate NLP-based entity recognition (e.g., spaCy) as a secondary strategy.
# ocr-parser-java
