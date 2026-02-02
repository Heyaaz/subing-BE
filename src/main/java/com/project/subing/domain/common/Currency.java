package com.project.subing.domain.common;

public enum Currency {
	KRW("원"),
	USD("달러");

	private final String displayName;

	Currency(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
