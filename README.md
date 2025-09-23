# Script Deployer

Script Deployer is a small, configuration-driven utility that discovers scripts in a folder tree, orders them deterministically, and executes them via your database/client command-line tool. It keeps a local history (embedded H2) of what ran, with checksums and versions, so subsequent runs are fast, idempotent, and safe.

Typical use: database DDL/DML deployment driven by files on disk using tools like Oracle sqlplus, PostgreSQL psql, etc. Any target that can be driven by a shell command works.

Project home: https://github.com/techy-planet/script-deployer


## Key features
- Three script classes with strict, predictable ordering: One-time (S_), Repeatable (R_), and Per-run (PRE_/POST_)
- Deterministic sort with optional <seq_num> numbers; prevents duplicate names across directories
- History with checksums and versions stored locally in embedded H2 (no external DB required)
- Shell-command execution with variable substitution for script content and command line
- Fail-fast validation and detailed logging
- Packaged as an executable Spring Boot jar plus a ready-to-run distribution (tar.gz)


## Prerequisites
- Java 8 or higher (tested with 1.8)
- Windows or Linux/macOS shell environment to run your DB client


## Build and packaging (Gradle)
This project uses Gradle and includes the Gradle Wrapper. You don’t need a local Gradle installed; only a JDK is required.

Common commands (Linux/Mac/WSL):
- Build (with tests): ./gradlew clean build
- Build (skip tests): ./gradlew clean build -x test
- Run tests:          ./gradlew test
- Run locally:        ./gradlew bootRun

Common commands (Windows CMD/PowerShell):
- Build (with tests): gradlew clean build
- Build (skip tests): gradlew clean build -x test
- Run tests:          gradlew test
- Run locally:        gradlew bootRun

Outputs (after build):
- Executable jar: build/libs/script-deployer-<version>.jar (e.g., build/libs/script-deployer-2.0.3-RELEASE.jar)
- Distribution archives: build/distributions/script-deployer-<version>.zip and .tar (e.g., build/distributions/script-deployer-2.0.3-RELEASE.zip)
  - Archives contain launch scripts under bin/ and dependencies under lib/ (standard Gradle application layout)


## Quick start (using packaged distribution)
1) Download the latest zip/tar from Releases or build locally.
2) Unpack to a folder, e.g., $SCRIPT_DEPLOYER_HOME
3) Edit $SCRIPT_DEPLOYER_HOME/deployer/application.properties
   - Set app.script.execute.command to a working command that can execute a single script file. Use the <script> token where the file path should go.
     - Example (Postgres): psql "host=${db.host} port=${db.port} dbname=${db.name} user=${db.user}" -f <script>
     - Example (Oracle via sqlplus using wrapper): sqlplus ${db.user}/${db.pass}@${db.tns} @../samples/oracle/executeScript.sql <script>
4) Place your scripts under $SCRIPT_DEPLOYER_HOME/files (you can create subfolders).
5) From $SCRIPT_DEPLOYER_HOME/deployer run:
   - Linux/macOS: ./deployer.sh
   - Windows:     .\\deployer.bat
6) Watch logs under $SCRIPT_DEPLOYER_HOME/logs and the console for progress/errors.


## How it works
Script Deployer recursively scans the configured scripts directory, groups files by pattern, sorts within each group, and executes them in this order:
1) One-time (S_) – must include a unique <seq_num>
2) Repeatable (R_) – may include optional <seq_num>
3) Per-run "pre" (PRE_) – runs every time, before repeatables
4) Per-run "post" (POST_) – runs every time, after repeatables

For each processed file the tool records path, type, sequence, version, checksum, pattern, and timestamps in embedded H2 (db/H2 files under the deployer home). This prevents rework and enables drift detection.


## File naming, patterns, and ordering
Default patterns are configured in deployer/application.properties. Patterns support multiple entries separated by a delimiter (comma by default). <seq_num> is a special token that becomes a numeric capture group for ordering.

Defaults:
- app.scripts.file.pattern.delimiter=,
- app.scripts.pre.run.file.pattern=PRE_<seq_num>_.+\.sql,PRE__.+\.sql
- app.scripts.oneTime.file.pattern=S_<seq_num>_.+\.sql
- app.scripts.repeatable.file.pattern=R_<seq_num>_.+\.sql
- app.scripts.post.run.file.pattern=POST_<seq_num>_.+\.sql,POST__.+\.sql
- app.scripts.file.pattern.conflict=error

Ordering and rules:
- One-time
  - <seq_num> is mandatory and must be unique across all S_ files.
  - Sorted by sequence ascending. Duplicate sequences throw.
  - Changing content after it has executed throws by default. See “One-time changes” below.
- Repeatable
  - <seq_num> is optional. When present, files sort by sequence then filename; when absent, natural filename order is used.
  - On checksum change, the script re-executes and version increments.
- PRE_/POST_
  - <seq_num> is optional; sorting follows the same rules as Repeatable.
  - Always executes each run. Version increases only when content changes.
- Duplicate file names are not allowed across the tree (even in different subfolders). The sorter enforces this to avoid ambiguity.
- Pattern conflict handling: if a file previously ran under one pattern and now matches a different configured pattern, behavior is controlled by app.scripts.file.pattern.conflict:
  - error (default): fail with a clear message
  - any other value: allow

One-time changes (safety switch):
- app.script.sequence.file.modified.error controls behavior when a previously executed S_ file’s content changes:
  - reset-hash: do NOT re-run; only update the stored checksum/version so future runs don’t fail
  - any other value (default): fail fast with an error


## Execution and substitution
- app.script.execute.command is the shell command template executed per file. Use <script> where the file path should be inserted.
  - Logging uses app.script.execute.command.log (defaults to the same value) to avoid leaking secrets in logs if you prefer a masked form.
- Variable substitution inside script files:
  - app.script.template.variables is a comma-separated list of variable names. For each name, values are sourced from the Spring Environment (system properties, environment variables, or application.properties).
  - The listed variables can be referenced as ${varName} within your SQL/script content. At runtime a temporary script is generated with placeholders replaced.
  - If app.scripts.execute.validate.fileSize=true and the result is empty, the run fails to protect against bad substitutions.
- Console output handling:
  - app.script.execute.command.output=false by default. When false, output is suppressed unless the command fails; when true, live output is printed.
  - app.script.execute.stopOnfail=true stops the run immediately when an external command returns a non-zero exit.


## Configuration reference (selected)
- app.scripts.location=../files – root folder for scripts (relative to deployer/ by default)
- app.scripts.deployer.home=. – base folder used for logs and embedded DB; usually the deployer/ directory
- app.scripts.file.pattern.delimiter=,
- app.scripts.pre.run.file.pattern=...
- app.scripts.oneTime.file.pattern=...
- app.scripts.repeatable.file.pattern=...
- app.scripts.post.run.file.pattern=...
- app.scripts.file.pattern.conflict=error – see above
- app.script.execute.command=echo "<script>" – replace with your DB client command
- app.script.execute.command.log=${app.script.execute.command}
- app.script.execute.command.output=false
- app.script.execute.stopOnfail=true
- app.script.execute.reqNumber=NA – free-form deployment request number stored in history
- app.script.template.variables= – comma-separated variable names to substitute
- app.scripts.execute.validate.fileSize=true – reject empty scripts (post-substitution)
- app.script.sequence.file.modified.error=true – set to reset-hash to accept S_ changes without running
- app.exception.trace=false – include stack traces in logs when true
- app.log.skip.script.enabled=true – log “Skipping” messages for already up-to-date files
- app.scripts.db.metadata.spool=true – enable optional DB metadata spooling (see DBSpooler)
- Logging
  - app.logging.pattern, app.log.level, app.log.class.pattern; see src/main/resources/logback-spring.xml
- Persistence (embedded H2)
  - spring.datasource.url=jdbc:h2:file:${app.scripts.deployer.home}/db/H2;AUTO_SERVER=TRUE
  - spring.jpa.hibernate.ddl-auto=update
  - spring.jpa.hibernate.naming.table.prefix= (prefix for the history table if needed)


## Distribution layout
After extracting script-deployer-<version>.(zip|tar) you will see:
- deployer/
  - application.properties (edit me)
  - deployer.sh / deployer.bat (launchers)
  - banner.txt, logback-spring.xml (defaults)
- files/
  - place_files_here.sql (example placeholder)

At runtime, logs/ and db/ folders are created under deployer/ (or wherever app.scripts.deployer.home points).


## Samples
See the samples/ directory for working templates:
- samples/postgres/postgres-sample.properties and postgres-init.sql
- samples/oracle/oracle-sample.properties, executeScript.sql, oracle-init.sql

Tips:
- Ensure your PATH includes psql or sqlplus.
- For Oracle, the wrapper executeScript.sql demonstrates how to read and run the external script path passed in.


## Frequently asked questions (FAQ)
- What happens if I delete a script from the filesystem?
  - Nothing is retracted from history. One-time entries remain recorded; repeatable/per-run scripts simply won’t be found and thus won’t run.
- What if I move a file to another folder or rename it?
  - The tool enforces unique names across the tree. Duplicated names across directories will fail sort/validation. Moving/renaming changes the relative path; if pattern conflict detection is set to error, switching patterns for the same path will also fail.
- What if a one-time file is modified after execution?
  - By default the run fails to prevent accidental changes to historical DDL. Set app.script.sequence.file.modified.error=reset-hash to accept the new checksum without executing.
- How do I inject variables like schema names or credentials?
  - List variable keys in app.script.template.variables, then provide their values via environment variables, JVM -D properties, or application.properties. Use ${var} in scripts.
- Can I run non-SQL files?
  - Yes. As long as your app.script.execute.command can run the file (bash, python, custom CLI), Script Deployer treats it generically.
- Why do I see “Skipping” messages?
  - It means the stored checksum matches and there is nothing to do for that file. Control this via app.log.skip.script.enabled.


## Development and testing
- JDK 8
- Run tests: ./gradlew test (or: gradlew test on Windows)
- Code structure to start exploring:
  - Entry point: ScriptDeployerApplication
  - Settings: component/AppSettings
  - Sorting/checksums: utils/CommonUtils
  - Execution: utils/CommandUtils, component/DBSpooler
  - Persistence: entity/ScriptHistory, repository/ScriptHistoryRepository


## Troubleshooting
- “No pattern defined…” messages: ensure the respective app.scripts.*.file.pattern property is set and not blank.
- External command not found/permission denied: verify PATH and command, and that deployer.sh has execute permission (chmod +x).
- Empty script after substitution: check app.script.template.variables and supplied values; disable validation via app.scripts.execute.validate.fileSize=false if you must.
- Pattern conflict error: either align the file’s class (pattern) or set app.scripts.file.pattern.conflict to a non-error value.


## License
This project is licensed under the MIT License. See LICENSE for details.
