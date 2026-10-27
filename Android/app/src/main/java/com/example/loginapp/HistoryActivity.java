package com.example.loginapp;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Lists the signed-in user's finished online matches (loaded from the server; nothing is stored locally). */
public class HistoryActivity extends RealmActivity {
    private TextView statusView;
    private LinearLayout list;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_history);
        setupTopBar("MATCH HISTORY", "THE REALMS YOU HAVE SURVIVED", "HISTORY");
        statusView = findViewById(R.id.tvHistoryStatus);
        list = findViewById(R.id.historyList);
        load();
    }

    private boolean gone() { return isFinishing() || isDestroyed(); }

    private void load() {
        RetrofitClient.getApiService(this).history().enqueue(new Callback<List<MatchSummary>>() {
            @Override public void onResponse(Call<List<MatchSummary>> c, Response<List<MatchSummary>> r) {
                if (gone()) return;
                if (!r.isSuccessful() || r.body() == null) {
                    statusView.setText(Ui.errorMessage(r, "Your history could not be loaded."));
                    return;
                }
                show(r.body());
            }
            @Override public void onFailure(Call<List<MatchSummary>> c, Throwable t) {
                if (!gone()) statusView.setText("Cannot reach the realm server. Check your connection and reopen this screen.");
            }
        });
    }

    private void show(List<MatchSummary> matches) {
        list.removeAllViews();
        if (matches.isEmpty()) {
            statusView.setText("No finished matches yet. Play an online realm to the end and it will appear here.");
            return;
        }
        statusView.setText(matches.size() + " finished match" + (matches.size() == 1 ? "" : "es") + " • tap one for details");
        for (MatchSummary m : matches) {
            TextView row = new TextView(this);
            String when = m.getFinishedAt() == null ? "" : m.getFinishedAt().replace('T', ' ');
            if (when.length() > 16) when = when.substring(0, 16);
            row.setText((m.isWon() ? "🏆 WON" : "☠ LOST") + "  •  Room " + m.getRoomCode()
                    + "\nYou were " + (m.getMyRole() == null ? "?" : Roles.normalize(m.getMyRole()))
                    + (m.isSurvived() ? " (survived)" : " (eliminated)")
                    + "\n" + Roles.winnerLabel(m.getWinner()) + " won • " + m.getRounds() + " round" + (m.getRounds() == 1 ? "" : "s")
                    + " • " + m.getPlayerCount() + " players\n" + when);
            row.setTextColor(getResources().getColor(R.color.realm_lilac));
            row.setTextSize(13);
            row.setPadding(dp(14), dp(12), dp(14), dp(12));
            row.setBackgroundResource(R.drawable.realm_panel_soft);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(8);
            row.setLayoutParams(lp);
            final Long id = m.getMatchId();
            row.setOnClickListener(v -> openDetail(id));
            list.addView(row);
        }
    }

    private void openDetail(Long matchId) {
        if (matchId == null) return;
        RetrofitClient.getApiService(this).historyDetail(matchId).enqueue(new Callback<MatchDetail>() {
            @Override public void onResponse(Call<MatchDetail> c, Response<MatchDetail> r) {
                if (gone()) return;
                if (!r.isSuccessful() || r.body() == null) { Ui.toast(HistoryActivity.this, Ui.errorMessage(r, "Match details are unavailable.")); return; }
                new AlertDialog.Builder(HistoryActivity.this)
                        .setTitle("Room " + r.body().getRoomCode())
                        .setMessage(MatchText.describe(r.body()))
                        .setPositiveButton("CLOSE", null)
                        .show();
            }
            @Override public void onFailure(Call<MatchDetail> c, Throwable t) { if (!gone()) Ui.toast(HistoryActivity.this, "Cannot reach the realm server."); }
        });
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }
}
