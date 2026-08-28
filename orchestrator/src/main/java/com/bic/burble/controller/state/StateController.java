package com.bic.burble.controller.state;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Example endpoint for the {@code state} module. Orchestrator hosts all
 * API endpoints; the state module itself is a library with no web layer.
 */
@RestController
@RequestMapping("/api/v1/state")
public class StateController {

  @GetMapping("/foo")
  public Map<String, String> foo() {
    return Map.of(
        "module", "state",
        "status", "ok",
        "message", "bar"
    );
  }
}
