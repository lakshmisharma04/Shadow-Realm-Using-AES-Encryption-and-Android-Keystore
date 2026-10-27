package com.example.loginapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Final verdict screen. Online: loaded from the server. Offline: read from the local engine. */
public class ResultsActivity extends RealmActivity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_results);
        setupTopBar("THE VERDICT", "THE REALM HAS SPOKEN", "RESULTS");
        if (getIntent().getBooleanExtra("ONLINE", false)) renderOnline();
        else renderLocal();
    }

    private boolean gone() { return isFinishing() || isDestroyed(); }

    private void renderOnline() {
        final String room = getIntent().getStringExtra("ROOM_CODE");
        final TextView result = findViewById(R.id.txtResult), winner = findViewById(R.id.txtWinner),
                detail = findViewById(R.id.tvVerdictDetail), outcome = findViewById(R.id.tvOutcome);
        Button next = findViewById(R.id.btnNextRound), home = findViewById(R.id.btnBackHome);
        result.setText("MATCH COMPLETE");
        winner.setText("Reading the final verdict…");
        detail.setText("");
        outcome.setText("");
        next.setText("VIEW MATCH HISTORY  ✦");
        next.setOnClickListener(v -> { OnlineSession.clear(this); startActivity(new Intent(this, HistoryActivity.class)); finish(); });
        home.setText("RETURN TO THE REALM");
        home.setOnClickListener(v -> { OnlineSession.clear(this); returnHome(v); });
        if (room == null) return;

        RetrofitClient.getApiService(this).game(room).enqueue(new Callback<GameResponse>() {
            @Override public void onResponse(Call<GameResponse> c, Response<GameResponse> r) {
                if (gone()) return;
                if (!r.isSuccessful() || r.body() == null) { winner.setText(Ui.errorMessage(r, "The verdict could not be loaded.")); return; }
                GameResponse g = r.body();
                if (!g.isFinished()) {   // not over yet: back to the live game
                    Intent i = new Intent(ResultsActivity.this, GameActivity.class);
                    i.putExtra("ROOM_CODE", room);
                    startActivity(i); finish();
                    return;
                }
                winner.setText(g.getWinner() == null ? "THE HOST ENDED THE MATCH" : "🏆 " + Roles.winnerLabel(g.getWinner()) + " WIN");
                outcome.setText("ROUND " + g.getCurrentRound());
                if (g.getLastEventMessage() != null) detail.setText(g.getLastEventMessage());
                loadDetail(room, detail, g.getLastEventMessage());
            }
            @Override public void onFailure(Call<GameResponse> c, Throwable t) { if (!gone()) winner.setText("Cannot reach the realm server."); }
        });
    }

    /** Hidden roles are only revealed here, after the match is over. */
    private void loadDetail(String room, TextView detail, String lastEvent) {
        RetrofitClient.getApiService(this).historyForRoom(room).enqueue(new Callback<MatchDetail>() {
            @Override public void onResponse(Call<MatchDetail> c, Response<MatchDetail> r) {
                if (gone() || !r.isSuccessful() || r.body() == null) return;
                detail.setText((lastEvent == null ? "" : lastEvent + "\n\n") + MatchText.describe(r.body()));
            }
            @Override public void onFailure(Call<MatchDetail> c, Throwable t) { }
        });
    }

    private void renderLocal() {
        GameEngine game = GameEngine.get();
        boolean spectator = getIntent().getBooleanExtra("SPECTATOR", false);
        TextView winner = findViewById(R.id.txtWinner), detail = findViewById(R.id.tvVerdictDetail),
                outcome = findViewById(R.id.tvOutcome), result = findViewById(R.id.txtResult);
        Button next = findViewById(R.id.btnNextRound), home = findViewById(R.id.btnBackHome);

        if (game.winner() == null) {   // nothing to show yet: back to the match
            startActivity(new Intent(this, LocalGameActivity.class));
            finish();
            return;
        }
        result.setText(spectator ? "SPECTATOR MODE • FINAL VERDICT" : "OFFLINE DEMO • FINAL VERDICT");
        winner.setText("🏆 " + Roles.winnerLabel(game.winner()) + " WIN");
        StringBuilder sb = new StringBuilder();
        if (spectator) sb.append("Your story ended, but the realm continued.\n\n");
        sb.append("Hidden roles:");
        for (GameEngine.Player p : game.players())
            sb.append("\n• ").append(p.name).append(" — ").append(Roles.normalize(p.role)).append(p.alive ? " (survived)" : " (eliminated)");
        detail.setText(sb.toString());
        outcome.setText("ROUND " + game.round());
        next.setText("PLAY ANOTHER REALM ✦");
        next.setOnClickListener(v -> {
            GameEngine.get().start(savedName());
            Intent i = new Intent(this, RoleActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            finish();
        });
        home.setOnClickListener(v -> returnHome(v));
    }
}
