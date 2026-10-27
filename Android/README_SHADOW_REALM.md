# Shadow Realm Android app

This is the native Java/XML Android client for the Spring Boot backend in `../Backend`. It is not a frontend-only demo: registration, login, room creation/joining, lobby synchronization, private role/word retrieval, night actions, discussion, voting, results, and online match history use the server API.

Open the `Android` folder in Android Studio. The emulator's backend address is `http://10.0.2.2:8080/`. For a trusted local physical-device test, use your current PC LAN IPv4 address in `RetrofitClient.BASE_URL`; HTTP is not safe for public or untrusted networks.

The app's online flow:

1. Create an account or sign in.
2. Create a waiting room or join by room code.
3. Wait for at least four players; only the host can start.
4. Reveal the role and private word/hint returned for the signed-in player.
5. Submit role-appropriate night action, discuss, vote, and follow server results.
6. View online match history after the match ends.

**The OTP screen is not connected to an OTP service and cannot authenticate users.** Registration uses the backend directly. The profile's password-change screen calls the backend, requires the current password, and replaces the local JWT after the change.

The **Quick Start** mode is a separate offline game engine; it does not use the server or add entries to online match history. Windows setup and build commands are in the root `BUILD_AND_RUN.md`.
