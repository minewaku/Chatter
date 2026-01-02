package com.minewaku.chatter.adapter.web.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import com.minewaku.chatter.adapter.annotation.Attribute;
import com.minewaku.chatter.adapter.annotation.PdpCheck;
import com.minewaku.chatter.adapter.db.postgresql.JpaUserRepository;
import com.minewaku.chatter.adapter.entity.JpaUserEntity;
import com.minewaku.chatter.adapter.mapper.UserMapper;
import com.minewaku.chatter.adapter.web.request.AssignRoleRequest;
import com.minewaku.chatter.adapter.web.response.UserDto;
import com.minewaku.chatter.application.service.user.HardDeleteUserApplicationService;
import com.minewaku.chatter.application.service.user.LockUserApplicationService;
import com.minewaku.chatter.application.service.user.RestoreUserApplicationService;
import com.minewaku.chatter.application.service.user.SoftDeleteUserApplicationService;
import com.minewaku.chatter.application.service.user.UnlockUserApplicationService;
import com.minewaku.chatter.application.service.user_role.AssignRoleApplicationService;
import com.minewaku.chatter.application.service.user_role.UnassignRoleApplicationService;
import com.minewaku.chatter.domain.command.user_role.CreateUserRoleCommand;
import com.minewaku.chatter.domain.value.id.RoleId;
import com.minewaku.chatter.domain.value.id.UserId;
import com.minewaku.chatter.domain.value.id.UserRoleId;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Users", description = "User API")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final LockUserApplicationService lockUserApplicationService;
    private final UnlockUserApplicationService unlockUserApplicationService;
    private final SoftDeleteUserApplicationService softDeleteUserApplicationService;
    private final RestoreUserApplicationService restoreUserApplicationService;
    private final HardDeleteUserApplicationService hardDeleteUserApplicationService;

    private final AssignRoleApplicationService assignRoleApplicationService;
    private final UnassignRoleApplicationService unassignRoleApplicationService;

    private final JpaUserRepository jpaUserRepository;
    private final UserMapper userMapper;

    public UserController(
            LockUserApplicationService lockUserApplicationService,
            UnlockUserApplicationService unlockUserApplicationService,
            SoftDeleteUserApplicationService softDeleteUserApplicationService,
            RestoreUserApplicationService restoreUserApplicationService,
            HardDeleteUserApplicationService hardDeleteUserApplicationService,

            AssignRoleApplicationService assignRoleApplicationService,
            UnassignRoleApplicationService unassignRoleApplicationService,

            UserMapper userMapper,
            JpaUserRepository jpaUserRepository) {

        this.lockUserApplicationService = lockUserApplicationService;
        this.unlockUserApplicationService = unlockUserApplicationService;
        this.softDeleteUserApplicationService = softDeleteUserApplicationService;
        this.restoreUserApplicationService = restoreUserApplicationService;
        this.hardDeleteUserApplicationService = hardDeleteUserApplicationService;

        this.assignRoleApplicationService = assignRoleApplicationService;
        this.unassignRoleApplicationService = unassignRoleApplicationService;

        this.userMapper = userMapper;
        this.jpaUserRepository = jpaUserRepository;
    }


    @PdpCheck(
        resourceType = "user", 
        action = "list"
    )
    @GetMapping("")
    public ResponseEntity<Page<UserDto>> findAll(Pageable pageable) {
        Page<JpaUserEntity> users = jpaUserRepository.findAll(pageable);
        Page<UserDto> userDtos = users.map(userMapper::entityToDto);
        return ResponseEntity.ok(userDtos);
    }


    @PdpCheck(
        resourceType = "user", 
        action = "lock"
    )
    @PutMapping("/{id}/lock")
    public ResponseEntity<Void> lock(@PathVariable Long id) {
        UserId userId = new UserId(id);
        lockUserApplicationService.handle(userId);
        return ResponseEntity.ok().build();
    }


    @PdpCheck(
        resourceType = "user", 
        action = "unlock"
    )
    @PutMapping("/{id}/unlock")
    public ResponseEntity<Void> unlock(@PathVariable Long id) {
        UserId userId = new UserId(id);
        unlockUserApplicationService.handle(userId);
        return ResponseEntity.ok().build();
    }


    @PdpCheck(
        resourceType = "user", 
        resourceIdParam = "#id", 
        action = "delete", 
        resourceAttrs = 
            @Attribute(key = "permanent", value = "#permanent"
        )
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDeleteUser(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean permanent
    ) {
        if(permanent) {
            UserId userId = new UserId(id);
            hardDeleteUserApplicationService.handle(userId);
            return ResponseEntity.ok().build();
        }

        UserId userId = new UserId(id);
        softDeleteUserApplicationService.handle(userId);
        return ResponseEntity.ok().build();
    }


    @PdpCheck(
        resourceType = "user", 
        action = "restore"
    )
    @PostMapping("/{id}/restore")
    public ResponseEntity<Void> restoreUser(@PathVariable Long id) {
        UserId userId = new UserId(id);
        restoreUserApplicationService.handle(userId);
        return ResponseEntity.ok().build();
    }


    @PdpCheck(
        resourceType = "user-role", 
        resourceIdParam = "#request.roleId", 
        action = "create"
    )
    @PostMapping("/{userId}/roles")
    public void assignRoleToUser(
        @PathVariable Long userId, 
        @RequestBody AssignRoleRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        String createdByIdString = jwt.getSubject(); 
        Long createdById = Long.valueOf(createdByIdString);

        CreateUserRoleCommand command = new CreateUserRoleCommand(
                new UserId(userId),
                new RoleId(request.roleId()),
                new UserId(createdById));
        assignRoleApplicationService.handle(command);
    }


    @PdpCheck(
        resourceType = "user-role", 
        resourceIdParam = "#roleId", 
        action = "delete"
    )
    @DeleteMapping("/{userId}/roles/{roleId}")
    public void unassignRoleFromUser(
            @PathVariable Long userId, 
            @PathVariable Long roleId) {
        UserRoleId userRoleId = new UserRoleId(
                new UserId(userId),
                new RoleId(roleId));
        unassignRoleApplicationService.handle(userRoleId);
    }
}