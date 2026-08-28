# Burble

**Burble** is an open-source worldbuilding, storytelling, and simulation platform for creatives, players, gamers, and organizers.

It helps you create, organize, and connect characters, places, items, events, and more — whether you’re building a novel setting, a TTRPG campaign, a personal wiki, or a living game world.

Relationships are treated as first-class citizens. The system is built around a rigorous ontology so what exists in your world stays consistent, queryable, and usable across story tracking, TTRPG play, and future simulation layers.

---

## Documentation

Full documentation is built with MkDocs and available via GitHub Pages.

You can find it live on [Burble Docs](https://BurningIceCube.github.io/Burble/).

**Local docs commands:**

- `mkdocs serve` (http://127.0.0.1:8000)
- `mkdocs build`
- `mkdocs gh-deploy` (publish to GitHub Pages)

Once deployed, the docs will be available at:  
`https://<your-username>.github.io/Burble/`

---

## Features

- Rich entity model built on a formal ontology
- First-class relationships with time and certainty
- User-friendly **Composites** (Culture, Economy, Ecology, etc.) that map onto the formal model
- Wikipedia-style pages for deep lore
- Designed for both worldbuilding and actionable TTRPG / game use
- Layered architecture:
    - **Ontology** — what exists and what is true
    - **Story** — narrative structure tooling and tracking
    - **State** — live mechanical and situational state, especially for TTRPGs
    - **Game** — rules and play, live sessions, and simulation
    - **Shared** — common DTOs, events, and contracts
    - **Forum** — discussion and collaboration and online forum play
    - **Orchestrator** — the main application that ties everything together
- Open-source and self-hostable

---

## Architecture (High Level)

- **Worlds** are the top-level containers
- **Entities** belong to one or more formal **Archetypes**
- **Statements** (relationships) connect entities and carry time + certainty
- **Composites** provide familiar concepts (Culture, Economy, etc.) as facades over the formal ontology

For detailed design, see the [documentation](docs/index.md).

---

## Project Structure

```text
burble/
├── docs/          # MkDocs    documentation    
├── ontology/      # Formal    ontology and entity model 
├── story/         # Narrative   structure and story tracking  
├── state/         # Live mechanical and situational state, especially for TTRPGs
├── game/          # Rules and play, live sessions, and simulation
├── forum/         # Discussion and collaboration and online forum play
├── shared/         # Common DTOs, events, common contracts  
├── orchestrator/            # Orchestrator / runnable module
└── README.md
```

---

## Running the Application

### Prerequisites

- Java 21+ installed (JDK 21 LTS or newer recommended)

### Build and Run

Run via Gradle wrapper:

```bash
# Unix / macOS
./gradlew bootRun

# Windows (Command Prompt / PowerShell)
.\gradlew.bat bootRun
```

The application starts on `http://localhost:8080`.

### Endpoints 

Only the `orchestrator` module exposes endpoints. The other modules are libraries that provide functionality to the orchestrator.

### API Documentation

- **Swagger UI**: http://localhost:8080/swagger-ui.html (or http://localhost:8080/swagger-ui/index.html)
- **OpenAPI Spec**: http://localhost:8080/v3/api-docs

### Actuator

- **Health**: http://localhost:8080/actuator/health
- **Info**: http://localhost:8080/actuator/info

---

## License

[MIT](LICENSE)