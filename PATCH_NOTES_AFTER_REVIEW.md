# Patch notes after review

This archive contains targeted repairs to the supplied project. It is not certified as a production-ready release; see `TEST_REPORT.md` for the exact checks and limitations.

## Changes made

1. **Doctor self-protection UI:** the Doctor can select themselves as a night-action target. Other roles still cannot target themselves.
2. **Stalled-vote fallback:** the host can resolve a stalled council; only votes submitted by living players count and missing votes are abstentions. A tie eliminates nobody.
3. **Error handling:** unexpected exceptions are logged on the server without returning raw exception details to clients.
4. **Credentials and private game state:** registration uses a DTO and BCrypt; JWT/encryption keys are required; password changes revoke older JWTs; the OTP demo no longer creates a fake session; live votes/roles/words remain private.
5. **Room/API correctness:** room capacity, ownership, membership and concurrent start/join checks are enforced server-side; Retrofit response types match the backend.
6. **Docs and tests:** Windows setup, live-backend requirements and actual test outcomes are recorded in `BUILD_AND_RUN.md` and `TEST_REPORT.md`.

## Not verified

Backend compilation/package and 17 database-independent tests succeeded. The complete backend suite reached the application-context test but could not connect to MySQL. The Android build stopped before compilation because no Android SDK is installed here. No live database, emulator, or multi-device test was completed. Exact outputs and Windows commands are in `TEST_REPORT.md` and `BUILD_AND_RUN.md`.

## Phone connection reminder

The default Android URL `http://10.0.2.2:8080/` is for the Android emulator. For a physical phone, change `BASE_URL` in `Android/app/src/main/java/com/example/loginapp/RetrofitClient.java` to `http://<YOUR_LAPTOP_WIFI_IPV4>:8080/`; connect both devices to the same Wi-Fi and allow the port through Windows Firewall. Do not use the example IP unless it is currently assigned to your laptop.
