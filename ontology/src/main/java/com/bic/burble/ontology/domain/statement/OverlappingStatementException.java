package com.bic.burble.ontology.domain.statement;

/**
 * A statement was rejected because the subject already has the same verb for
 * part of that time, for example an item carried by two characters at once.
 */
public class OverlappingStatementException extends InvalidStatementException {
    public OverlappingStatementException(String message) {
        super(message);
    }
}
