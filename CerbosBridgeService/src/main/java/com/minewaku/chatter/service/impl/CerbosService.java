package com.minewaku.chatter.service.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.util.JsonFormat;
import com.minewaku.chatter.service.ICerbosService;

import dev.cerbos.sdk.CerbosBlockingClient;
import dev.cerbos.sdk.CheckResourcesResult;
import dev.cerbos.sdk.CheckResult;
import dev.cerbos.sdk.PlanResourcesResult;
import dev.cerbos.sdk.builders.Principal;
import dev.cerbos.sdk.builders.Resource;
import dev.cerbos.sdk.builders.ResourceAction;

@Service
public class CerbosService implements ICerbosService {

    private final CerbosBlockingClient cerbosBlockingClient;
    private final ObjectMapper objectMapper;

    public CerbosService(
            CerbosBlockingClient cerbosBlockingClient,
            ObjectMapper objectMapper) {

        this.cerbosBlockingClient = cerbosBlockingClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public CheckResult checkResult(
                Principal principal, 
                Resource resource,
                String[] actions
        ) {

            CheckResult result = cerbosBlockingClient.check(
                principal,
                resource,
                actions);

        return result;
    }

    public CheckResourcesResult checkResultByBatch(
                Principal principal, 
                ResourceAction[] resourceActions
        ) {

            CheckResourcesResult result = cerbosBlockingClient.batch(principal).check();

        return result;
    }

    public PlanResourcesResult planResources(
                Principal principal, 
                Resource resource, 
                String action) {
                    
        PlanResourcesResult result = cerbosBlockingClient.plan(principal, resource, action);
        return result;
    }

    public Set<String> extractConditions(PlanResourcesResult planResourcesResult) {
        try {
            JsonNode conditions = protoTextFormatConditionstoJsonNode(planResourcesResult);
            if (conditions == null) {
                return Collections.emptySet();
            }
            return traversalConditionTreeRecursive(conditions, new HashSet<>());
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse plan conditions: " + e.getMessage(), e);
        }
    }

    private Set<String> traversalConditionTreeRecursive(JsonNode currentNode, Set<String> conditions) {
        if (currentNode == null || currentNode.isMissingNode()) {
            return conditions;
        }

        JsonNode expression = currentNode.get("expression");
        if (expression == null || expression.isMissingNode()) {
            return conditions;
        }

        JsonNode operands = expression.get("operands");
        if (operands != null && operands.isArray()) {
            for (JsonNode operand : operands) {
                if (operand.has("expression")) {
                    traversalConditionTreeRecursive(operand, conditions);
                } else if (operand.has("variable")) {
                    conditions.add(operand.get("variable").asText());
                }
            }
        }

        return conditions;
    }

    private JsonNode protoTextFormatConditionstoJsonNode(PlanResourcesResult planResourcesResult) throws IOException {
        String json = JsonFormat.printer().print(planResourcesResult.getRaw());
        JsonNode jsonNode = objectMapper.readTree(json);

        JsonNode filter = jsonNode.get("filter");
        if (filter == null || filter.isMissingNode()) {
            return null;
        }

        JsonNode condition = filter.get("condition");
        if (condition == null || condition.isMissingNode()) {
            return null;
        }

        System.out.println(condition.toPrettyString());
        return condition;
    }
}
