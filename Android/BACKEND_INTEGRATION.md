# Android and backend integration

The Android app's `ApiService` is connected to the Spring Boot backend in the sibling `Backend` directory. The Retrofit routes cover:

- account registration, login, and authenticated password change
- room create/join/leave/delete
- room player list and the signed-in player's private role/action state
- game start, state polling, and private word/hint retrieval
- attack/protect/investigate and host-only night resolution
- discussion chat, vote casting, host fallback vote resolution
- participant-only match history and post-match details

The server derives the current account/player from the JWT. Public player responses do not include roles. A member can fetch only their own role and word/hint; live votes are private, and vote reads do not expose other players' choices. Match roles are available only to participants after the match is complete.

## Local addresses

- Android emulator: `http://10.0.2.2:8080/`
- Physical phone on a trusted local network: set `RetrofitClient.BASE_URL` to the PC's current LAN IPv4 address, e.g. `http://192.168.1.25:8080/`.

The HTTP configuration is for local development only because it does not encrypt bearer tokens in transit. Use HTTPS before public deployment. Backend environment variables and Windows instructions are in `../BUILD_AND_RUN.md`.

OTP verification and password reset do not exist on the backend. The OTP screen cannot create an authenticated session; password changes do use the authenticated API.
