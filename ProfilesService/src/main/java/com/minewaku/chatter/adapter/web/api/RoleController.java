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

import com.minewaku.chatter.adapter.annotation.PdpCheck;
import com.minewaku.chatter.adapter.db.postgresql.JpaRoleRepository;
import com.minewaku.chatter.adapter.entity.JpaRoleEntity;
import com.minewaku.chatter.adapter.mapper.RoleMapper;
import com.minewaku.chatter.adapter.web.request.CreateRoleRequest;
import com.minewaku.chatter.adapter.web.request.UpdateRoleRequest;
import com.minewaku.chatter.adapter.web.response.RoleDto;
import com.minewaku.chatter.application.service.role.CreateRoleApplicationService;
import com.minewaku.chatter.application.service.role.HardDeleteRoleApplicationService;
import com.minewaku.chatter.application.service.role.RestoreRoleApplicationService;
import com.minewaku.chatter.application.service.role.SoftDeleteRoleApplicationService;
import com.minewaku.chatter.application.service.role.UpdateRoleApplicationService;
import com.minewaku.chatter.domain.command.role.CreateRoleCommand;
import com.minewaku.chatter.domain.command.role.UpdateRoleCommand;
import com.minewaku.chatter.domain.value.Code;
import com.minewaku.chatter.domain.value.id.RoleId;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Roles", description = "Role API")
@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {
    
    private final CreateRoleApplicationService createRoleApplicationService;
    private final UpdateRoleApplicationService updateRoleApplicationService;
    private final SoftDeleteRoleApplicationService softDeleteRoleApplicationService;
    private final RestoreRoleApplicationService restoreRoleApplicationService;
    private final HardDeleteRoleApplicationService hardDeleteRoleApplicationService;

    private final RoleMapper RoleMapper;
    private final JpaRoleRepository jpaRoleRepository;

    public RoleController(
        CreateRoleApplicationService createRoleApplicationService,
        UpdateRoleApplicationService updateRoleApplicationService,
        SoftDeleteRoleApplicationService softDeleteRoleApplicationService,
        RestoreRoleApplicationService restoreRoleApplicationService,
        HardDeleteRoleApplicationService hardDeleteRoleApplicationService,

        JpaRoleRepository jpaRoleRepository,
        RoleMapper RoleMapper
    ) {

        this.createRoleApplicationService = createRoleApplicationService;
        this.updateRoleApplicationService = updateRoleApplicationService;
        this.softDeleteRoleApplicationService = softDeleteRoleApplicationService;
        this.restoreRoleApplicationService = restoreRoleApplicationService;
        this.hardDeleteRoleApplicationService = hardDeleteRoleApplicationService;

        this.jpaRoleRepository = jpaRoleRepository;
        this.RoleMapper = RoleMapper;
    }


    @PdpCheck(
        resourceType = "role",
        action = "list"
    )
    @GetMapping("")
    public ResponseEntity<Page<RoleDto>> findAll(Pageable pageable) {
        Page<JpaRoleEntity> Roles = jpaRoleRepository.findAll(pageable);
        Page<RoleDto> RoleDtos = Roles.map(RoleMapper::entityToDto);
        return ResponseEntity.ok(RoleDtos);
	}

    @PdpCheck(
        resourceType = "role",
        action = "create"
    )
    @PostMapping("")
    public ResponseEntity<RoleDto> createRole(@RequestBody CreateRoleRequest request) {
        CreateRoleCommand command = new CreateRoleCommand(
            request.name(),
            new Code(request.code()),
            request.description()
        );
        createRoleApplicationService.handle(command);
        return ResponseEntity.ok().build();
    }


    @PdpCheck(
        resourceType = "role",
        resourceIdParam = "#id",
        action = "update"
    )
    @PutMapping("/{id}")
    public ResponseEntity<RoleDto> updateRole(@PathVariable Long id, @RequestBody UpdateRoleRequest request) {
        UpdateRoleCommand command = new UpdateRoleCommand(
            new RoleId(id),
            request.name(),
            request.description()
        );
        updateRoleApplicationService.handle(command);
        return ResponseEntity.ok().build();
    }


    @PdpCheck(
        resourceType = "role",
        resourceIdParam = "#id",
        action = "soft-delete"
    )
    @PutMapping("/{id}/deleted")
    public ResponseEntity<Void> softDeleteRole(@PathVariable Long id) {
        RoleId RoleId = new RoleId(id);
        softDeleteRoleApplicationService.handle(RoleId);
        return ResponseEntity.ok().build();
    }


    @PdpCheck(
        resourceType = "role",
        resourceIdParam = "#id",
        action = "restore"
    )
    @PutMapping("/{id}/restored")
    public ResponseEntity<Void> restoreRole(
        @PathVariable Long id,
        @RequestParam(defaultValue = "false") boolean permanent
    ) {
        RoleId RoleId = new RoleId(id);
        restoreRoleApplicationService.handle(RoleId);
        return ResponseEntity.ok().build();
    }


    @PdpCheck(
        resourceType = "role",
        resourceIdParam = "#id",
        action = "delete"
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> hardDeleteRole(@PathVariable Long id) {
        RoleId RoleId = new RoleId(id);
        hardDeleteRoleApplicationService.handle(RoleId);
        return ResponseEntity.ok().build();
    }
}
