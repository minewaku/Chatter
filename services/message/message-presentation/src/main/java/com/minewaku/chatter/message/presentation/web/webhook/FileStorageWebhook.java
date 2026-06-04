package com.minewaku.chatter.message.presentation.web.webhook;

import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minewaku.chatter.message.application.port.inbound.command.file.HandleUploadNotificationUseCase;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Upload Image Webhook Notifications", description = "Webhook notifications for image uploads")
@RestController
@RequestMapping("/api/v1/webhooks/cloudinary")
@Slf4j
@AllArgsConstructor
public class FileStorageWebhook {

	@Qualifier("handleUploadAttachmentNotificationUseCase")
	private final HandleUploadNotificationUseCase handleUploadAttachmentNotificationUseCase;
	
	@Qualifier("handleUploadGuildIconNotificationUseCase")
	private final HandleUploadNotificationUseCase handleUploadGuildIconNotificationUseCase;
	
	@PostMapping("/guild-icons")
	public ResponseEntity<Void> handleGuildIconUploadNotification(
				@RequestHeader Map<String, String> headers,
				@RequestBody Map<String, Object> body) {

		log.info("Received upload notification with headers: {} and body: {}", headers, body);
		handleUploadGuildIconNotificationUseCase.handle(new HandleUploadNotificationUseCase.Command(headers, body));
		return ResponseEntity.ok().build();
	}

	@PostMapping("/attachments")
	public ResponseEntity<Void> handleAttachmentUploadNotification(
				@RequestHeader Map<String, String> headers,
				@RequestBody Map<String, Object> body) {

		log.info("Received upload notification with headers: {} and body: {}", headers, body);
		handleUploadAttachmentNotificationUseCase.handle(new HandleUploadNotificationUseCase.Command(headers, body));
		return ResponseEntity.ok().build();
	}
}

