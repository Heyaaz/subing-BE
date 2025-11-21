package com.project.subing.domain.common;

public enum BillingCycle {
    MONTHLY("월간"),
    YEARLY("연간");
    
    private final String description;
    
    BillingCycle(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
