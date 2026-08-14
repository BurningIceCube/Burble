package com.bic.burble.game;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
