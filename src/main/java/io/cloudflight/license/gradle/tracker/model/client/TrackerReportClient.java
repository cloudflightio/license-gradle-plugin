package io.cloudflight.license.gradle.tracker.model.client;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.Asserts;

import java.io.File;
import java.io.IOException;

public class TrackerReportClient {

    private final String trackerUrl;
    private final String authorizationHeader;

    private TrackerReportClient(String trackerUrl, String authorizationHeader) {
        Asserts.check(trackerUrl != null, "trackerUrl must not be null");
        this.trackerUrl = trackerUrl;
        this.authorizationHeader = authorizationHeader;
    }

    public static TrackerReportClient getInstance(String trackerUrl, String authorizationHeader) {
        return new TrackerReportClient(trackerUrl, authorizationHeader);
    }

    public void sendReport(File dependencies) {
        try {
            HttpClient client = HttpClients.createDefault();
            HttpPost post = new HttpPost(trackerUrl + "/report/upload");

            post.setEntity(new FileEntity(dependencies, ContentType.APPLICATION_JSON));
            if (authorizationHeader != null && authorizationHeader.length() != 0) {
                post.addHeader("Authorization", authorizationHeader);
            }

            HttpResponse response = client.execute(post);

            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new TrackerClientException("Tracker responded with " + response.getStatusLine().getStatusCode() + ": " + response.getStatusLine().getReasonPhrase());
            }
        } catch (IOException e) {
            throw new TrackerClientException("unknown exception: " + e.getMessage(), e);
        }
    }
}
