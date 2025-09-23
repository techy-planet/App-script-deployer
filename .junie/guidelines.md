Project: Script Deployer – Development Guidelines

Scope
- Audience: Experienced Java/Spring developer contributing to Script Deployer.
- Goal: Capture the project-specific knowledge required to build, configure, test, and extend the tool safely.

1) Build and Packaging
- JDK: Target is Java 8 (pom.xml: <java.version>1.8</java.version>). Use JDK8+.
- Build system: Maven, Spring Boot parent 2.1.4.RELEASE.
  - Common commands:
    - mvn -DskipTests=true clean package
    - mvn test
    - mvn spring-boot:run (for local debugging of the CLI app)
  - Artifacts:
    - Standard Spring Boot executable jar: target/script-deployer.jar (finalName is script-deployer).
    - Assembly packaging configured via maven-assembly-plugin (src/assembly/assembly.xml) builds a distributable with:
      - deployer/ (scripts, properties, launcher sh/bat)
      - files/ placeholder
      - banner/logback/application.properties
    - The assembly goal runs at package phase and produces script-deployer-${project.version}.tar.gz under target.
- Runtime footprint:
  - Uses Spring Boot core and Spring Data JPA with H2; no external DB needed to store deployment history (uses embedded H2 by default).
  - Apache Commons Exec and IO are used for executing external DB client commands and file IO.

2) Configuration Model
- Primary configuration file (runtime): src/main/resources/application.properties (default) and package copies under src/pkg/deployer/application.properties used in assembly.
- Key properties (AppSettings maps these):
  - app.scripts.dir: Root directory holding scripts. In packaged distribution this is ../files relative to deployer/.
  - app.scripts.oneTime.file.pattern (default: S_(\d+)_.+\.sql)
  - app.scripts.repeatable.file.pattern (default: R_(\d+)_.+\.sql)
  - app.scripts.run.always.file.pattern (default: RA_(\d+)_.+\.sql)
  - app.script.execute.command: The shell command to execute per file. Example for Oracle sqlplus or Postgres psql; see samples/*.
  - app.variables.*: Free-form variables that can be referenced in scripts and in execute command via ${var} (ScriptStrSubstitutor).
  - app.fail.on.oneTime.file.modify: If true, fails when a processed one-time file changes later.
- Execution engine:
  - FileProcessorService orders and dispatches files; CommandUtils delegates to Apache Commons Exec to run the configured command with per-file substitution.
  - DBSpooler optionally spools/collects DB output (see component implementation).
- Samples:
  - Postgres: samples/postgres/postgres-sample.properties and samples/postgres/postgres-init.sql (plus a JDBC driver jar example under samples/postgres/). Adjust PATH/command accordingly.
  - Oracle: samples/oracle/* including an executeScript.sql wrapper.

3) Script Classification and Ordering (critical for correctness)
- One-time (S_):
  - Must have a unique required <seq_num>; enforced when sorting. If sameSequenceAllowed=false for this class, duplicate sequences throw.
  - Typical DDL: CREATE TABLE/ALTER TABLE.
- Repeatable (R_):
  - <seq_num> optional and not unique. If absent, natural sort by filename; otherwise sort by <seq_num> then by filename.
  - Typical DDL: CREATE OR REPLACE VIEW/FUNCTION.
- Run-always (RA_):
  - <seq_num> optional; processed every run; sorted like Repeatable.
- Sorting implementation details:
  - CommonUtils.scriptPrioritySorter(regex, seqNumApplicable, sameSequenceAllowed) compares by extracted sequence, then by name; identical name across directories is disallowed and will throw.
  - CommonUtils.getFileSequence returns 0 when no match or when seq not applicable.

4) Variables, Substitution, and Validation
- VariablesValidator checks presence and format of required variables at startup.
- ScriptStrSubstitutor provides ${...} replacement against AppSettings properties for:
  - app.script.execute.command
  - Script content/paths where used by the execution layer.
- Be careful to escape regex metacharacters in patterns when editing defaults or creating project-specific ones.

5) Logging and Observability
- logback-spring.xml controls logging. Default INFO-level logs for progress; DEBUG for deeper troubleshooting.
- A startup banner (banner.txt) is packaged for quick identification.
- Fail-fast exceptions are thrown for inconsistent state: duplicate sequences (when disallowed), duplicate names, checksum generation failures.

6) Persistence
- ScriptHistory entity tracks executed scripts and checksums in embedded H2 by default.
- PrefixPhysicalNamingStrategy defines table naming conventions; ensure any schema/table prefix adjustments are considered if integrating with an external RDBMS for history.

7) Testing Strategy
- Frameworks:
  - JUnit 4 (from spring-boot-starter-test in Spring Boot 2.1.x). @RunWith(SpringRunner.class) + @SpringBootTest exists for context smoke test.
- Unit tests (preferred for utilities):
  - Write plain JUnit 4 tests for utilities like CommonUtils, CommandUtils (mock external processes), and validators. Avoid bringing up Spring context unless necessary.
- Integration tests (optional):
  - Use @SpringBootTest for end-to-end ordering and persistence validation; consider test-specific application.properties and temporary directories.
- Running tests:
  - Maven: mvn -DskipTests=false test
  - IDE: Run individual test classes/methods.
- Adding a new test:
  - Place under src/test/java in the matching package.
  - Example (validated locally during guideline preparation):
    - Purpose: Verify CommonUtils sequence parsing and ordering rules.
    - Snippet:
      package com.techyplanet.scriptdeployer;

      import com.techyplanet.scriptdeployer.utils.CommonUtils;
      import org.junit.Test;
      import java.io.File;
      import java.util.*;
      import static org.junit.Assert.*;

      public class CommonUtilsExampleTest {
          @Test
          public void getFileSequence_parsesSequence() {
              String pattern = "S_([0-9]+)_.+\\.sql";
              assertEquals(Long.valueOf(42L), CommonUtils.getFileSequence(pattern, "S_42_any.sql"));
              assertEquals(Long.valueOf(0L), CommonUtils.getFileSequence(pattern, "random.sql"));
          }

          @Test
          public void sorter_ordersAndCanDisallowDuplicates() {
              String pattern = "S_([0-9]+)_.+\\.sql";
              Comparator<File> disallowDup = CommonUtils.scriptPrioritySorter(pattern, true, false);
              try {
                  Arrays.asList(new File("/tmp/S_1_b.sql"), new File("/tmp/S_1_a.sql")).stream().sorted(disallowDup).toArray();
                  fail("Expected duplicate sequence exception");
              } catch (RuntimeException expected) {}
          }
      }
    - This example was executed successfully during these guidelines’ preparation using the repository’s test runner; remove example tests after use to keep the tree clean.

8) Adding/Running the Deployer Distribution
- After mvn clean package, pick up target/script-deployer-${version}.tar.gz.
- Unpack, then in deployer/application.properties set:
  - app.script.execute.command to your DB client command; see samples/oracle and samples/postgres for working templates.
  - Point app.scripts.dir to the folder with your scripts (or keep default ../files).
- On Linux/Mac: deployer/deployer.sh; on Windows: deployer/deployer.bat.
- Logs will show processing order and any validation failures (e.g., wrong sequences).

9) Code Style and Contribution Notes
- Java 8, prefer streams with caution in performance-sensitive ordering paths.
- Avoid checked exceptions leakage in utilities; this code converts them to RuntimeExceptions with meaningful messages.
- Keep regex patterns centralized in AppSettings; mirror changes in documentation and samples.
- When adding DB-specific features, keep CommandUtils generic and extend via configuration rather than hardcoding DB logic.
- Keep tests deterministic: use temporary folders, fixed filenames, and avoid system-dependent paths.

10) Common Pitfalls
- Forgetting to set app.script.execute.command causes no-op or failure to deploy; always validate with a harmless script first.
- Duplicate file names across directories are prohibited by design (sorting logic will throw) even if sequences differ.
- Changing a processed one-time file will be detected; set app.fail.on.oneTime.file.modify=false only with strong operational guardrails.
- Ensure your shell environment (Windows vs bash) matches the deployer script you invoke.

11) Where to Look in Code
- Entry: ScriptDeployerApplication (Spring Boot main).
- Core sorting/checksum: CommonUtils.
- Settings: component/AppSettings and config/CommonBeans.
- Execution: utils/CommandUtils and component/DBSpooler.
- Persistence: entity/ScriptHistory and repository/ScriptHistoryRepository.

Notes on Verification for These Guidelines
- During preparation, a focused unit test validating CommonUtils behavior was added, executed successfully (2/2 passing), and then removed to keep the repository clean.
- The project compiles with the provided build configuration.
