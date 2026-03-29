package com.minewaku.chatter.profile.presentation.web.api.command;

import java.io.IOException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.minewaku.chatter.profile.application.port.inbound.command.profile.command.InputAvatar;
import com.minewaku.chatter.profile.application.port.inbound.command.profile.command.InputBanner;
import com.minewaku.chatter.profile.application.port.inbound.command.profile.usecase.UpdateProfileUseCase;
import com.minewaku.chatter.profile.application.port.inbound.command.profile.usecase.UploadAvatarUseCase;
import com.minewaku.chatter.profile.application.port.inbound.command.profile.usecase.UploadBannerUseCase;
import com.minewaku.chatter.profile.domain.model.profile.model.Bio;
import com.minewaku.chatter.profile.domain.model.profile.model.DisplayName;
import com.minewaku.chatter.profile.domain.model.profile.model.ProfileId;
import com.minewaku.chatter.profile.presentation.web.request.UpdateProfileRequest;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Profile Command", description = "Profile Command API")
@RestController
@RequestMapping("/api/v1/profiles")
public class ProfileCommandController {

	private final UpdateProfileUseCase updateProfileUseCase;
	private final UploadAvatarUseCase uploadAvatarUseCase;
	private final UploadBannerUseCase uploadBannerUseCase;

	public ProfileCommandController(
			UpdateProfileUseCase updateProfileUseCase,
			UploadAvatarUseCase uploadAvatarUseCase,
			UploadBannerUseCase uploadBannerUseCase) {
				
		this.updateProfileUseCase = updateProfileUseCase;
		this.uploadAvatarUseCase = uploadAvatarUseCase;
		this.uploadBannerUseCase = uploadBannerUseCase;
	}


	@PutMapping("/{id}")
	public ResponseEntity<Void> updateProfile(@PathVariable("id") Long id, @RequestBody UpdateProfileRequest request) {
		ProfileId profileId = new ProfileId(id);
		DisplayName	displayName = new DisplayName(request.displayName());
		Bio bio = new Bio(request.bio());
		
		UpdateProfileUseCase.Command command = new UpdateProfileUseCase.Command(
			profileId,
			displayName,
			bio);

		updateProfileUseCase.handle(command);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/{id}/avatar")
	public ResponseEntity<Void> uploadAvatar(@RequestParam("file") MultipartFile file, @PathVariable("id") Long id) {
		try {
			InputAvatar inputAvatar = new InputAvatar(
				file.getOriginalFilename(),
				file.getContentType(),
				file.getSize(),
				file.getInputStream()
			);

			UploadAvatarUseCase.Command command = new UploadAvatarUseCase.Command(
				new ProfileId(id),
				inputAvatar
			);
			
			uploadAvatarUseCase.handle(command);

		} catch(IOException e) {
			throw new RuntimeException("Error during reading file", e);

		}

		return ResponseEntity.ok().build();
	}

	@PostMapping("/{id}/banner")
	public ResponseEntity<Void> uploadBanner(@RequestParam("file") MultipartFile file, @PathVariable("id") Long id) {
		try {
			InputBanner inputBanner = new InputBanner(
				file.getOriginalFilename(),
				file.getContentType(),
				file.getSize(),
				file.getInputStream()
			);

			UploadBannerUseCase.Command command = new UploadBannerUseCase.Command(
				new ProfileId(id),
				inputBanner
			);
			
			uploadBannerUseCase.handle(command);

		} catch(IOException e) {
			throw new RuntimeException("Error during reading file", e);

		}

		return ResponseEntity.ok().build();
	}
}
