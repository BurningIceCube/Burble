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

### Option A: Docker Compose (recommended)

The easiest way to run the full stack — the Burble app **and** Neo4j — is via Docker Compose. No local Java or Gradle setup required.

**Prerequisites:**

- Docker with Compose v2 (`docker compose`)

**Run it:**

```bash
docker compose up --build
```

This builds the orchestrator image (multi-stage Gradle build), then starts:

- **Burble app**: http://localhost:8080
- **Neo4j Browser**: http://localhost:7474
- **Neo4j Bolt** (used internally by the app): `bolt://localhost:7687`

Neo4j runs with authentication disabled (`NEO4J_AUTH=none`), matching the app's default (no-auth) configuration. The app connects to Neo4j over the internal Docker network at `bolt://neo4j:7687` and waits for Neo4j to report healthy before starting.

To stop the stack:

```bash
docker compose down
```

Add `-v` to also remove the Neo4j data volume (`docker compose down -v`).

### Option B: Local Gradle

If you'd rather run the app directly with Gradle (e.g. for faster iteration / debugging), run it locally. The default JSON storage needs no database.

**Prerequisites:**

- Java 21+ installed (JDK 21 LTS or newer recommended)
- Docker, only if entity storage is switched to Neo4j (see [Neo4j](#neo4j-optional-entity-persistence) below)

#### Storage

Ontology storage is chosen in `orchestrator/src/main/resources/application.yml`:

```yaml
burble:
  ontology:
    persistance:
      world: json   # json | h2
      entity: json  # json | neo4j
      json:
        path: data/ontology.json
```

`json` is the default for both. Worlds and entities are written to that one file (path is relative to the process working directory). An entity is a single record. Each archetype is an `IS_A` statement on the entity, so one sword can be an Item and a Character at the same time. The file is reread on startup, so records survive a restart. No database is required.

Set `world: h2` to use the embedded H2 database for worlds, and `entity: neo4j` to use Neo4j for entities. Neo4j is only required when entity storage is `neo4j`.

#### Neo4j (optional entity persistence)

When `burble.ontology.persistance.entity` is `neo4j`, entity records are stored in Neo4j. You need a Neo4j instance running before those entity endpoints will work.

The quickest way to get one running locally is via Docker, with authentication disabled to match the app's default (no-auth) configuration:

```bash
docker run \
  --name burble-neo4j \
  -p 7474:7474 -p 7687:7687 \
  -e NEO4J_AUTH=none \
  neo4j:5
```

- **Neo4j Browser** (to inspect the graph): http://localhost:7474
- **Bolt port** (used by the app): `bolt://localhost:7687`

The orchestrator connects to `bolt://localhost:7687` with no credentials by default (Spring Boot's Neo4j auto-configuration). If you run Neo4j with authentication enabled instead, set the following in `orchestrator/src/main/resources/application.yml` (or as environment variables):

```yaml
spring:
  neo4j:
    uri: bolt://localhost:7687
    authentication:
      username: neo4j
      password: <your-password>
```

To stop/remove the container later:

```bash
docker stop burble-neo4j && docker rm burble-neo4j
```

#### Build and Run

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

Notable ontology endpoints:

- `/api/v1/ontology/world` — CRUD for World records
- `/api/v1/ontology/world/{worldId}/entity` — CRUD for Entity records within a world (archetypes are stored as `IS_A` statements)

See the full list via Swagger UI.

### API Documentation

- **Swagger UI**: http://localhost:8080/swagger-ui.html (or http://localhost:8080/swagger-ui/index.html)
- **OpenAPI Spec**: http://localhost:8080/v3/api-docs

### Actuator

- **Health**: http://localhost:8080/actuator/health
- **Info**: http://localhost:8080/actuator/info

---

## License

[MIT](LICENSE)
