package com.minewaku.chatter.service;

import java.util.Set;

import dev.cerbos.sdk.CheckResourcesResult;
import dev.cerbos.sdk.CheckResult;
import dev.cerbos.sdk.PlanResourcesResult;
import dev.cerbos.sdk.builders.Principal;
import dev.cerbos.sdk.builders.Resource;
import dev.cerbos.sdk.builders.ResourceAction;

public interface ICerbosService {
    CheckResult checkResult(Principal principal, Resource resource, String[] actions);
    CheckResourcesResult checkResultByBatch(Principal principal, ResourceAction[] resourceActions);
	PlanResourcesResult planResources(Principal principal, Resource resource, String action);
    Set<String> extractConditions(PlanResourcesResult planResourcesResult);
}
