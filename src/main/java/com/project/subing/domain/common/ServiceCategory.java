package com.project.subing.domain.common;

public enum ServiceCategory {
    OTT("OTT"),
    LIFE("생활"),
    AI("AI"),
    MUSIC("음악"),
    CLOUD("클라우드"),
    DESIGN("디자인"),
    DELIVERY("배달"),
    ETC("기타");
    
    private final String description;
    
    ServiceCategory(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
