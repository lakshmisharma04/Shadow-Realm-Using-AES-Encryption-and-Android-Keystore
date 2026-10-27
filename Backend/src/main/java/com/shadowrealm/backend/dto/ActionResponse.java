package com.shadowrealm.backend.dto;

/** Small, safe acknowledgement returned for night actions and votes. */
public class ActionResponse {
    private String status;
    private String message;

    public ActionResponse() {}
    public ActionResponse(String status, String message) { this.status = status; this.message = message; }

    public String getStatus() { return status; }
    public String getMessage() { return message; }
}
