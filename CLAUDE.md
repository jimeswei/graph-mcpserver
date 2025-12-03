# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Knowledge Graph MCP (Model Context Protocol) Server built with Spring Boot that provides intelligent graph analysis capabilities through an AI-accessible interface. The system runs on port 5821 and integrates with AI systems via the MCP protocol for natural language graph queries.

## Development Commands

```bash
# Build and compile
mvn clean compile

# Run tests
mvn test

# Run specific test class
mvn test -Dtest=GraphTest

# Package the application
mvn package

# Run the application locally
mvn spring-boot:run

# Build Docker image
mvn spring-boot:build-image

# Git workflow (from README)
git add .
git commit -m "commit message"
git push origin main
```

## Architecture

**Layered Architecture:**
The codebase follows a clean layered architecture with reactive programming throughout:

1. **Core Graph Engine** (`src/main/java/com/example/graph/core/`)
   - `Graph.java` - Main graph data structure with adjacency list implementation
   - `Node.java` - Graph node with attributes support
   - `Edge.java` - Graph edge with source, destination, and weight properties
   - Supports both directed/undirected and weighted/unweighted graphs

2. **Algorithm Layer** (`src/main/java/com/example/graph/algorithm/`)
   - `PageRank.java` - PageRank algorithm with caching optimizations
   - `CommunityDetection.java` - Community detection algorithms
   - `JaccardSimilarity.java` - Similarity calculations
   - `ShortestPath.java` - Path finding algorithms

3. **MCP Integration Layer** (`src/main/java/com/example/graph/mcp/`)
   - `handler/` - REST controllers implementing MCP endpoints with reactive streaming
   - `service/` - Business logic services (GraphAnalysisService uses Gremlin query templates, GraphServiceOptimized exposes MCP tools)
   - `entity/` - JPA entities for database persistence (GraphCache for query caching)
   - `repository/` - JPA repositories with Spring Data
   - `dto/` - Data transfer objects for API requests/responses
   - `model/` - Response models including StreamableResponse for reactive streaming
   - `config/` - Configuration classes for MCP server and external API integration

**Technology Stack:**
- Spring Boot 3.4.5 with WebFlux for full reactive stack
- Java 17
- Spring AI 1.0.0 with MCP Server WebFlux support
- MySQL 8.0+ with JPA/Hibernate for graph caching
- FastJSON2 2.0.57 for JSON processing
- External Gremlin graph API integration

**Key Architectural Patterns:**
- Reactive programming with `Flux<StreamableResponse>` for streaming responses
- Clean separation between core graph engine, algorithms, and MCP integration
- Repository pattern with JPA for data persistence
- Service layer with dependency injection
- Configuration-driven external API integration

## Database Configuration

The application connects to MySQL at `192.168.3.78:3307/graph-agent`. Database credentials can be overridden with environment variables:
- `DB_USERNAME` (default: root)
- `DB_PASSWORD` (default: 123456)

Schema is provided in `graph-agent.sql` with tables:
- `graph_cache` - Caches graph query results with thread_id indexing
- `analysis_sessions` - Tracks analysis sessions with success/failure metrics
- `mcp_tool_results` - Stores MCP tool call results with agent and session tracking
- `tool_call_names` - Manages tool call metadata with parameters and indexing
- `users` - User authentication table with username/password/role support

## Main Graph Analysis Endpoints

1. **Mutual Friends**: `/mcp/mutual_friend_between_stars`
2. **Dream Team Common Works**: `/mcp/dream_team_common_works`
3. **Relation Chain**: `/mcp/relation_chain_between_stars`
4. **Similarity Analysis**: `/mcp/similarity_between_stars`
5. **Common Ancestor**: `/mcp/most_recent_common_ancestor`

## Testing

The project uses JUnit 5 and Mockito 5.2.0 with comprehensive test coverage:
- Algorithm tests (PageRank, Community Detection, Shortest Path)
- Core graph functionality tests
- MCP service integration tests
- Graph result formatting tests

Test files are organized in `src/test/java/com/example/graph/` with both unit tests and integration examples.

## Configuration

- Main config: `src/main/resources/application.yml`
- Development profile: `src/main/resources/application-dev.yml` (sets debug logging, external Gremlin API at `192.168.3.78:28080`)
- Active profile: `dev`
- MCP server runs on port 5821 with SSE endpoint at `/sse`
- External graph API integration configurable via `graph.api.base-url`

## Documentation

Comprehensive Chinese documentation is available in the `docs/` directory covering system architecture, algorithm design, and model interfaces.