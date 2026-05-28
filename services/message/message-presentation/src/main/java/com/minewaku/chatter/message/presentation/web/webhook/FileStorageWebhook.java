package com.minewaku.chatter.message.presentation.web.webhook;

import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minewaku.chatter.message.application.port.inbound.command.guild.HandleUploadGuildIconNotificationUseCase;
import com.minewaku.chatter.message.application.port.inbound.command.message.HandleUploadAttachmentNotificationUseCase;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.log4j.Log4j2;

@Tag(name = "Upload Image Webhook Notifications", description = "Webhook notifications for image uploads")
@RestController
@RequestMapping("/api/v1/webhooks/cloudinary")
@Log4j2
public class FileStorageWebhook {

	@Qualifier("handleUploadAttachmentNotificationUseCase")
	private final HandleUploadAttachmentNotificationUseCase handleUploadAttachmentNotificationUseCase;
	
	@Qualifier("handleUploadGuildIconNotificationUseCase")
	private final HandleUploadGuildIconNotificationUseCase handleUploadGuildIconNotificationUseCase;

	public FileStorageWebhook(
			HandleUploadAttachmentNotificationUseCase handleUploadAttachmentNotificationUseCase,
			HandleUploadGuildIconNotificationUseCase handleUploadGuildIconNotificationUseCase) {
				
		this.handleUploadAttachmentNotificationUseCase = handleUploadAttachmentNotificationUseCase;
		this.handleUploadGuildIconNotificationUseCase = handleUploadGuildIconNotificationUseCase;
	}

	
	@PostMapping("/guild-icons")
	public ResponseEntity<Void> handleGuildIconUploadNotification(
				@RequestHeader Map<String, String> headers,
				@RequestBody Map<String, Object> body) {

		log.info("Received upload notification with headers: {} and body: {}", headers, body);
		handleUploadGuildIconNotificationUseCase.handle(new HandleUploadGuildIconNotificationUseCase.Command(headers, body));
		return ResponseEntity.ok().build();
	}

	@PostMapping("/attachments")
	public ResponseEntity<Void> handleAttachmentUploadNotification(
				@RequestHeader Map<String, String> headers,
				@RequestBody Map<String, Object> body) {

		log.info("Received upload notification with headers: {} and body: {}", headers, body);
		handleUploadAttachmentNotificationUseCase.handle(new HandleUploadAttachmentNotificationUseCase.Command(headers, body));
		return ResponseEntity.ok().build();
	}
}

