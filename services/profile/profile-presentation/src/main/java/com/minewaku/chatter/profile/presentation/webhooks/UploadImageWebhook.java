package com.minewaku.chatter.profile.presentation.webhooks;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minewaku.chatter.profile.application.port.inbound.command.profile.usecase.GenerateUploadSignatureUseCase;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.log4j.Log4j2;

@Tag(name = "Upload Image Webhook Notifications", description = "Webhook notifications for image uploads")
@RestController
@RequestMapping("/webhooks/upload")
@Log4j2
public class UploadImageWebhook {

	private final GenerateUploadSignatureUseCase generateUploadSignatureUseCase;

	public UploadImageWebhook(
			GenerateUploadSignatureUseCase generateUploadSignatureUseCase) {
				
		this.generateUploadSignatureUseCase = generateUploadSignatureUseCase;
	}

	
	@PutMapping("/temp")
	public ResponseEntity<Void> generateSignature(
				@RequestBody Map<String, Object> map) {

		log.info("Received the fuking webhook notification! {}", map);
		return ResponseEntity.ok().build();
	}
}
