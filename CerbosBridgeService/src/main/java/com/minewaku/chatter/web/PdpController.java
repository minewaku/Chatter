package com.minewaku.chatter.web;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minewaku.chatter.service.ICerbosService;
import com.minewaku.chatter.service.IPipService;
import com.minewaku.chatter.service.IPipService.PipRequest;
import com.minewaku.chatter.service.IPipService.PipResponse;
import com.minewaku.chatter.util.CerbosAttributeValueUtil;
import com.minewaku.chatter.util.JwtUtil;
import com.minewaku.chatter.web.request.PdpRequest;
import com.minewaku.chatter.web.response.PdpResponse;

import dev.cerbos.sdk.PlanResourcesResult;
import dev.cerbos.sdk.builders.Principal;
import dev.cerbos.sdk.builders.Resource;
import io.swagger.v3.oas.annotations.tags.Tag;


@Tag(name = "Pdp", description = "Policy Desicion Point API")
@RestController
@RequestMapping("pdp/api/v1/")
public class PdpController {

    private final JwtUtil jwtUtil;
    private final ICerbosService cerbosService;
    private final IPipService pipService;
    private final CerbosAttributeValueUtil attributeValueCerbosUtil;

    public PdpController(
        ICerbosService cerbosService,
        JwtUtil jwtUtil,
        IPipService pipService,
        CerbosAttributeValueUtil attributeValueCerbosUtil) {

        this.cerbosService = cerbosService;
        this.jwtUtil = jwtUtil;
        this.pipService = pipService;
        this.attributeValueCerbosUtil = attributeValueCerbosUtil;
    }

    @PostMapping("/resources/check")
    public ResponseEntity<PdpResponse> checkResource(@RequestBody PdpRequest request) {
        JwtAuthenticationToken auth = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = auth.getToken();
        
        String id = jwt.getSubject();
        List<String> roles = jwt.getClaimAsStringList("roles");
        Map<String, Object> customClaims = jwtUtil.getCustomClaims(jwt);

        Principal principal= Principal.newInstance(id, roles.toArray(new String[0]));
        principal = attributeValueCerbosUtil.applyAttributes(principal, customClaims);
        
        Resource resource = Resource.newInstance(request.resourceType(), request.resourceId());
        resource = attributeValueCerbosUtil.applyAttributes(resource, request.resourceAttrs());

        PlanResourcesResult result = cerbosService.planResources(principal, resource, request.action());

        PdpResponse response;

        if (result.isAlwaysAllowed()) {
            response = new PdpResponse(true);
        } else if(result.isAlwaysDenied()) {
            response = new PdpResponse(false);
        } else if(result.isConditional()) {
            Set<String> requiredAttrs = cerbosService.extractConditions(result);
            PipRequest pipRequest = new PipRequest(request.resourceId(), customClaims, requiredAttrs);

            PipResponse pipResponse = pipService.sendPipRequest(pipRequest);

            if(pipResponse.success()) {
                resource = attributeValueCerbosUtil.applyAttributes(resource, pipResponse.conditions());
                PlanResourcesResult resultAfterSuplementAttrs = cerbosService.planResources(principal, resource, request.action());

                if (resultAfterSuplementAttrs.isAlwaysAllowed()) {
                    response = new PdpResponse(true);
                } else if(resultAfterSuplementAttrs.isAlwaysDenied()) {
                    response = new PdpResponse(false);
                } else {
                    response = new PdpResponse(false);
                }
            }

            response = new PdpResponse(false);
        } else {
            response = new PdpResponse(false);
        }

        return ResponseEntity.ok().body(response);
    }

}
