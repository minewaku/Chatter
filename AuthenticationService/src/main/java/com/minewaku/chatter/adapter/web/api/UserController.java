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
import org.springframework.web.bind.annotation.RestController;

import com.minewaku.chatter.adapter.annotation.PdpCheck;
import com.minewaku.chatter.adapter.db.mysql.JpaUserRepository;
import com.minewaku.chatter.adapter.entity.JpaUserEntity;
import com.minewaku.chatter.adapter.mapper.UserMapper;
import com.minewaku.chatter.adapter.web.request.AssignRoleRequest;
import com.minewaku.chatter.adapter.web.response.UserDTO;
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


@Tag(name = "Authentication", description = "Authentication API")
@RestController
@RequestMapping("auth-service/api/v1/users/")
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
        JpaUserRepository jpaUserRepository
    ) {

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
    public ResponseEntity<Page<UserDTO>> findAll(Pageable pageable) {
        Page<JpaUserEntity> users = jpaUserRepository.findAll(pageable);
        Page<UserDTO> userDtos = users.map(userMapper::entityToDto);
        return ResponseEntity.ok(userDtos);
	}
    

    @PdpCheck(
        resourceType = "user",
        action = "lock"
    )
    @PutMapping("{ids}/locked")
    public ResponseEntity<Void> lock(@PathVariable Long id) {
    	UserId userId = new UserId(id);
		lockUserApplicationService.handle(userId);
		return ResponseEntity.ok().build();
	}


    @PdpCheck(
        resourceType = "user",
        action = "unlock"
    )
    @DeleteMapping("{ids}/locked")
    public ResponseEntity<Void> unlock(@PathVariable Long id) {
    	UserId userId = new UserId(id);
		unlockUserApplicationService.handle(userId);
		return ResponseEntity.ok().build();
	}


    @PdpCheck(
        resourceType = "user",
        action = "soft-delete"
    )
    @PutMapping("{id}/deleted")
    public ResponseEntity<Void> softDeleteUser(@PathVariable Long id) {
        UserId userId = new UserId(id);
        softDeleteUserApplicationService.handle(userId);
        return ResponseEntity.ok().build();
    }


    @PdpCheck(
        resourceType = "user",
        action = "restore"
    )
    @DeleteMapping("{id}/deleted")
    public ResponseEntity<Void> restoreUser(@PathVariable Long id) {
        UserId userId = new UserId(id);
        restoreUserApplicationService.handle(userId);
        return ResponseEntity.ok().build();
    }


    @PdpCheck(
        resourceType = "user",
        resourceIdParam = "#id",
        action = "delete"
    )
    @DeleteMapping("{id}")
    public ResponseEntity<Void> hardDeleteUser(@PathVariable Long id) {
        UserId userId = new UserId(id);
        hardDeleteUserApplicationService.handle(userId);
        return ResponseEntity.ok().build();
    }


    @PdpCheck(
        resourceType = "user",
        resourceIdParam = "#userId",
        action = "assign_role"
    )
    @PostMapping("{userId}/roles")
    public void assignRoleToUser(@PathVariable Long userId, @RequestBody AssignRoleRequest request) {
        CreateUserRoleCommand command = new CreateUserRoleCommand(
            new UserId(userId),
            new RoleId(request.roleId()),
            request.createdBy() != null ? new UserId(request.createdBy()) : null
        );
        assignRoleApplicationService.handle(command);
    }


    @PdpCheck(
        resourceType = "user",
        resourceIdParam = "#userId",
        action = "delete"
    )
    @DeleteMapping("{userId}/roles/{roleId}")
    public void unassignRoleFromUser(@PathVariable Long userId, @PathVariable Long roleId) {
        UserRoleId userRoleId = new UserRoleId(
            new UserId(userId),
            new RoleId(roleId)
        );
        unassignRoleApplicationService.handle(userRoleId);
    }
}