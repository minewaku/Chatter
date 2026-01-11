package com.minewaku.chatter.adapter.web.api.app;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.minewaku.chatter.adapter.annotation.PdpCheck;
import com.minewaku.chatter.adapter.mapper.MultipartFileMapper;
import com.minewaku.chatter.adapter.mapper.UserMapper;
import com.minewaku.chatter.adapter.web.request.UpdateProfileRequest;
import com.minewaku.chatter.adapter.web.response.ProfileDto;
import com.minewaku.chatter.application.service.profile.FindActivatedProfileByUsernameApplicationService;
import com.minewaku.chatter.application.service.profile.UpdateProfileApplicationService;
import com.minewaku.chatter.application.service.profile.UploadAvatarApplicationService;
import com.minewaku.chatter.application.service.profile.UploadBannerApplicationService;
import com.minewaku.chatter.domain.command.profile.UpdateProfileCommand;
import com.minewaku.chatter.domain.command.profile.UploadFileCommand;
import com.minewaku.chatter.domain.model.User;
import com.minewaku.chatter.domain.value.Bio;
import com.minewaku.chatter.domain.value.DisplayName;
import com.minewaku.chatter.domain.value.Username;
import com.minewaku.chatter.domain.value.file.InputImage;
import com.minewaku.chatter.domain.value.id.UserId;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Profiles", description = "Profile API")
@RestController
@RequestMapping("/api/v1/profiles")
public class ProfileController {
    
    private final FindActivatedProfileByUsernameApplicationService findActivatedProfileByUsernameApplicationService;
    private final UpdateProfileApplicationService updateProfileApplicationService;
    private final UploadAvatarApplicationService uploadAvatarApplicationService;
    private final UploadBannerApplicationService uploadBannerApplicationService;
    private final UserMapper userMapper;
    private final MultipartFileMapper multipartFileMapper;

    public ProfileController(
            FindActivatedProfileByUsernameApplicationService findActivatedProfileByUsernameApplicationService,
            UpdateProfileApplicationService updateProfileApplicationService,
            UploadAvatarApplicationService uploadAvatarApplicationService,
            UploadBannerApplicationService uploadBannerApplicationService,
            UserMapper userMapper,
            MultipartFileMapper multipartFileMapper) {

        this.findActivatedProfileByUsernameApplicationService = findActivatedProfileByUsernameApplicationService;
        this.updateProfileApplicationService = updateProfileApplicationService;
        this.uploadAvatarApplicationService = uploadAvatarApplicationService;
        this.uploadBannerApplicationService = uploadBannerApplicationService;
        this.userMapper = userMapper;
        this.multipartFileMapper = multipartFileMapper;
    }   

    @PdpCheck(
        resourceType = "profile", 
        action = "read"
    )
    @GetMapping("/{username}")
    public ResponseEntity<ProfileDto> findActivatedByUsername(@PathVariable String username) {
        Username usernameValue = new Username(username);
        User user = findActivatedProfileByUsernameApplicationService.handle(usernameValue);
        ProfileDto profile = userMapper.domainToProfileDto(user);
        
        return ResponseEntity.ok(profile);
    }

    
    @PdpCheck(
        resourceType = "profile", 
        action = "update"
    )
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateProfile(@RequestBody UpdateProfileRequest request, @PathVariable Long id) {
        UserId userId = new UserId(id);
        DisplayName displayName = new DisplayName(request.displayName());
        Bio bio = new Bio(request.bio());

        UpdateProfileCommand command = new UpdateProfileCommand(
            userId,
            displayName,
            bio
        );  
        updateProfileApplicationService.handle(command);
        
        return ResponseEntity.ok().build();
    }


    @PdpCheck(
        resourceType = "profile", 
        action = "update"
    )
    @PostMapping("/{id}")
    public ResponseEntity<Void> uploadAvatar(@RequestParam("file") MultipartFile file, @PathVariable Long id) {
        UserId userId = new UserId(id);
        InputImage inputImage = multipartFileMapper.toInputImage(file);
        UploadFileCommand command = new UploadFileCommand(
            userId, inputImage
        );  

        uploadAvatarApplicationService.handle(command);
        return ResponseEntity.ok().build();
    }


    @PdpCheck(
        resourceType = "profile", 
        action = "update"
    )
    @PostMapping("/{id}")
    public ResponseEntity<Void> uploadBanner(@RequestParam("file") MultipartFile file, @PathVariable Long id) {
        UserId userId = new UserId(id);
        InputImage inputImage = multipartFileMapper.toInputImage(file);
        UploadFileCommand command = new UploadFileCommand(
            userId, inputImage
        );  

        uploadBannerApplicationService.handle(command);
        return ResponseEntity.ok().build();
    }
}
