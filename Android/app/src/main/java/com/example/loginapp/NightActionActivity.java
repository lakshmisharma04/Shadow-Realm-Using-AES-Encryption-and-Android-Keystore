package com.example.loginapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NightActionActivity extends RealmActivity {
    private String room, role;
    private Long myId;
    private RadioGroup group;
    private RadioButton[] buttons;
    private Button action;
    private boolean submitting = false;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_night);
        setupTopBar("NIGHT PHASE", "THE VILLAGE SLEEPS • CHOOSE CAREFULLY", "NIGHT");
        room=getIntent().getStringExtra("ROOM_CODE");
        if(room==null) room=OnlineSession.room(this);
        group=findViewById(R.id.nightGroup);
        buttons=new RadioButton[]{findViewById(R.id.night1),findViewById(R.id.night2),findViewById(R.id.night3),findViewById(R.id.night4),findViewById(R.id.night5),findViewById(R.id.night6)};
        action=findViewById(R.id.btnNightAction);
        action.setEnabled(false);
        if(room==null){Ui.toast(this,"No active realm found.");finish();return;}
        loadMe();
    }

    private boolean gone(){return isFinishing()||isDestroyed();}

    private void loadMe() {
        RetrofitClient.getApiService(this).currentPlayer(room).enqueue(new Callback<PrivatePlayerResponse>() {
            @Override public void onResponse(Call<PrivatePlayerResponse> c, Response<PrivatePlayerResponse> r) {
                if(gone())return;
                if(r.isSuccessful()&&r.body()!=null){
                    PrivatePlayerResponse me=r.body();
                    role=me.getRole(); myId=me.getId();
                    if(!me.isAlive()){Ui.toast(NightActionActivity.this,"You have fallen. You can no longer act.");goGame();return;}
                    if(me.isNightActionSubmitted()){Ui.toast(NightActionActivity.this,"Your night action is already sealed.");goGame();return;}
                    configure(); loadTargets();
                } else Ui.toast(NightActionActivity.this,Ui.errorMessage(r,"Your private role could not be loaded."));
            }
            @Override public void onFailure(Call<PrivatePlayerResponse> c, Throwable t){if(!gone())Ui.toast(NightActionActivity.this,"Cannot reach the realm server.");}
        });
    }

    private void configure() {
        TextView title=findViewById(R.id.tvNightRole), instruction=findViewById(R.id.tvNightInstruction), note=findViewById(R.id.tvNightNote);
        if("TRAITOR".equals(role)){title.setText("TRAITOR • HUNT");instruction.setText("Choose a living player to eliminate.");action.setText("LOCK TARGET");note.setText("Your attack is private.");}
        else if("SEER".equals(role)){title.setText("SEER • INVESTIGATE");instruction.setText("Choose a living player to inspect.");action.setText("REVEAL TRUTH");note.setText("Only you will see the result.");}
        else if("DOCTOR".equals(role)){title.setText("DOCTOR • PROTECT");instruction.setText("Choose a living player to protect.");action.setText("GUARD PLAYER");note.setText("Your protection can stop the night attack.");}
        else {title.setText("VILLAGER • WAIT");instruction.setText("You have no night power. Wait for dawn.");action.setText("RETURN TO REALM");note.setText("Stay alert for the Day discussion.");
            action.setEnabled(true);action.setOnClickListener(v->goGame());}
    }

    private void loadTargets() {
        RetrofitClient.getApiService(this).alivePlayers(room).enqueue(new Callback<List<PlayerResponse>>() {
            @Override public void onResponse(Call<List<PlayerResponse>> c, Response<List<PlayerResponse>> r) {
                if(gone())return;
                if(!r.isSuccessful()||r.body()==null){Ui.toast(NightActionActivity.this,Ui.errorMessage(r,"Could not load the living players."));return;}
                showTargets(r.body());
            }
            @Override public void onFailure(Call<List<PlayerResponse>> c, Throwable t){if(!gone())Ui.toast(NightActionActivity.this,"Cannot reach the realm server.");}
        });
    }

    private void showTargets(List<PlayerResponse> targets) {
        int i=0;
        for(PlayerResponse p:targets){
            if (p.getId() == null) continue;
            // The Doctor may protect themselves; other roles cannot target themselves.
            if (p.getId().equals(myId) && !"DOCTOR".equals(role)) continue;
            if(i>=buttons.length) break;
            buttons[i].setText(p.getDisplayName()==null?"Player":p.getDisplayName());
            buttons[i].setTag(p.getId());
            buttons[i].setVisibility(View.VISIBLE);
            i++;
        }
        while(i<buttons.length){buttons[i].setVisibility(View.GONE);i++;}
        if("VILLAGER".equals(role)) return;
        action.setEnabled(true);
        action.setOnClickListener(v->submit());
    }

    private void submit() {
        if(submitting||"VILLAGER".equals(role))return;
        int id=group.getCheckedRadioButtonId();
        if(id==-1){Ui.toast(this,"Choose a living player first.");return;}
        View selected=findViewById(id);
        Object tag=selected==null?null:selected.getTag();
        if(!(tag instanceof Long)){Ui.toast(this,"That target is not valid.");return;}
        Long targetId=(Long)tag;
        submitting=true;           // blocks double taps; stays set after success until we leave
        action.setEnabled(false);
        if("SEER".equals(role)){
            RetrofitClient.getApiService(this).investigate(room,targetId).enqueue(new Callback<InvestigationResponse>(){
                @Override public void onResponse(Call<InvestigationResponse> c, Response<InvestigationResponse> r){
                    if(gone())return;
                    InvestigationResponse body=r.body();
                    if(r.isSuccessful()&&body!=null&&body.getRole()!=null){
                        String result=(body.getPlayerName()==null?"That player":body.getPlayerName())+" is "+Roles.normalize(body.getRole())+".";
                        OnlineSession.saveSeerNote(NightActionActivity.this,OnlineSession.round(NightActionActivity.this),result);
                        OnlineSession.markNightActed(NightActionActivity.this);
                        Ui.toast(NightActionActivity.this,"🔮 "+result);
                        goGame();
                    } else failed(r,"Investigation failed.");
                }
                @Override public void onFailure(Call<InvestigationResponse> c, Throwable t){
                    if(gone())return; submitting=false; action.setEnabled(true);Ui.toast(NightActionActivity.this,"Cannot reach the server. Try again.");
                }
            });
        } else {
            Call<Object> call = "TRAITOR".equals(role)
                    ? RetrofitClient.getApiService(this).attack(room,targetId)
                    : RetrofitClient.getApiService(this).protect(room,targetId);
            call.enqueue(new Callback<Object>(){
                @Override public void onResponse(Call<Object> c, Response<Object> r){
                    if(gone())return;
                    if(r.isSuccessful()){OnlineSession.markNightActed(NightActionActivity.this);goGame();}
                    else failed(r,"Night action was rejected by the server.");
                }
                @Override public void onFailure(Call<Object> c, Throwable t){
                    if(gone())return; submitting=false; action.setEnabled(true);Ui.toast(NightActionActivity.this,"Cannot reach the server. Try again.");
                }
            });
        }
    }

    private void failed(Response<?> r,String fallback){
        String msg=Ui.errorMessage(r,fallback);
        Ui.toast(this,msg);
        // "already acted" / wrong phase: the main screen shows the true state, so go back to it.
        if(r.code()==400){goGame();return;}
        submitting=false; action.setEnabled(true);
    }

    private void goGame(){Intent i=new Intent(this,GameActivity.class);i.putExtra("ROOM_CODE",room);i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);startActivity(i);finish();}
}
