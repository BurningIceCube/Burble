package com.bic.burble.ontology.domain.statement;

/** A statement was rejected because its time or certainty is not valid. */
public class InvalidStatementException extends IllegalArgumentException {
    public InvalidStatementException(String message) {
        super(message);
    }
}
