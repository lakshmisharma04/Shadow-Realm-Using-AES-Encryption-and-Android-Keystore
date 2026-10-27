package com.example.loginapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class OtpActivity extends RealmActivity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_otp);
        setupTopBar("IDENTITY CHECK", "ONE LAST STEP BEFORE THE GATES", "MFA");

        EditText otp = findViewById(R.id.otp);
        Button verify = findViewById(R.id.verifyOtp);
        TextView resend = findViewById(R.id.resendOtp);

        otp.setVisibility(android.view.View.GONE);
        resend.setVisibility(android.view.View.GONE);
        findViewById(R.id.otpInstructions).setVisibility(android.view.View.VISIBLE);
        verify.setText("RETURN TO SIGN IN");
        verify.setOnClickListener(v -> {
            Ui.toast(this, "The backend does not provide OTP verification. Sign in with your registered password.");
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
}
