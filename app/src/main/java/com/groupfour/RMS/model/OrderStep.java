package com.groupfour.RMS.model;

public class OrderStep {
    private final String label;
    private final String state;

    public OrderStep(String label, String state) {
        this.label = label;
        this.state = state;
    }

    public String getLabel() { return label; }

    // "done", "current" or "todo"
    public String getState() { return state; }
}
