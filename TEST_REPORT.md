# Verification report

Checks below were run in the Replit Linux workspace on 2026-10-23, against this corrected project copy. No Android SDK or MySQL server was available. No test used or modified a production database.

## Passed

| Check | Command / method | Actual result |
|---|---|---|
| Backend compile and focused unit tests | `cd Backend && bash mvnw "-Dtest=GameRulesTest,JwtServiceTest,UserServiceTest" test` | **BUILD SUCCESS**. Compiled 67 backend source files and 4 test source files. 17 tests passed; 0 failures, 0 errors, 0 skipped. |
| Backend package | `cd Backend && bash mvnw -DskipTests package` | **BUILD SUCCESS**. Spring Boot executable JAR created at `Backend/target/backend-0.0.1-SNAPSHOT.jar` (build output is not included in the ZIP). |
| Android XML syntax | Python XML parser over `Android/app/src/main` | **42 XML files parsed successfully.** This is not an Android resource build. |
| Suspicious demo credentials/default markers | Search of `Backend/` and `Android/` | **0 matches** for the audited demo token and weak-key placeholder patterns. |

## Blocked by local services / SDK

| Check | Actual result |
|---|---|
| Full backend test suite: `cd Backend && bash mvnw test` | **18 tests total: 17 passed, 0 assertion failures, 1 error.** `BackendApplicationTests.contextLoads` could not connect to MySQL at `localhost:3306` (`Connection refused`). The 17 database-independent unit tests pass. |
| Android unit tests and debug APK: `cd Android && bash gradlew --no-configuration-cache --no-daemon --console=plain testDebugUnitTest assembleDebug` | Gradle stopped before Java/resource compilation: **SDK location not found** (`ANDROID_HOME` and `Android/local.properties` are unset). No Android build or Android unit tests were completed. |

## Not verified against live services or devices

- Spring application startup, schema update, and REST behavior against MySQL.
- Registration/login from the Android app, multiplayer room synchronization, private role/word delivery, night actions, voting, winner/results, and match-history persistence across multiple devices.
- Android screens, emulator behavior, and physical-phone connectivity.

These are not reported as verified features. The source/API connections were reviewed and the Java backend sources compile, but live end-to-end checks require MySQL and an Android SDK/device.

## Windows commands to complete the blocked checks

After installing JDK 21, MySQL 8, Android Studio, and Android SDK Platform 37, follow `BUILD_AND_RUN.md`.

From a PowerShell window with the backend database and five environment variables set:

```powershell
cd path\to\extracted\project\Backend
.\mvnw.cmd test
```

After Android Studio has installed Platform 37 and Gradle is set to JDK 21:

```powershell
cd path\to\extracted\project\Android
.\gradlew.bat testDebugUnitTest assembleDebug
```

The Android SDK is usually managed from **Android Studio → Tools → SDK Manager**. Do not commit Android `local.properties`; it contains a machine-specific SDK path.
