package com.bic.burble.state;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
