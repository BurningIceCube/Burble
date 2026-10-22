package com.bic.burble.ontology.domain.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import com.bic.burble.ontology.domain.statement.StatementTime;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "A relationship stored once, on the subject entity. The inverse is not written.")
public record AddRelationshipRequest(
        @NotBlank(message = "Verb is required")
        @Schema(description = "Relationship verb from the fixed list (CARRIED_BY). Matched case-insensitively and stored upper case. Carrying is stored as CARRIED_BY on the thing carried.", example = "CARRIED_BY")
        String verb,

        @NotBlank(message = "Object entity id is required")
        @Schema(description = "GUID of the other entity", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        String objectId,

        @Schema(description = "When the statement holds. Leave out for always or unknown.")
        StatementTime time,

        @DecimalMin(value = "0.0", message = "Certainty must be at least 0")
        @DecimalMax(value = "1.0", message = "Certainty must be at most 1")
        @Schema(description = "How sure the world is, 0 to 1. Defaults to 1.", example = "0.8")
        Double certainty
) {
    public AddRelationshipRequest(String verb, String objectId) {
        this(verb, objectId, null, null);
    }
}
