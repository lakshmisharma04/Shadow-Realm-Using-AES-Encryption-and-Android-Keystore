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

public class WaitingRoomActivity extends RealmActivity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView players, status;
    private Button start;
    private String room;
    private boolean opening;
    private boolean resumed = false;   // poll only while the screen is visible
    private boolean busy = false;      // one request chain at a time
    private final Runnable poller = new Runnable() { @Override public void run() { refresh(); } };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_waiting_room);
        setupTopBar("THE LOBBY", "GATHER YOUR CIRCLE • SERVER SYNCHRONIZED", "LOBBY");
        TextView code = findViewById(R.id.tvRoomCode);
        players = findViewById(R.id.tvPlayers);
        status = findViewById(R.id.tvLobbyStatus);
        start = findViewById(R.id.btnStartGame);
        Button add = findViewById(R.id.btnAddPlayer);

        room = getIntent().getStringExtra("ROOM_CODE");
        code.setText("ROOM CODE  •  " + (room == null ? "------" : room));
        add.setVisibility(View.VISIBLE);
        add.getLayoutParams().height = (int)(52 * getResources().getDisplayMetrics().density);
        add.requestLayout();
        add.setText(OnlineSession.host(this) ? "DELETE REALM" : "LEAVE REALM");
        add.setOnClickListener(v -> leaveOrDelete());
        start.setOnClickListener(v -> startGame());
    }

    private void leaveOrDelete() {
        if (room == null) return;
        if (OnlineSession.host(this)) {
            RetrofitClient.getApiService(this).deleteRoom(room).enqueue(new Callback<Void>() {
                @Override public void onResponse(Call<Void> c, Response<Void> r) {
                    if (r.isSuccessful()) { OnlineSession.clear(WaitingRoomActivity.this); returnHome(null); }
                    else Ui.toast(WaitingRoomActivity.this, "The realm could not be deleted.");
                }
                @Override public void onFailure(Call<Void> c, Throwable t) { Ui.toast(WaitingRoomActivity.this, "Cannot reach the realm server."); }
            });
        } else {
            RetrofitClient.getApiService(this).leaveRoom(room).enqueue(new Callback<RoomResponse>() {
                @Override public void onResponse(Call<RoomResponse> c, Response<RoomResponse> r) {
                    if (r.isSuccessful()) { OnlineSession.clear(WaitingRoomActivity.this); returnHome(null); }
                    else Ui.toast(WaitingRoomActivity.this, "Could not leave the realm.");
                }
                @Override public void onFailure(Call<RoomResponse> c, Throwable t) { Ui.toast(WaitingRoomActivity.this, "Cannot reach the realm server."); }
            });
        }
    }

    private void refresh() {
        if (room == null || opening || !resumed || busy || isFinishing() || isDestroyed()) return;
        busy = true;
        RetrofitClient.getApiService(this).game(room).enqueue(new Callback<GameResponse>() {
            @Override public void onResponse(Call<GameResponse> call, Response<GameResponse> response) {
                if (isFinishing() || isDestroyed()) { busy = false; return; }
                if (response.isSuccessful() && response.body() != null && response.body().isStarted()) {
                    openRole();
                    return;
                }
                refreshPlayers();
            }
            @Override public void onFailure(Call<GameResponse> call, Throwable t) {
                if (isFinishing() || isDestroyed() || call.isCanceled()) { busy = false; return; }
                refreshPlayers();
            }
        });
    }

    private void refreshPlayers() {
        if (room == null || opening) { busy = false; return; }
        RetrofitClient.getApiService(this).players(room).enqueue(new Callback<List<PlayerResponse>>() {
            @Override public void onResponse(Call<List<PlayerResponse>> call, Response<List<PlayerResponse>> response) {
                if (isFinishing() || isDestroyed()) { busy = false; return; }
                if (!response.isSuccessful() || response.body() == null) { schedule(); return; }
                List<PlayerResponse> list = response.body();
                StringBuilder sb = new StringBuilder();
                for (int i=0;i<list.size();i++) {
                    PlayerResponse p=list.get(i);
                    sb.append("◈ ").append(p.getDisplayName() == null ? "Player" : p.getDisplayName())
                            .append(p.isHost() ? "  •  HOST" : "  •  READY")
                            .append(p.isAlive() ? "" : "  •  ELIMINATED");
                    if (i < list.size()-1) sb.append('\n');
                }
                players.setText(sb.length()==0 ? "Waiting for players…" : sb.toString());
                // Host status belongs to this signed-in session; merely being in
                // a room with a host must not expose the start control to guests.
                boolean host = OnlineSession.host(WaitingRoomActivity.this);
                start.setVisibility(host ? View.VISIBLE : View.GONE);
                start.setEnabled(list.size() >= 4);
                status.setText(list.size() >= 4
                        ? list.size()+" PLAYERS READY  •  THE GATES CAN OPEN"
                        : list.size()+" PLAYER"+(list.size()==1?"":"S")+" READY  •  NEED "+(4-list.size())+" MORE");
                schedule();
            }
            @Override public void onFailure(Call<List<PlayerResponse>> call, Throwable t) {
                if (isFinishing() || isDestroyed() || call.isCanceled()) { busy = false; return; }
                schedule();
            }
        });
    }

    private void schedule() {
        busy = false;
        handler.removeCallbacks(poller);
        if (resumed) handler.postDelayed(poller, 2000);
    }

    private void startGame() {
        if (opening) return;
        opening=true; start.setEnabled(false); start.setText("OPENING THE RITUAL…");
        RetrofitClient.getApiService(this).startGame(room).enqueue(new Callback<GameResponse>() {
            @Override public void onResponse(Call<GameResponse> call, Response<GameResponse> response) {
                if (response.isSuccessful()) openRole();
                else { opening=false; start.setEnabled(true); start.setText("⚔  START THE RITUAL"); Ui.toast(WaitingRoomActivity.this,"The game could not start. Make sure at least four players are present."); }
            }
            @Override public void onFailure(Call<GameResponse> call, Throwable t) { opening=false; start.setEnabled(true); start.setText("⚔  START THE RITUAL"); Ui.toast(WaitingRoomActivity.this,"Cannot reach the realm server."); }
        });
    }

    private void openRole() {
        Intent i=new Intent(this, RoleActivity.class); i.putExtra("ROOM_CODE",room); startActivity(i); finish();
    }

    @Override protected void onResume() {
        super.onResume();
        resumed = true;
        busy = false;
        handler.removeCallbacks(poller);
        refresh();
    }

    @Override protected void onPause() {
        resumed = false;
        handler.removeCallbacks(poller);
        super.onPause();
    }

    @Override protected void onDestroy() { resumed = false; handler.removeCallbacksAndMessages(null); super.onDestroy(); }
}
