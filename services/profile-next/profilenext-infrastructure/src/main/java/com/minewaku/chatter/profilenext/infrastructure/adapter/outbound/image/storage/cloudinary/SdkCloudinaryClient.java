package com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.image.storage.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.api.exceptions.NotFound;
import com.cloudinary.utils.ObjectUtils;
import com.minewaku.chatter.profilenext.profile.api.exception.ImageStorageUnavailableException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * The SDK-backed client. Provider errors are translated and the SDK's own messages (which may echo requests, and so
 * credentials) are never passed on. Header reads go to the public delivery address of the original, which needs no
 * credentials and no Admin API quota.
 */
public class SdkCloudinaryClient implements CloudinaryClient {

    private final Cloudinary cloudinary;
    private final HttpClient http;
    private final String deliveryUrl;
    private final Duration callTimeout;

    public SdkCloudinaryClient(Cloudinary cloudinary, String deliveryUrl, Duration callTimeout) {
        this.cloudinary = cloudinary;
        this.deliveryUrl = deliveryUrl;
        this.callTimeout = callTimeout;
        this.cloudinary.config.timeout = (int) Math.max(1, callTimeout.toSeconds());
        this.http = HttpClient.newBuilder()
                .connectTimeout(callTimeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public String sign(Map<String, Object> params) {
        try {
            return cloudinary.apiSignRequest(params, cloudinary.config.apiSecret, cloudinary.config.signatureVersion);
        } catch (RuntimeException e) {
            throw new ImageStorageUnavailableException("the upload could not be signed");
        }
    }

    @Override
    public Optional<Asset> asset(String publicId) {
        Map<?, ?> response;
        try {
            response = cloudinary.api().resource(publicId, ObjectUtils.asMap("resource_type", "image", "type", "upload"));
        } catch (NotFound e) {
            return Optional.empty();
        } catch (Exception e) {
            throw new ImageStorageUnavailableException("the image lookup did not reach the provider");
        }
        Object bytes = response == null ? null : response.get("bytes");
        Object format = response == null ? null : response.get("format");
        if (!(bytes instanceof Number number) || format == null) {
            throw new ImageStorageUnavailableException("the provider answered with an unexpected shape");
        }
        return Optional.of(new Asset(number.longValue(), format.toString()));
    }

    @Override
    public byte[] readLeadingBytes(String publicId, int maxBytes) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(deliveryUrl + "/" + publicId))
                .timeout(callTimeout)
                .header("Range", "bytes=0-" + (maxBytes - 1L))
                .GET()
                .build();
        try {
            HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                // a CDN that ignores Range answers 200 with the whole file; reading only what is needed and closing
                // the stream drops the rest
                if (response.statusCode() != 200 && response.statusCode() != 206) {
                    throw new ImageStorageUnavailableException("the image could not be read from the provider");
                }
                return body.readNBytes(maxBytes);
            }
        } catch (IOException e) {
            throw new ImageStorageUnavailableException("the image could not be read from the provider");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ImageStorageUnavailableException("the image read was interrupted");
        }
    }

    @Override
    public void destroy(String publicId) {
        Map<?, ?> result;
        try {
            result = cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image", "invalidate", true));
        } catch (IOException | RuntimeException e) {
            throw new ImageStorageUnavailableException("image removal did not reach the provider");
        }
        Object outcome = result == null ? null : result.get("result");
        if (!"ok".equals(outcome) && !"not found".equals(outcome)) {
            throw new ImageStorageUnavailableException("the provider did not confirm the removal");
        }
    }
}
