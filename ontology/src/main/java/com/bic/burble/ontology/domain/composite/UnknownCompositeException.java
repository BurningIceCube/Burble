package com.bic.burble.ontology.domain.composite;

import com.bic.burble.ontology.domain.statement.InvalidStatementException;

/** The composite name is not one of {@link Composite}. */
public class UnknownCompositeException extends InvalidStatementException {
    public UnknownCompositeException(String name) {
        super("Unknown composite \"" + name + "\". Allowed values: " + Composite.allowed());
    }
}
