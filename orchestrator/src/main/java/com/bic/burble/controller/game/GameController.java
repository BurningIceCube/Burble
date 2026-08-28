package com.bic.burble.controller.game;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Example endpoint for the {@code game} module. Orchestrator hosts all
 * API endpoints; the game module itself is a library with no web layer.
 */
@RestController
@RequestMapping("/api/v1/game")
public class GameController {

  @GetMapping("/foo")
  public Map<String, String> foo() {
    return Map.of(
        "module", "game",
        "status", "ok",
        "message", "bar"
    );
  }
}
