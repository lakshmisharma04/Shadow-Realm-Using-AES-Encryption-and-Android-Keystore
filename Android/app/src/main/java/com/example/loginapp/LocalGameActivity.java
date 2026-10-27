package com.example.loginapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import java.util.ArrayList;
import java.util.List;

/**
 * OFFLINE / DEMO game screen. Everything runs inside GameEngine on the device:
 * no login, no backend request, no network. The online game uses GameActivity instead,
 * so the two modes never share state.
 */
public class LocalGameActivity extends RealmActivity {
    private interface Picked { void onPicked(String name); }

    private GameEngine engine;
    private Button chat, primary, vote, spectate, backHome;
    private TextView round, status, hint, roleView, players;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.game_screen);
        setupTopBar("SHADOW REALM", "OFFLINE DEMO • PLAYED ON THIS DEVICE", "DEMO");
        engine = GameEngine.get();

        round = findViewById(R.id.tvRound); status = findViewById(R.id.txtGameStatus);
        hint = findViewById(R.id.tvGameHint); roleView = findViewById(R.id.tvGameRole);
        players = findViewById(R.id.tvAlivePlayers); chat = findViewById(R.id.btnChat);
        primary = findViewById(R.id.btnNight); vote = findViewById(R.id.btnVote);
        spectate = findViewById(R.id.btnSpectateEnd); backHome = findViewById(R.id.btnBackToHomeFromGame);
        spectate.setVisibility(View.GONE);
        backHome.setVisibility(View.GONE);

        if (engine.me() == null) {   // e.g. the app process was recreated
            Ui.toast(this, "No offline match is running. Start a new one from Quick Start.");
            returnHome(null);
            return;
        }
        chat.setOnClickListener(v -> showChat());
        render();
    }

    private void render() {
        GameEngine.Player me = engine.me();
        if (me == null || isFinishing() || isDestroyed()) return;
        if (engine.winner() != null) { openResults(); return; }

        round.setText("ROUND " + engine.round() + "  •  " + engine.phase());
        roleView.setText(Roles.normalize(me.role) + "  •  PRIVATE");
        StringBuilder sb = new StringBuilder("LIVING PLAYERS\n");
        for (GameEngine.Player p : engine.players()) if (p.alive) sb.append("◈ ").append(p.name).append("\n");
        players.setText(sb.toString().trim());

        if (!me.alive) {
            status.setText("YOU HAVE FALLEN INTO THE SHADOWS.");
            hint.setText("You can no longer act. The remaining players will finish the match.");
            primary.setText("WATCH THE REALM END"); primary.setEnabled(true);
            primary.setOnClickListener(v -> { engine.spectateToEnd(); openResults(); });
            vote.setText("YOU CANNOT ACT"); vote.setEnabled(false);
            chat.setEnabled(false);
            backHome.setVisibility(View.VISIBLE);
            return;
        }

        String seer = engine.seerMessage();
        String seerLine = seer == null ? "" : "\n🔮 " + seer;

        if ("NIGHT".equals(engine.phase())) {
            status.setText("☾  THE NIGHT FALLS.");
            chat.setEnabled(false);
            vote.setText("WAITING FOR DAWN"); vote.setEnabled(false);
            if (engine.humanHasNightAction()) {
                hint.setText("Choose your night action. Traitor hunts, Doctor protects, Seer investigates." + lastCouncilLine());
                primary.setText("☾  NIGHT ACTION"); primary.setEnabled(true);
                primary.setOnClickListener(v -> nightAction());
            } else {
                hint.setText("You have no night power. The shadows move without you." + lastCouncilLine());
                primary.setText("☾  SLEEP UNTIL DAWN"); primary.setEnabled(true);
                primary.setOnClickListener(v -> { engine.resolveNight(null); afterNight(); });
            }
        } else {
            status.setText("☀  " + engine.lastNightMessage());
            hint.setText("Discuss the secret without saying it, then cast your vote. A tie eliminates nobody." + seerLine);
            primary.setText("☀  DAY DISCUSSION"); primary.setEnabled(false);
            chat.setEnabled(true);
            if (engine.hasVoted(me.name)) {
                vote.setText("✔  VOTE RECORDED"); vote.setEnabled(false);
            } else {
                vote.setText("⚔  CAST MY VOTE"); vote.setEnabled(true);
                vote.setOnClickListener(v -> castVote());
            }
        }
    }

    private String lastCouncilLine() { return councilNote == null ? "" : "\nLast council: " + councilNote; }
    private String councilNote = null;

    private void nightAction() {
        GameEngine.Player me = engine.me();
        List<String> names = new ArrayList<>();
        String title;
        if ("TRAITOR".equals(me.role)) {
            title = "Choose who the shadows take";
            for (GameEngine.Player p : engine.aliveOthers()) if (!"TRAITOR".equals(p.role)) names.add(p.name);
        } else if ("DOCTOR".equals(me.role)) {
            title = "Choose who to protect";
            names.add(me.name);                         // the Doctor may protect themselves
            for (GameEngine.Player p : engine.aliveOthers()) names.add(p.name);
        } else {
            title = "Choose who to investigate";
            for (GameEngine.Player p : engine.aliveOthers()) names.add(p.name);
        }
        pick(title, names, "CONFIRM", name -> { engine.resolveNight(name); afterNight(); });
    }

    private void afterNight() {
        if (engine.winner() != null) { openResults(); return; }
        String msg = engine.lastNightMessage();
        if (engine.seerMessage() != null) msg += "\n\n🔮 " + engine.seerMessage();
        render();
        showInfo("DAWN", msg);
    }

    private void castVote() {
        GameEngine.Player me = engine.me();
        List<String> names = new ArrayList<>();
        for (GameEngine.Player p : engine.aliveOthers()) names.add(p.name);
        pick("Who is the Traitor?", names, "LOCK MY VOTE", name -> {
            if (!engine.vote(me.name, name)) { Ui.toast(this, "That vote is not valid."); render(); return; }
            engine.resolveVotes();
            afterVote();
        });
    }

    private void afterVote() {
        String eliminated = engine.lastEliminated();
        String msg;
        if (eliminated != null) {
            GameEngine.Player p = engine.find(eliminated);
            msg = eliminated + " was eliminated by the council." + (p == null ? "" : " Their role: " + Roles.normalize(p.role) + ".");
        } else {
            msg = "The council was split. Nobody was eliminated.";
        }
        councilNote = msg;
        if (engine.winner() != null) { openResults(); return; }
        render();
        showInfo("THE COUNCIL HAS SPOKEN", msg);
    }

    // ------------------------------------------------------------------ dialogs

    private void pick(String title, List<String> names, String confirmText, Picked picked) {
        if (names.isEmpty()) { Ui.toast(this, "There is nobody to choose."); return; }
        final int[] selected = {-1};
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setSingleChoiceItems(names.toArray(new String[0]), -1, (d, which) -> selected[0] = which)
                .setPositiveButton(confirmText, (d, w) -> {
                    if (selected[0] < 0) { Ui.toast(this, "Choose a player first."); pick(title, names, confirmText, picked); return; }
                    picked.onPicked(names.get(selected[0]));
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void showInfo(String title, String message) {
        if (isFinishing() || isDestroyed()) return;
        new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("CONTINUE", null).show();
    }

    private void showChat() {
        GameEngine.Player me = engine.me();
        StringBuilder log = new StringBuilder();
        for (String line : engine.chat()) log.append(line).append("\n\n");

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad, pad, 0);
        ScrollView scroll = new ScrollView(this);
        TextView text = new TextView(this);
        text.setText(log.toString().trim());
        scroll.addView(text);
        box.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (260 * getResources().getDisplayMetrics().density)));
        EditText input = new EditText(this);
        input.setHint("Say something (never the secret word)");
        box.addView(input);
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));

        new AlertDialog.Builder(this)
                .setTitle("COUNCIL CHAT")
                .setView(box)
                .setPositiveButton("SEND", (d, w) -> {
                    String msg = input.getText().toString().trim();
                    if (!msg.isEmpty()) engine.addChat(me.name, msg);
                    showChat();
                })
                .setNegativeButton("CLOSE", null)
                .show();
    }

    private void openResults() {
        Intent i = new Intent(this, ResultsActivity.class);
        i.putExtra("SPECTATOR", engine.me() != null && !engine.me().alive);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        finish();
    }
}
