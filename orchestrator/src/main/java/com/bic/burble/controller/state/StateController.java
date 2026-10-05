package com.bic.burble.controller.state;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

  private static final Logger log = LoggerFactory.getLogger(StateController.class);

  @GetMapping("/foo")
  public Map<String, String> foo() {
    log.info("GET  /api/v1/state/foo");
    return Map.of(
        "module", "state",
        "status", "ok",
        "message", "bar"
    );
  }
}
