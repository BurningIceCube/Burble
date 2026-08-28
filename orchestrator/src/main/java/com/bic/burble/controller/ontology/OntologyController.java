package com.bic.burble.controller.ontology;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Example endpoint for the {@code ontology} module. Orchestrator hosts all
 * API endpoints; ontology remains a library that provides the real
 * domain/service layer (see {@link WorldController}).
 */
@RestController
@RequestMapping("/api/v1/ontology")
public class OntologyController {

  @GetMapping("/foo")
  public Map<String, String> foo() {
    return Map.of(
        "module", "ontology",
        "status", "ok",
        "message", "bar"
    );
  }
}
