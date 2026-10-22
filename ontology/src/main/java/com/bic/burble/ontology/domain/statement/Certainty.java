package com.bic.burble.ontology.domain.statement;

/**
 * How sure the world is that a statement is true, from 0 (not at all) to 1
 * (certain). Statements without a stored certainty are certain.
 */
public final class Certainty {

    public static final double CERTAIN = 1.0;

    private Certainty() {
    }

    public static double orDefault(Double certainty) {
        return certainty == null ? CERTAIN : certainty;
    }

    public static double validate(Double certainty) {
        double value = orDefault(certainty);
        if (Double.isNaN(value) || value < 0.0 || value > 1.0) {
            throw new InvalidStatementException("Certainty must be between 0 and 1, got " + certainty);
        }
        return value;
    }
}
