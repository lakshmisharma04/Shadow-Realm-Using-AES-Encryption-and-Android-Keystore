# Run Shadow Realm on Windows

This guide assumes you have the corrected project ZIP downloaded and extracted to a folder you can find again.

## 1. Install the required programs

Install:

- Android Studio, including Android SDK Platform 37 and an Android emulator.
- JDK 21.
- MySQL 8.

The project includes the Maven and Gradle wrapper files, so you do not need to install Maven or Gradle separately. The first build needs an internet connection to download project dependencies.

## 2. Create the MySQL database

Start MySQL, then open MySQL Workbench and run:

```sql
CREATE DATABASE IF NOT EXISTS shadowrealm CHARACTER SET utf8mb4;
```

This creates the database only if it does not already exist. Do not drop or recreate a database that already contains data. On startup, Hibernate's existing `ddl-auto=update` setting may add or update schema objects, including the nullable `users.token_version` column. This project does not issue a database reset. Back up any existing production data and review schema changes before using it in production.

## 3. Start the backend

Open PowerShell in the extracted `Backend` folder. Set your local MySQL login and generate fresh random application keys in this PowerShell window:

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/shadowrealm"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new("", (Read-Host "MySQL password" -AsSecureString)).Password

$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$bytes = New-Object byte[] 32
$rng.GetBytes($bytes)
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
$rng.GetBytes($bytes)
$env:SECRET_ENCRYPTION_KEY = [Convert]::ToBase64String($bytes)
$rng.Dispose()

.\mvnw.cmd spring-boot:run
```

Keep this PowerShell window open while playing. The backend is at `http://localhost:8080`. These keys are only available in this PowerShell session; generate and store suitable secrets using your normal secure deployment method before hosting the API. Never paste them into source files, screenshots, or chat.

To run the database-independent backend unit tests in a second PowerShell window:

```powershell
cd path\to\your\extracted\project\Backend
.\mvnw.cmd "-Dtest=GameRulesTest,JwtServiceTest,UserServiceTest" test
```

To run the entire backend test suite, keep MySQL running and set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, and `SECRET_ENCRYPTION_KEY` in that test window first, then run:

```powershell
.\mvnw.cmd test
```

The full suite includes a Spring application-context test, so it needs a reachable database.

## 4. Run the Android app

1. Open Android Studio.
2. Choose **Open** and select the project's `Android` folder.
3. Let Gradle sync. If asked, select JDK 21 for Gradle and install Android SDK Platform 37.
4. Start an Android emulator.
5. Choose the `app` run configuration and press **Run**.

The emulator reaches the backend on the same PC through `http://10.0.2.2:8080/`, which is the configured default.

To run the Android JVM tests and build a debug APK from PowerShell:

```powershell
cd path\to\your\extracted\project\Android
.\gradlew.bat testDebugUnitTest assembleDebug
```

The APK is created at `Android\app\build\outputs\apk\debug\app-debug.apk`.

## 5. Connect a real phone (local testing only)

Put the phone and PC on the same trusted Wi-Fi network. Find the PC's IPv4 address with `ipconfig`, then update `BASE_URL` in:

`Android\app\src\main\java\com\example\loginapp\RetrofitClient.java`

For example, use `http://192.168.1.25:8080/` only if `ipconfig` currently shows that address for your PC. Allow port 8080 through Windows Firewall for the private network. This local HTTP setup sends bearer tokens without transport encryption; do not use it on public/untrusted Wi-Fi or for a publicly hosted server. Configure HTTPS before exposing the backend beyond trusted local development.

## 6. Try an online match

Register four or more accounts, sign each into a separate emulator/device, create a room with one account, and join using the six-character code. The host starts the match. All players must remain connected for the automatic vote resolution, unless the host resolves a stalled vote. Try match history after a game finishes.

**Account note:** registration and sign-in use the live backend. OTP delivery/verification and password reset are not implemented. The password-change screen is connected and requires the current password.

## If something does not start

- Backend database connection error: check MySQL is running, the `shadowrealm` database exists, and the five environment variables are set in that PowerShell window.
- Backend says an application key is missing/too short: regenerate both 32-byte keys using step 3.
- Emulator cannot reach the API: keep Spring Boot running and use `10.0.2.2`, not `localhost`.
- Gradle reports an SDK missing: install Android SDK Platform 37 from Android Studio's SDK Manager.
- For another error, copy the first actual `ERROR`/`Caused by` message and the command you ran. Do not share passwords, JWT secrets, or encryption keys.
