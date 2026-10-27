# Shadow Realm

Shadow Realm is a native Android multiplayer social-deduction game backed by a Spring Boot REST API and MySQL.

- `Android/` — Java/XML Android app. Open this folder in Android Studio.
- `Backend/` — Spring Boot 3.5 API, Java 21, Maven, MySQL. Runs on port 8080.

## Online match flow

1. Register and sign in. Passwords are stored as BCrypt hashes; JWTs are stored encrypted with Android Keystore.
2. Create or join a 4–7 player waiting room. Only the host can start or delete a waiting room.
3. The server privately assigns roles and the round's word/hint.
4. Traitor, Doctor, and Seer submit private night actions. The server resolves night automatically when everyone living with a night role acts; the host can force dawn after the Traitor submits.
5. Players discuss during the day. The host opens the council.
6. Living players vote once. The last vote resolves the round automatically; a host can resolve a stalled council using only votes already submitted. A tie eliminates nobody.
7. The server checks the winner, saves match history, and sends the same public result to every player.

The offline demo is local-only and does not use the backend or save online match history.

## Security and setup notes

- Supply `JWT_SECRET` and `SECRET_ENCRYPTION_KEY`; the server refuses to start if either is missing or shorter than 32 UTF-8 bytes.
- The local Android URL uses HTTP for emulator/phone testing. Do not expose this configuration or send real traffic over an untrusted network; use HTTPS before public use.
- The OTP screen is informational only because the backend does not implement OTP delivery or verification. It cannot create a session.
- Password changes require the current password, replace the current JWT, and revoke older JWTs.
- Database schema updates are additive in this project. The backend was not pointed at or used to modify an existing database during this repair.

See `BUILD_AND_RUN.md` for beginner-friendly Windows instructions and `TEST_REPORT.md` for commands and the results actually obtained.
