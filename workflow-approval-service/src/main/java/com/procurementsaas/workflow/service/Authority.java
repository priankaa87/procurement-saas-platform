package com.procurementsaas.workflow.service;

/**
 * How an actor came to be allowed to act on a step.
 *
 * @param roleCode   the role the step required
 * @param onBehalfOf the person whose authority was borrowed, or null if the actor holds
 *                   the role themselves
 */
public record Authority(String roleCode, String onBehalfOf) {

    public static Authority ownRole(String roleCode) {
        return new Authority(roleCode, null);
    }

    public static Authority delegatedFrom(String roleCode, String delegator) {
        return new Authority(roleCode, delegator);
    }

    public boolean isDelegated() {
        return onBehalfOf != null;
    }
}
