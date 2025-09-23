Script Deployer 3.0.0 — Release Notes
Date: 2025-09-22

Overview
Script Deployer 3.0.0 is a major release focused on modernizing the build/runtime stack, simplifying packaging, and clarifying script classification and safety rules. It introduces a Gradle-based build, upgrades Spring Boot to 3.5.x, refreshes dependencies, and refines configuration and defaults. A distributable tar.gz bundle is produced with an opinionated layout ready for use.

Highlights
- Build and runtime modernization
  - Migrated project to Gradle with an included Wrapper; simplified common tasks (build, test, run).
  - Upgraded Spring Boot to 3.5.5.
  - Java toolchain configured; builds target a modern JDK via Gradle toolchains.
- Packaging overhaul
  - New Gradle task bundleTar produces script-deployer-<version>-bundle.tar.gz under build/distributions/.
  - Bundle contains:
    - deployer/ (application.properties, launchers, and the jar renamed to script-deployer.jar)
    - files/ placeholder for your scripts
    - README.md and LICENSE at the root of the archive
- Script classification and patterns
  - Clarified and expanded classes: One-time (S_), Repeatable (R_), and per-run PRE_/POST_ scripts.
  - Deterministic ordering retains sequence behavior; duplicate names across the tree are rejected.
- Safety and validation
  - Pattern conflict handling is explicit via app.scripts.file.pattern.conflict (default: error).
  - One-time change behavior controlled by app.script.sequence.file.modified.error (default: fail fast; set to reset-hash to only update checksum).
- Execution and substitution
  - Command template uses <script> placeholder for file path insertion.
  - Optional variable substitution for script contents via app.script.template.variables.
- Dependency and security updates
  - Libraries refreshed; commons-lang3 explicitly pinned to 3.18.0 to address CVE-2025-48924.

Breaking changes
- Build system
  - Maven build and its assembly plugin are replaced by Gradle. Standard commands now:
    - Linux/macOS: ./gradlew clean build, ./gradlew test, ./gradlew bootRun
    - Windows:     gradlew clean build, gradlew test, gradlew bootRun
  - Artifacts move to build/* instead of target/*.
- Spring Boot and Java
  - Spring Boot upgraded to 3.5.x; requires a modern JDK for building/running. Review custom code that depends on deprecated Boot 2.x APIs.
- Script classes and defaults
  - PRE_/POST_ classes are formalized alongside S_ and R_. Ensure your filters/patterns align with the new defaults in deployer/application.properties.
- Configuration keys and defaults
  - Pattern conflict handling via app.scripts.file.pattern.conflict defaults to error.
  - One-time changes controlled by app.script.sequence.file.modified.error. If you previously relied on silent acceptance, set reset-hash explicitly.

New and improved
- Distribution bundle task (bundleTar) that assembles a ready-to-run deployer/ plus files/.
- Clear logging around skipping/up-to-date files and conflict detection.
- Optional DB metadata spooling retained and documented.

How to get the distribution
- After running ./gradlew clean build (or gradlew on Windows), pick up:
  - build/libs/script-deployer-<version>.jar (executable Spring Boot jar)
  - build/distributions/script-deployer-<version>-bundle.tar.gz (recommended ready-to-run bundle)

Migration guide (2.x -> 3.0.0)
1) Build/Run
   - Replace Maven commands with Gradle equivalents. If you automated packaging via the old assembly plugin, use the new bundleTar output.
2) Java/Spring
   - Ensure your runtime JDK aligns with Spring Boot 3.5.x requirements. If embedding this library or extending code, recompile against Boot 3.5.x.
3) Properties
   - Review deployer/application.properties in the bundle for updated defaults.
   - Decide desired behavior for:
     - app.scripts.file.pattern.conflict (error to fail on class changes vs allow)
     - app.script.sequence.file.modified.error (reset-hash to tolerate S_ content change without execution)
   - Verify your script naming fits the new/clarified patterns for PRE_, POST_, S_, and R_.
4) Execution command
   - Update app.script.execute.command to your DB client and ensure it contains the <script> token where the file path should appear.
5) CI/CD artifacts
   - Update paths from target/… to build/… and from script-deployer-<version>.tar.gz to script-deployer-<version>-bundle.tar.gz if you consume the assembled distribution.

Notable dependencies
- Spring Boot 3.5.5
- Apache Commons Exec 1.3
- Apache Commons IO 2.20.0 (with a resolution strategy ensuring a consistent newer line in transitive graphs)
- Apache Commons Text 1.14.0 (resolution strategy also pins to the 3.18.0 line if pulled transitively)
- H2 database (runtime)

Versioning
- Project version: 3.0.0-RELEASE

Acknowledgements
Thanks to everyone who contributed feedback and pull requests leading up to 3.0.0.
