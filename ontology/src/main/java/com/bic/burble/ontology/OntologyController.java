package com.bic.burble.ontology;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
