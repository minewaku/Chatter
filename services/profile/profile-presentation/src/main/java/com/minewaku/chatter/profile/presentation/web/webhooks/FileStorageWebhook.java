package com.minewaku.chatter.profile.presentation.web.webhooks;

import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minewaku.chatter.profile.application.port.inbound.command.asset.usecase.HandleUploadNotificationUseCase;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Upload Image Webhook Notifications", description = "Webhook notifications for image uploads")
@RestController
@RequestMapping("/api/v1/webhooks/cloudinary")
@Slf4j
@AllArgsConstructor
public class FileStorageWebhook {

	@Qualifier("handleUploadAvatarNotificationUseCase")
	private final HandleUploadNotificationUseCase handleUploadAvatarNotificationUseCase;
	
	@Qualifier("handleUploadBannerNotificationUseCase")
	private final HandleUploadNotificationUseCase handleUploadBannerNotificationUseCase;

	
	@PostMapping("/avatars")
	public ResponseEntity<Void> generateAvatarSignature(
				@RequestHeader Map<String, String> headers,
				@RequestBody Map<String, Object> body) {

		log.info("Received upload notification with headers: {} and body: {}", headers, body);
		handleUploadAvatarNotificationUseCase.handle(new HandleUploadNotificationUseCase.Command(headers, body));
		return ResponseEntity.ok().build();
	}

	@PostMapping("/banners")
	public ResponseEntity<Void> generateBannerSignature(
				@RequestHeader Map<String, String> headers,
				@RequestBody Map<String, Object> body) {

		log.info("Received upload notification with headers: {} and body: {}", headers, body);
		handleUploadBannerNotificationUseCase.handle(new HandleUploadNotificationUseCase.Command(headers, body));
		return ResponseEntity.ok().build();
	}
}
