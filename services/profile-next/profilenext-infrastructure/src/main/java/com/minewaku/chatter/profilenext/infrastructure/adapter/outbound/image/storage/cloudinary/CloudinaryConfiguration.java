package com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.image.storage.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.SignatureAlgorithm;
import com.cloudinary.utils.ObjectUtils;
import com.minewaku.chatter.profilenext.profile.internal.application.command.ImagePolicy;
import com.minewaku.chatter.profilenext.profile.internal.application.command.RemovalPolicy;
import com.minewaku.chatter.profilenext.profile.internal.application.command.SweepPolicy;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Builds the SDK client and the plain-record policies core's services are constructed with. */
@Configuration
public class CloudinaryConfiguration {

    @Bean
    Cloudinary cloudinary(ImageProperties properties) {
        Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", properties.cloudName(),
                "api_key", properties.apiKey(),
                "api_secret", properties.apiSecret(),
                "secure", true));
        cloudinary.config.signatureAlgorithm =
                properties.signatureAlgorithm().equals("SHA-256") ? SignatureAlgorithm.SHA256 : SignatureAlgorithm.SHA1;
        return cloudinary;
    }

    @Bean
    CloudinaryClient cloudinaryClient(Cloudinary cloudinary) {
        return new SdkCloudinaryClient(cloudinary);
    }

    @Bean
    ImagePolicy imagePolicy(ImageProperties properties) {
        Set<String> formats = properties.allowedFormats().stream()
                .map(format -> format.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        return new ImagePolicy(
                properties.maxBytes().avatar().toBytes(),
                properties.maxBytes().banner().toBytes(),
                formats,
                properties.permissionLifetime(),
                properties.webhookTolerance());
    }

    @Bean
    RemovalPolicy removalPolicy(ImageProperties properties) {
        ImageProperties.Removal removal = properties.removal();
        return new RemovalPolicy(removal.maxRetries(), removal.delays(), removal.pollBatchSize(), removal.lease());
    }

    @Bean
    SweepPolicy sweepPolicy(ImageProperties properties) {
        return new SweepPolicy(properties.sweep().gracePeriod());
    }
}
