# Copilot Instructions

## Build & Run

Requires **Java 25+** and **Maven 3.9+**.

```bash
# Build the uber JAR
mvn clean package -q

# Run all benchmarks
java -jar target/benchmarks.jar

# Run a specific benchmark class
java -jar target/benchmarks.jar "Log4j2"

# Run a specific scenario (regex match)
java -jar target/benchmarks.jar "LogbackBenchmark.syncSimple"

# Quick smoke test (shorter iterations)
java -jar target/benchmarks.jar -f 1 -wi 3 -i 5

# Export results as JSON for the visualization dashboard
java -jar target/benchmarks.jar -rf json -rff results.json
```

There are no unit tests — this is a JMH benchmark project. Validation means the jar builds and benchmarks execute without errors.

## Architecture

This is a JMH (Java Microbenchmark Harness) project comparing three logging frameworks: **Log4j2**, **Logback**, and **JUL** (java.util.logging). The build produces a single uber JAR (`target/benchmarks.jar`) via maven-shade-plugin.

Each framework has one benchmark class in `src/main/java/com/benchmark/logging/`:

- **`Log4j2Benchmark`** — configured via XML files in `src/main/resources/` (`log4j2-bench.xml` for sync, `log4j2-bench-async.xml` for async). Async mode uses LMAX Disruptor via `@Fork(jvmArgsAppend)` to set the `AsyncLoggerContextSelector`.
- **`LogbackBenchmark`** — configured **programmatically** in the `@State` setup methods (not XML) to avoid SLF4J binding conflicts with Log4j2.
- **`JULBenchmark`** — configured programmatically. Sync only (no async variant).

`docs/index.html` is a static dashboard for visualizing JSON benchmark results. It is designed for GitHub Pages hosting.

## Key Conventions

- Each benchmark class manages its own **temp directory** (`Files.createTempDirectory`) for log output, cleaned up in `@TearDown`. Never write log files to fixed paths.
- All frameworks use an **equivalent log pattern** (`%date [%thread] %-5level %logger{36} - %message%n`) to ensure fair comparison.
- Benchmark scenarios follow a **consistent naming convention**: `syncSimple`, `syncParameterized`, `syncException`, `syncDisabledLevel`, `asyncSimple`, `asyncParameterized`, `asyncException`. New benchmarks should follow this pattern.
- JMH settings are declared via annotations: `@BenchmarkMode(Mode.Throughput)`, `@Warmup(iterations=5)`, `@Measurement(iterations=10)`, `@Fork(2)`.
- Log4j2 is the only framework using XML config files; Logback and JUL are configured in Java to avoid classpath conflicts.
