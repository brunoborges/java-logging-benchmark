# Java Logging Framework Benchmark

JMH benchmarks comparing the performance of Java's most popular logging frameworks on **Java 25**.

Inspired by the [SitePoint article](https://www.sitepoint.com/which-java-logging-framework-has-the-best-performance/) on Java logging performance.

## Frameworks & Versions

| Framework | Version | Mode |
|---|---|---|
| Apache Log4j2 | 2.25.3 | Sync (FileAppender) + Async (LMAX Disruptor) |
| Logback | 1.5.32 | Sync (FileAppender) + Async (AsyncAppender) |
| java.util.logging (JUL) | JDK 25 | Sync (FileHandler) |

## Benchmark Scenarios

Each framework is tested with these scenarios:

| Scenario | Description |
|---|---|
| `syncSimple` | Log a simple string message synchronously |
| `syncParameterized` | Log a parameterized message (avoids string concatenation) |
| `syncException` | Log an error with an exception stack trace |
| `syncDisabledLevel` | Log at DEBUG when level is INFO (tests guard performance) |
| `asyncSimple` | Log a simple message asynchronously (Log4j2 & Logback only) |
| `asyncParameterized` | Log a parameterized message asynchronously |
| `asyncException` | Log an error with exception asynchronously |

## Prerequisites

- **Java 25** (or later)
- **Apache Maven 3.9+**

## Build

```bash
mvn clean package -q
```

This produces `target/benchmarks.jar` — a self-contained uber JAR with all dependencies.

## Run All Benchmarks

```bash
java -jar target/benchmarks.jar
```

## Run Specific Benchmarks

```bash
# Only Log4j2 benchmarks
java -jar target/benchmarks.jar "Log4j2"

# Only async benchmarks
java -jar target/benchmarks.jar ".*async.*"

# Only Logback sync
java -jar target/benchmarks.jar "LogbackBenchmark.sync"

# Only JUL
java -jar target/benchmarks.jar "JUL"
```

## Customize JMH Parameters

```bash
# Quick smoke test (1 fork, 3 warmup, 5 measurement iterations)
java -jar target/benchmarks.jar -f 1 -wi 3 -i 5

# Output results as JSON
java -jar target/benchmarks.jar -rf json -rff results.json

# List available benchmarks
java -jar target/benchmarks.jar -l
```

## Configuration Details

### Log4j2
- **Sync**: `FileAppender` with `bufferedIO=true`, `bufferSize=8192`
- **Async**: `RandomAccessFileAppender` with `immediateFlush=false`, using Log4j2's `AsyncLoggerContextSelector` backed by LMAX Disruptor 4.0.0

### Logback
- **Sync**: `FileAppender` with `immediateFlush=true`
- **Async**: `AsyncAppender` wrapping `FileAppender` with `queueSize=1024`, `discardingThreshold=0`, `immediateFlush=false`

### JUL
- **Sync**: `FileHandler` with custom `Formatter` matching the pattern layout of Log4j2/Logback
- No built-in async support

### Common Pattern
All frameworks use an equivalent log pattern:
```
%date [%thread] %-5level %logger{36} - %message%n
```

## Results Visualization

📊 **Live dashboard:** [brunoborges.github.io/java-logging-benchmark](https://brunoborges.github.io/java-logging-benchmark/)

The dashboard auto-loads the latest CI benchmark results from Linux, macOS, and Windows with tabbed comparison views.

You can also run benchmarks locally and drag & drop your own `results.json` files onto the page:

```bash
java -jar target/benchmarks.jar -rf json -rff results.json
```

Name files with the OS (e.g. `results-linux.json`, `results-macos.json`, `results-windows.json`) to get per-OS tabs.

## CI / Cross-Platform Benchmarks

A GitHub Actions workflow runs the full benchmark suite on three operating systems via a matrix build:

| Runner | OS |
|---|---|
| `ubuntu-latest` | Linux |
| `macos-latest` | macOS (Apple Silicon) |
| `windows-latest` | Windows |

The workflow triggers on pushes and PRs to `main`, or manually via **Actions → Logging Benchmark → Run workflow**.

Each runner uploads its results as a JSON artifact (`benchmark-results-<os>`), making it easy to download and compare cross-platform performance.

## Architecture

All benchmarks write to temporary files (via `Files.createTempDirectory`) to avoid I/O interference between runs. Temp files are cleaned up in `@TearDown`. Each framework is configured programmatically (Logback, JUL) or via classpath XML config (Log4j2) to ensure isolation.
