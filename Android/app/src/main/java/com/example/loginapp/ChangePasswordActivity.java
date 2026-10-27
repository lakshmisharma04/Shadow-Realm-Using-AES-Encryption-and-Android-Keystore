package com.example.loginapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChangePasswordActivity extends RealmActivity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_change_password);
        setupTopBar("CHANGE PASSWORD", "HARDEN YOUR IDENTITY", "SECURITY");

        EditText current = findViewById(R.id.currentPassword);
        EditText password = findViewById(R.id.newPassword);
        EditText confirm = findViewById(R.id.confirmNewPassword);
        Button save = findViewById(R.id.changePasswordBtn);

        save.setOnClickListener(v -> {
            String oldPassword = current.getText().toString();
            String p = password.getText().toString();
            String c = confirm.getText().toString();
            if (oldPassword.isEmpty()) { current.setError("Enter your current password"); return; }
            if (p.length() < 6) { password.setError("At least 6 characters"); return; }
            if (p.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
                password.setError("Use no more than 72 UTF-8 bytes");
                return;
            }
            if (!p.equals(c)) { confirm.setError("Passwords do not match"); return; }
            save.setEnabled(false);
            RetrofitClient.getApiService(this).changePassword(new ChangePasswordRequest(oldPassword, p))
                    .enqueue(new Callback<LoginResponse>() {
                        @Override public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                            save.setEnabled(true);
                            if (response.isSuccessful() && response.body() != null
                                    && SecureStore.saveToken(ChangePasswordActivity.this, response.body().getToken())) {
                                Ui.toast(ChangePasswordActivity.this, "Password updated. Other signed-in sessions have been revoked.");
                                finish();
                            } else if (response.isSuccessful()) {
                                Ui.toast(ChangePasswordActivity.this, "Password changed. Please sign in again.");
                                logout();
                            } else {
                                Ui.toast(ChangePasswordActivity.this,
                                        Ui.errorMessage(response, "Password could not be changed."));
                            }
                        }

                        @Override public void onFailure(Call<LoginResponse> call, Throwable t) {
                            save.setEnabled(true);
                            Ui.toast(ChangePasswordActivity.this, "Cannot reach the realm server.");
                        }
                    });
        });
    }
}
