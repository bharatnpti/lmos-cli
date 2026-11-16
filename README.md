# lmos-cli

A command-line tool for [Eclipse LMOS](https://github.com/eclipse-lmos), the open-source platform for building and running multi-agent systems.
It scaffolds new [Arc](https://github.com/eclipse-lmos/arc) agent projects and keeps your LLM provider settings and API keys in the OS keychain rather than in plain-text config files.

Built with Kotlin, Quarkus and Picocli.

> **Status: prototype (early 2025).** `agent create` and `config llm` work on macOS. `agent chat` is a skeleton, `agent install` and `agent list` are not implemented yet, and the Linux and Windows credential stores are still open. See [Roadmap](#roadmap).

## Commands

```
lmos
├── agent
│   ├── create   Generate a new agent project
│   └── chat     Chat with a local agent (work in progress)
└── config
    └── llm      add | get | list | update | delete
```

### Create an agent

```bash
lmos agent create -t ARC --an weather-agent --am gpt-4o \
  --ad "Answers questions about the weather" \
  --ap "You are a helpful weather assistant."
```

Any option you leave out is prompted for interactively. The command generates a Gradle and Spring Boot project for an Arc agent, with the name, model, description and system prompt filled in, under `~/.lmos/cli/agents/projects/`.

### Manage LLM configs

```bash
lmos config llm add -i openai     # prompts for model, base URL, API key and provider
lmos config llm list              # IDs only; secrets are never printed
lmos config llm get -i openai     # API key masked to its last 4 characters
lmos config llm update -i openai
lmos config llm delete -i openai
```

Each config (model name, base URL, API key, provider) is serialised to YAML and stored as one secret per ID.

## Design

| Piece | What it does |
|---|---|
| `LmosCli` | The Picocli `@TopCommand`. On first run it creates `~/.lmos/cli/` (agent projects, agent registry, credential store). |
| `LLMConfigManager` | CRUD for LLM configs on top of whichever credential manager is active |
| `CredentialManager` | Interface for secret storage, with one implementation per platform |
| `MacOSCredentialManager` | Stores secrets in the macOS Keychain through the `security` CLI |
| `DefaultCredentialManager` | Fallback store: an AES-encrypted file in `~/.lmos/cli/.cred/` |
| `CredentialManagerFactory` | Picks the store for the current OS, checks that it works, falls back to the default store if not, and records the choice so later runs use the same store |
| `Create` | Calls the `lmos-starter` generator (`LmosAgentGeneratorService` + `GradleSpringProjectFactory`) |

## Build

Requirements: JDK 21. The build also needs the `org.eclipse.lmos:lmos-starter:0.0.1-SNAPSHOT` generator library in your local Maven repository, because it is not published to Maven Central yet.

```bash
./gradlew build
java -jar build/quarkus-app/quarkus-run.jar --help
alias lmos='java -jar /path/to/lmos-cli/build/quarkus-app/quarkus-run.jar'
```

Dev mode with live reload:

```bash
./gradlew quarkusDev --quarkus-args='config llm list'
```

## Roadmap

- `agent chat`: start the generated agent and route the conversation to it (the `AgentCommunicator` and `AgentRegistry` interfaces are in place)
- `agent install` / `agent list`: install agents and tools from a registry
- Credential stores for Linux (Secret Service) and Windows (Credential Manager)
- Native binary through Quarkus/GraalVM
