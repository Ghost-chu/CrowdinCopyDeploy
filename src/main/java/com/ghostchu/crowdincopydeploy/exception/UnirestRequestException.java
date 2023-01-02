package com.ghostchu.crowdincopydeploy.exception;

import kong.unirest.HttpResponse;

public class UnirestRequestException extends RuntimeException {
    public UnirestRequestException(String stuff, HttpResponse<?> response) {
        super("Failed to request on action `" + stuff + "`. HTTP Code: " + response.getStatus() + " Response: " + response.getBody().toString());
    }
}
