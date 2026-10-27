package com.example.loginapp;

import android.content.Context;
import android.widget.Toast;

import retrofit2.Response;

public final class Ui {
    private Ui() {}
    public static void toast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Best human-readable message for a failed (non-2xx) response. The backend sends either a
     * plain-text reason or a JSON object with "message"; anything else falls back to the given text.
     */
    public static String errorMessage(Response<?> response, String fallback) {
        try {
            if (response != null && response.errorBody() != null) {
                String raw = response.errorBody().string();
                if (raw != null) {
                    raw = raw.trim();
                    if (raw.startsWith("{")) {
                        org.json.JSONObject o = new org.json.JSONObject(raw);
                        String m = o.optString("message", o.optString("error", ""));
                        if (!m.isEmpty()) raw = m; else raw = "";
                    }
                    if (!raw.isEmpty() && raw.length() <= 160) return raw;
                }
            }
        } catch (Exception ignored) {
            // fall through to the fallback text
        }
        return fallback;
    }
}
