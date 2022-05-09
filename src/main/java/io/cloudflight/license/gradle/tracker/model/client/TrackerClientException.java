package io.cloudflight.license.gradle.tracker.model.client;

public class TrackerClientException extends RuntimeException {

    public TrackerClientException(String message, Exception cause) {
        super(message, cause);
    }

    public TrackerClientException(String message) {
        super(message);
    }
}