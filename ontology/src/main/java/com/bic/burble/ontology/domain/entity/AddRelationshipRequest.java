package com.bic.burble.ontology.domain.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "A relationship stored once, on the subject entity. The inverse is not written.")
public record AddRelationshipRequest(
        @NotBlank(message = "Verb is required")
        @Schema(description = "Relationship verb. Carrying is stored as CARRIED_BY on the thing carried.", example = "CARRIED_BY")
        String verb,

        @NotBlank(message = "Object entity id is required")
        @Schema(description = "GUID of the other entity", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        String objectId
) {
}
