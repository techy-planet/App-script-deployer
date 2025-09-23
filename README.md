# Script Deployer #

Script Deployer makes it easy to deploy script/file changes found recursively in a given directory. This tool can be used in multiple ways for various use cases, but most commonly this can be used for DB scripts management. It can support all DBs in the world which have a command prompt utility for making changes to DB, for example "sqlplus" for Oracle DB, "psql" for postgres, etc.

Let's understand further how it works and how it can best suit an individual/organization need.

## Prerequisite ##

  - Java 8 or higher
  - Windows OS Or Linux OS with bash shell execution support

## How it works? ##

### Files can be processed in 3 ways. ###

  - #### one-time file ####

  File will be processed exactly once. If file is modified after execution then tool will throw an error/exception in later runs, as logically if one-time file is already processed then no changes should be done in the file. However, one configuration change could be done to avoid it if required. (refer to FAQ Q.3)
    
      Property : app.scripts.oneTime.file.pattern
      Value    : can be defined in regex format. Default is "S_<seq_num>_.+\.sql"
      Example  : S_1_anyName.sql

  From SQL scripts perspective, this is best suitable for "CREATE TABLE" or "ALTER TABLE" type of commands.

  - #### Repeatable file ####
  
  Tool will deploy the file for first time and will re-deploy if any change is detected in future runs.
      
      Property : app.scripts.repeatable.file.pattern
      Value    : can be defined in regex format. Default is "R_<seq_num>_.+\.sql"
      Example  : R_1_anyName.sql
  
  From SQL scripts perspective, this can be used for "CREATE OR REPLACE VIEW" or "CREATE OR REPLACE FUNCTIONS" type of commands where definition need to be redeployed when DDL is changed.

  - #### Maintenance files ####

  Files will be processed every time for each run. This can be used for deploying post run maintenance scripts.
      
      Property : app.scripts.run.always.file.pattern
      Value    : can be defined in regex format. Default is "RA_<seq_num>_.+\.sql"
      Example  : RA_1_anyName.sql

  From SQL scripts perspective, this can be used for running "GRANTS" or reset columns of a table on need basis.


### File processing order ###

  Files will be processed in below given order.
  
  1. All one-time files
      - <seq_num> is mandatory and has to be unique for each file, files will be ordered based on unique sequence number. 
  2. Repeatable files
      - <seq_num> is optional and need not to be unique
        if provided then ordering will happen on <seq_num> and in case of <seq_num> conflict natural sorting of file name will decide the order.
      - if no <seq_num> is provided then files will be sorted in natural order of the file name for execution
  3. Maintenance files
      - <seq_num> is optional and need not to be unique
        if provided then ordering will happen on <seq_num> and in case of <seq_num> conflict natural sorting of file name will decide the order.
      - if no <seq_num> is provided then files will be sorted in natural order of the file name for execution
  
## Getting Started ##

  1. Download latest tar file from release.
  2. Unzip it in a custom location. (Let's say, $SCRIPT_DEPLOYER_HOME)
  3. Edit $SCRIPT_DEPLOYER_HOME/deployer/application.properties
      - "app.script.execute.command" - set the exact command that need to be executed for each file/script.
  3. Place scripts in files folder ($SCRIPT_DEPLOYER_HOME/files).
  4. Go to folder $SCRIPT_DEPLOYER_HOME/deployer
  5. Execute deploy.bat or deploy.sh to run the tool.
  6. Monitor the logs for any error in files otherwise successful execution.

## Advance configuration ##



## FAQs ##

  - What will happen if file is removed from actual location?
    - Answer 1

  - What will happen if file is moved in some other folder?
    - Answer 2

  - What will happen if a one-time file is modified after execution?
    - Answer 3

## Build and Run (Gradle Wrapper)

This project now includes the Gradle Wrapper for portability. You do not need a local Gradle installation; only Java 8+ is required.

Common commands:
- On Linux/Mac: ./gradlew clean assemble
- On Windows:   gradlew.bat clean assemble
- Run tests:    ./gradlew test
- Run app:      ./gradlew bootRun

Outputs:
- Bootable jar: build/libs/script-deployer.jar
- Distribution tar.gz: build/distributions/script-deployer-2.0.3-RELEASE.tar.gz

Note about gradle-wrapper.jar:
- The wrapper scripts expect gradle/wrapper/gradle-wrapper.jar to exist. If it is missing in your checkout, regenerate it with any local Gradle installation:
  - gradle wrapper --gradle-version 6.9.4
  - or use an existing Gradle to run: ./gradlew wrapper --gradle-version 6.9.4
This will download the wrapper JAR and ensure the wrapper is fully self-contained for subsequent users/CI.

#### Project Link ####
https://github.com/techy-planet/script-deployer
