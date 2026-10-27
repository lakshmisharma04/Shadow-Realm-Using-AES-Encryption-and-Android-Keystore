package com.example.loginapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VotingActivity extends RealmActivity {
    private String room;
    private Long myId;
    private RadioGroup group;
    private Button submit;
    private boolean submitting=false;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_voting);
        setupTopBar("THE COUNCIL", "ONE VOTE • ONE CHANCE TO EXPOSE THE SHADOW", "VOTE");
        room=getIntent().getStringExtra("ROOM_CODE");
        if(room==null) room=OnlineSession.room(this);
        group=findViewById(R.id.playerGroup);
        submit=findViewById(R.id.btnSubmitVote);
        submit.setEnabled(false);
        if(room==null){Ui.toast(this,"No active realm found.");finish();return;}
        loadMe();
    }

    private boolean gone(){return isFinishing()||isDestroyed();}

    private void loadMe(){
        RetrofitClient.getApiService(this).currentPlayer(room).enqueue(new Callback<PrivatePlayerResponse>() {
            @Override public void onResponse(Call<PrivatePlayerResponse> c, Response<PrivatePlayerResponse> r) {
                if(gone())return;
                if(!r.isSuccessful()||r.body()==null){Ui.toast(VotingActivity.this,Ui.errorMessage(r,"Your player state is unavailable."));return;}
                PrivatePlayerResponse me=r.body();
                myId=me.getId();
                if(!me.isAlive()){Ui.toast(VotingActivity.this,"Fallen players cannot vote.");goGame();return;}
                if(me.isVoteSubmitted()){Ui.toast(VotingActivity.this,"Your vote is already sealed.");goGame();return;}
                loadPlayers();
            }
            @Override public void onFailure(Call<PrivatePlayerResponse> c, Throwable t) {if(!gone())Ui.toast(VotingActivity.this,"Cannot reach the realm server.");}
        });
    }

    private void loadPlayers(){
        RetrofitClient.getApiService(this).alivePlayers(room).enqueue(new Callback<List<PlayerResponse>>() {
            @Override public void onResponse(Call<List<PlayerResponse>> c, Response<List<PlayerResponse>> r){
                if(gone())return;
                if(!r.isSuccessful()||r.body()==null){Ui.toast(VotingActivity.this,Ui.errorMessage(r,"Could not load the council."));return;}
                renderTargets(r.body());
            }
            @Override public void onFailure(Call<List<PlayerResponse>> c, Throwable t){if(!gone())Ui.toast(VotingActivity.this,"Cannot reach the realm server.");}
        });
    }

    private void renderTargets(List<PlayerResponse> targets){
        RadioButton[] buttons={findViewById(R.id.player1),findViewById(R.id.player2),findViewById(R.id.player3),findViewById(R.id.player4),findViewById(R.id.player5),findViewById(R.id.player6)};
        int i=0;
        for(PlayerResponse p:targets){
            if(p.getId()==null||p.getId().equals(myId)) continue;   // no self-votes
            if(i>=buttons.length)break;
            buttons[i].setText((p.getDisplayName()==null?"Player":p.getDisplayName()));
            buttons[i].setTag(p.getId()); buttons[i].setVisibility(View.VISIBLE); i++;
        }
        while(i<buttons.length){buttons[i].setVisibility(View.GONE);i++;}
        submit.setText("⚔  LOCK MY VOTE");
        submit.setEnabled(true);
        submit.setOnClickListener(v->cast());
    }

    private void cast(){
        if(submitting)return;
        int id=group.getCheckedRadioButtonId();
        if(id==-1){Ui.toast(this,"Choose a suspect before locking your vote.");return;}
        View selected=findViewById(id);
        Object tag=selected==null?null:selected.getTag();
        if(!(tag instanceof Long)){Ui.toast(this,"That target is not valid.");return;}
        submitting=true; submit.setEnabled(false);
        // The server takes the voter from your login token; the voterId below is ignored by it.
        VoteRequest req=new VoteRequest(room,myId==null?0L:myId,(Long)tag);
        RetrofitClient.getApiService(this).castVote(req).enqueue(new Callback<Object>(){
            @Override public void onResponse(Call<Object> c, Response<Object> r){
                if(gone())return;
                if(r.isSuccessful()){
                    OnlineSession.markVoted(VotingActivity.this);
                    Ui.toast(VotingActivity.this,"Your vote is sealed. Waiting for the council.");
                    goGame();   // the server resolves the round as soon as the last living player has voted
                } else {
                    Ui.toast(VotingActivity.this,Ui.errorMessage(r,"The server rejected your vote."));
                    if(r.code()==400){goGame();return;}   // e.g. already voted / voting closed: show the true state
                    submitting=false; submit.setEnabled(true);
                }
            }
            @Override public void onFailure(Call<Object> c, Throwable t){
                if(gone())return; submitting=false; submit.setEnabled(true);Ui.toast(VotingActivity.this,"Cannot reach the server. Try again.");
            }
        });
    }

    private void goGame(){Intent i=new Intent(this,GameActivity.class);i.putExtra("ROOM_CODE",room);i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);startActivity(i);finish();}
}
