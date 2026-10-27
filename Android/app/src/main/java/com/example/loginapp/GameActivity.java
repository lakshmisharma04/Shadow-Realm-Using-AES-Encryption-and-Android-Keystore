package com.example.loginapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ONLINE game screen. The server is the single source of truth: this screen only reads
 * the state (game + my private player + players) and shows the matching controls.
 * Offline/demo matches use LocalGameActivity instead.
 */
public class GameActivity extends RealmActivity {
    private static final long POLL_MS = 2500L;

    private String room;
    private GameResponse game;
    private PrivatePlayerResponse me;
    private List<PlayerResponse> playerList;
    private Button chat, primary, vote, backHome;
    private TextView round, status, hint, roleView, players;

    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private boolean polling = false;
    private boolean requestInFlight = false;   // one refresh chain at a time
    private boolean actionInFlight = false;    // host actions (open council / force dawn)
    private boolean errorShown = false;        // show a connection problem once per failure streak
    private boolean leaving = false;           // results/home already being opened
    private Call<GameResponse> gameCall;
    private Call<PrivatePlayerResponse> meCall;
    private Call<List<PlayerResponse>> playersCall;

    private final Runnable pollTask = new Runnable() {
        @Override public void run() {
            if (!polling || isFinishing() || isDestroyed()) return;
            refresh();
            pollHandler.postDelayed(this, POLL_MS);
        }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.game_screen);
        setupTopBar("SHADOW REALM", "THE REALM IS LIVE • STAY ALERT", "GAME");
        room = getIntent().getStringExtra("ROOM_CODE");
        if (room == null) room = OnlineSession.room(this);

        round = findViewById(R.id.tvRound); status = findViewById(R.id.txtGameStatus);
        hint = findViewById(R.id.tvGameHint); roleView = findViewById(R.id.tvGameRole);
        players = findViewById(R.id.tvAlivePlayers); chat = findViewById(R.id.btnChat);
        primary = findViewById(R.id.btnNight); vote = findViewById(R.id.btnVote);
        backHome = findViewById(R.id.btnBackToHomeFromGame);
        findViewById(R.id.btnSpectateEnd).setVisibility(View.GONE);
        backHome.setVisibility(View.GONE);

        if (room == null) { Ui.toast(this, "No active realm found."); finish(); return; }
        // Remember the room so the other screens (and a restarted app) can find it again.
        if (OnlineSession.room(this) == null) OnlineSession.saveRoom(this, room, false);
    }

    // ------------------------------------------------------------------ data

    private void refresh() {
        if (requestInFlight || isFinishing() || isDestroyed() || leaving) return;
        requestInFlight = true;
        gameCall = RetrofitClient.getApiService(this).game(room);
        gameCall.enqueue(new Callback<GameResponse>() {
            @Override public void onResponse(Call<GameResponse> c, Response<GameResponse> r) {
                if (isGone()) { requestInFlight = false; return; }
                if (r.isSuccessful() && r.body() != null) { game = r.body(); loadMe(); }
                else failed(r, "The realm state could not be loaded.");
            }
            @Override public void onFailure(Call<GameResponse> c, Throwable t) { networkFailed(c); }
        });
    }

    private void loadMe() {
        meCall = RetrofitClient.getApiService(this).currentPlayer(room);
        meCall.enqueue(new Callback<PrivatePlayerResponse>() {
            @Override public void onResponse(Call<PrivatePlayerResponse> c, Response<PrivatePlayerResponse> r) {
                if (isGone()) { requestInFlight = false; return; }
                if (r.isSuccessful() && r.body() != null) { me = r.body(); loadPlayers(); }
                else failed(r, "Your private player state is unavailable.");
            }
            @Override public void onFailure(Call<PrivatePlayerResponse> c, Throwable t) { networkFailed(c); }
        });
    }

    private void loadPlayers() {
        playersCall = RetrofitClient.getApiService(this).players(room);
        playersCall.enqueue(new Callback<List<PlayerResponse>>() {
            @Override public void onResponse(Call<List<PlayerResponse>> c, Response<List<PlayerResponse>> r) {
                if (isGone()) { requestInFlight = false; return; }
                if (r.isSuccessful() && r.body() != null) playerList = r.body();
                // The player list is cosmetic; render with the previous one if it failed.
                requestInFlight = false;
                errorShown = false;
                render();
            }
            @Override public void onFailure(Call<List<PlayerResponse>> c, Throwable t) {
                if (c.isCanceled() || isGone()) { requestInFlight = false; return; }
                requestInFlight = false;
                render();
            }
        });
    }

    private boolean isGone() { return isFinishing() || isDestroyed() || leaving; }

    private void networkFailed(Call<?> call) {
        requestInFlight = false;
        if (call.isCanceled() || isGone()) return;
        if (!errorShown) { errorShown = true; Ui.toast(this, "Cannot reach the realm server. Retrying…"); }
    }

    private void failed(Response<?> r, String fallback) {
        requestInFlight = false;
        if (isGone()) return;
        int code = r.code();
        if (code == 401 || code == 403) {
            leaving = true;
            Ui.toast(this, "Your session has expired. Please sign in again.");
            logout();
        } else if (code == 404) {
            leaving = true;
            OnlineSession.clear(this);
            Ui.toast(this, "This realm no longer exists.");
            returnHome(null);
        } else if (!errorShown) {
            errorShown = true;
            Ui.toast(this, Ui.errorMessage(r, fallback));
        }
    }

    // ------------------------------------------------------------------ rendering

    private void render() {
        if (game == null || me == null || isGone()) return;
        OnlineSession.syncRound(this, game.getCurrentRound());
        round.setText("ROUND " + game.getCurrentRound() + "  •  " + game.getPhase());
        roleView.setText((me.getRole() == null ? "ROLE HIDDEN" : me.getRole()) + "  •  PRIVATE");
        showPlayers();

        if (game.isFinished()) { openResults(); return; }

        String event = game.getLastEventMessage();
        String seer = "SEER".equals(me.getRole()) ? OnlineSession.seerNote(this, game.getCurrentRound()) : null;
        String seerLine = seer == null ? "" : "\n🔮 " + seer;

        if (!me.isAlive()) {
            status.setText("YOU HAVE FALLEN INTO THE SHADOWS.");
            hint.setText("You can no longer act or speak. Watch the realm unfold." + (event == null ? "" : "\n" + event));
            primary.setText("WATCHING THE REALM");
            primary.setEnabled(false);
            vote.setText("YOU CANNOT ACT"); vote.setEnabled(false);
            chat.setEnabled(false);
            backHome.setVisibility(View.VISIBLE);
            return;
        }
        backHome.setVisibility(View.GONE);

        String phase = game.getPhase() == null ? "" : game.getPhase();
        boolean host = me.isHost();
        chat.setEnabled("DAY".equals(phase) || "VOTING".equals(phase));
        chat.setOnClickListener(v -> { Intent i = new Intent(this, ChatActivity.class); i.putExtra("ROOM_CODE", room); startActivity(i); });

        switch (phase) {
            case "NIGHT": renderNight(host, event, seerLine); break;
            case "DAY": renderDay(host, event); break;
            case "VOTING": renderVoting(event); break;
            default:
                status.setText("THE REALM STIRS…");
                hint.setText("Waiting for the next phase.");
                primary.setText("WAITING"); primary.setEnabled(false);
                vote.setEnabled(false);
        }
    }

    private void renderNight(boolean host, String event, String seerLine) {
        boolean special = Roles.hasNightAction(me.getRole());
        boolean acted = me.isNightActionSubmitted();
        status.setText("☾  THE NIGHT FALLS.");
        String last = (event == null) ? "" : "\nLast council: " + event;
        if (special && !acted) {
            hint.setText("Choose your night action. Traitor hunts, Doctor protects, Seer investigates." + last);
            primary.setText("☾  NIGHT ACTION"); primary.setEnabled(true);
            primary.setOnClickListener(v -> { Intent i = new Intent(this, NightActionActivity.class); i.putExtra("ROOM_CODE", room); startActivity(i); });
        } else if (special) {
            // Waiting state after submitting: no second submission possible, dawn comes when everyone has acted.
            hint.setText("Your action is sealed. Waiting for the others… dawn comes when every night role has acted." + seerLine + last);
            primary.setText("✔  ACTION SEALED • WAITING FOR DAWN"); primary.setEnabled(false);
        } else {
            hint.setText("You have no night power. Dawn comes when every night role has acted." + last);
            primary.setText("☾  THE VILLAGE SLEEPS"); primary.setEnabled(false);
        }
        if (host && (!special || acted)) {
            vote.setText("☀  FORCE DAWN"); vote.setEnabled(!actionInFlight);
            vote.setOnClickListener(v -> forceDawn());
        } else {
            vote.setText("WAITING FOR DAWN"); vote.setEnabled(false);
        }
    }

    private void renderDay(boolean host, String event) {
        status.setText(event != null ? "☀  " + event : "☀  THE VILLAGE WAKES.");
        hint.setText("Discuss the secret without saying it. The host opens the council when everyone is ready.");
        primary.setText("☀  DAY DISCUSSION"); primary.setEnabled(false);
        vote.setText(host ? "⚔  OPEN COUNCIL" : "WAIT FOR COUNCIL");
        vote.setEnabled(host && !actionInFlight);
        vote.setOnClickListener(v -> openCouncil());
    }

    private void renderVoting(String event) {
        status.setText("⚖  THE COUNCIL IS OPEN.");
        primary.setText("COUNCIL IS OPEN"); primary.setEnabled(false);
        if (me.isVoteSubmitted()) {
            if (me.isHost()) {
                hint.setText("Your vote is sealed. If someone is AFK, you can resolve the council using votes already submitted.");
                vote.setText("⚖  RESOLVE COUNCIL");
                vote.setEnabled(!actionInFlight);
                vote.setOnClickListener(v -> resolveVoting());
            } else {
                hint.setText("Your vote is sealed. Waiting for the host or every living player to finish voting.");
                vote.setText("✔  VOTE RECORDED • WAITING"); vote.setEnabled(false);
            }
        } else {
            hint.setText("Choose carefully. Every living player must cast one vote. A tie eliminates nobody.");
            vote.setText("⚔  CAST MY VOTE"); vote.setEnabled(true);
            vote.setOnClickListener(v -> { Intent i = new Intent(this, VotingActivity.class); i.putExtra("ROOM_CODE", room); startActivity(i); });
        }
    }

    private void resolveVoting() {
        if (actionInFlight) return;
        actionInFlight = true;
        vote.setEnabled(false);
        RetrofitClient.getApiService(this).resolveVote(room).enqueue(new Callback<Object>() {
            @Override public void onResponse(Call<Object> c, Response<Object> r) {
                actionInFlight = false;
                if (isGone()) return;
                if (r.isSuccessful()) {
                    Ui.toast(GameActivity.this, "Council resolved.");
                    refresh();
                } else {
                    vote.setEnabled(true);
                    Ui.toast(GameActivity.this, Ui.errorMessage(r, "Could not resolve the council."));
                }
            }
            @Override public void onFailure(Call<Object> c, Throwable t) {
                actionInFlight = false;
                if (isGone()) return;
                vote.setEnabled(true);
                Ui.toast(GameActivity.this, "Cannot reach the realm server.");
            }
        });
    }

    private void showPlayers() {
        if (playerList == null) return;
        StringBuilder sb = new StringBuilder("LIVING PLAYERS\n");
        for (PlayerResponse p : playerList) if (p.isAlive())
            sb.append("◈ ").append(p.getDisplayName() == null ? "Player" : p.getDisplayName())
              .append(p.isHost() ? "  •  HOST" : "").append("\n");
        players.setText(sb.toString().trim());
    }

    private void openResults() {
        if (leaving) return;
        leaving = true;
        polling = false;
        pollHandler.removeCallbacks(pollTask);
        Intent i = new Intent(this, ResultsActivity.class);
        i.putExtra("ONLINE", true);
        i.putExtra("ROOM_CODE", room);
        startActivity(i);
        finish();
    }

    // ------------------------------------------------------------------ host actions

    private void openCouncil() {
        if (actionInFlight) return;
        actionInFlight = true; vote.setEnabled(false);
        RetrofitClient.getApiService(this).nextPhase(room).enqueue(new Callback<GameResponse>() {
            @Override public void onResponse(Call<GameResponse> c, Response<GameResponse> r) {
                actionInFlight = false;
                if (isGone()) return;
                if (r.isSuccessful()) refresh();
                else { vote.setEnabled(true); Ui.toast(GameActivity.this, Ui.errorMessage(r, "The council cannot open yet.")); }
            }
            @Override public void onFailure(Call<GameResponse> c, Throwable t) {
                actionInFlight = false;
                if (isGone()) return;
                vote.setEnabled(true); Ui.toast(GameActivity.this, "Cannot reach the realm server.");
            }
        });
    }

    /** Host fallback when a Doctor/Seer is away: ends the night once the Traitor has chosen. */
    private void forceDawn() {
        if (actionInFlight) return;
        actionInFlight = true; vote.setEnabled(false);
        RetrofitClient.getApiService(this).resolveNight(room).enqueue(new Callback<Object>() {
            @Override public void onResponse(Call<Object> c, Response<Object> r) {
                actionInFlight = false;
                if (isGone()) return;
                if (r.isSuccessful()) refresh();
                else { vote.setEnabled(true); Ui.toast(GameActivity.this, Ui.errorMessage(r, "Dawn cannot come yet.")); }
            }
            @Override public void onFailure(Call<Object> c, Throwable t) {
                actionInFlight = false;
                if (isGone()) return;
                vote.setEnabled(true); Ui.toast(GameActivity.this, "Cannot reach the realm server.");
            }
        });
    }

    // ------------------------------------------------------------------ lifecycle

    @Override protected void onResume() {
        super.onResume();
        if (room != null && !leaving) {
            polling = true;
            pollHandler.removeCallbacks(pollTask);
            pollHandler.post(pollTask);
        }
    }

    @Override protected void onPause() {
        polling = false;
        pollHandler.removeCallbacks(pollTask);
        cancelCalls();
        super.onPause();
    }

    @Override protected void onDestroy() {
        polling = false;
        pollHandler.removeCallbacksAndMessages(null);
        cancelCalls();
        super.onDestroy();
    }

    private void cancelCalls() {
        if (gameCall != null) gameCall.cancel();
        if (meCall != null) meCall.cancel();
        if (playersCall != null) playersCall.cancel();
        requestInFlight = false;
    }
}
