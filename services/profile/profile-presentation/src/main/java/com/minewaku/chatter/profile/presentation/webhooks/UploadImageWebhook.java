package com.minewaku.chatter.profile.presentation.webhooks;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minewaku.chatter.profile.application.port.inbound.command.file.usecase.HandleUploadNotificationUseCase;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.log4j.Log4j2;

@Tag(name = "Upload Image Webhook Notifications", description = "Webhook notifications for image uploads")
@RestController
@RequestMapping("/api/v1/profiles/webhooks/upload")
@Log4j2
public class UploadImageWebhook {

	private final HandleUploadNotificationUseCase handleUploadNotificationUseCase;

	public UploadImageWebhook(
			HandleUploadNotificationUseCase handleUploadNotificationUseCase) {
				
		this.handleUploadNotificationUseCase = handleUploadNotificationUseCase;
	}

	
	@PostMapping("/temp")
	public ResponseEntity<Void> generateSignature(
				@RequestHeader Map<String, String> headers,
				@RequestBody Map<String, Object> body) {

		log.info("Received upload notification with headers: {} and body: {}", headers, body);
		handleUploadNotificationUseCase.handle(new HandleUploadNotificationUseCase.Command(headers, body));
		return ResponseEntity.ok().build();
	}
}
