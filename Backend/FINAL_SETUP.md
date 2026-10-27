# Shadow Realm backend setup

The backend targets Java 25 (the current LTS) / Spring Boot 3.5 and runs as a REST API using MySQL. The Windows startup steps and secure key-generation commands are in the root `BUILD_AND_RUN.md`.

## Java runtime

Use a JDK 25 installation to run and build the backend. In PowerShell, set `JAVA_HOME` and put its `bin` directory first on `PATH` before starting Maven or the application:

```powershell
$env:JAVA_HOME = "C:\path\to\jdk-25"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
```

Confirm that the reported version is 25. Setting `JAVA_HOME` alone may not affect commands when another Java installation appears earlier on `PATH`.

## Security behavior

- Accounts are created through a validation DTO; passwords are BCrypt-hashed and never returned by API responses.
- JWT signing and secret-word encryption keys are required environment variables. There are no weak placeholder defaults.
- JWTs include a user token version. Existing database rows with a null version are treated as version 0; a password change increments the version, returns a new token, and invalidates older sessions.
- Server routes derive the current account and player from the authenticated JWT. Public player listings do not include private roles or words.
- Room mutations and match transitions use database transactions/locks where simultaneous requests could cause inconsistent state.
- Live votes are never disclosed to other players. A caller can read only their own resolved votes.

## Required environment

Set these before running Spring Boot:

- `DB_URL` (default `jdbc:mysql://localhost:3306/shadowrealm`)
- `DB_USERNAME` (default `root`)
- `DB_PASSWORD` (your local MySQL password)
- `JWT_SECRET` (random; at least 32 UTF-8 bytes)
- `SECRET_ENCRYPTION_KEY` (a separate random key; at least 32 UTF-8 bytes)

Keep production values in a secrets manager/environment configuration, never in source or the project ZIP. The project uses the original Hibernate `ddl-auto=update` setting. This repair did not connect to, reset, or delete data from any database; schema changes should be reviewed/backed up before production use.

## Run and test

Windows PowerShell:

```powershell
cd path\to\extracted\project\Backend
.\mvnw.cmd spring-boot:run
```

The API is normally available at `http://localhost:8080`. Android Emulator uses `http://10.0.2.2:8080/`; physical-device setup is in the root run guide. These HTTP URLs are local-development only. Use HTTPS before public exposure.

Database-independent unit tests:

```powershell
.\mvnw.cmd "-Dtest=GameRulesTest,JwtServiceTest,UserServiceTest" test
```

The full `.\mvnw.cmd test` suite also starts the Spring application context and therefore requires a running MySQL database and the environment values above.
