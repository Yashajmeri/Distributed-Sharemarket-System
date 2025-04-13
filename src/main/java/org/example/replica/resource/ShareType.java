package org.example.replica.resource;

public enum ShareType {
    EQUITY("Equity"),
    BONUS("Bonus"),
    DIVIDEND("Dividend");

    private String type;

    ShareType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
