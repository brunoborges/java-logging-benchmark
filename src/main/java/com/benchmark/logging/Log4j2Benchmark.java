package com.benchmark.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.openjdk.jmh.annotations.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmarks for Apache Log4j2 logging framework.
 * Tests both synchronous and asynchronous (LMAX Disruptor) logging.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
public class Log4j2Benchmark {

    private static final String LOG_PATTERN = "%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n";

    // ── Synchronous State ──────────────────────────────────────────────

    @State(Scope.Benchmark)
    public static class SyncState {
        Logger logger;
        Path tempDir;
        RuntimeException testException;

        @Setup(Level.Trial)
        public void setup() throws IOException {
            tempDir = Files.createTempDirectory("log4j2-sync-bench");
            System.setProperty("benchmark.logFile", tempDir.resolve("benchmark.log").toString());
            System.setProperty("log4j2.configurationFile", "log4j2-bench.xml");

            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            ctx.reconfigure();

            logger = LogManager.getLogger("com.benchmark.Log4j2Sync");
            testException = new RuntimeException("Test exception for benchmark");
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            Configurator.shutdown((LoggerContext) LogManager.getContext(false));
            deleteTempDir(tempDir);
        }
    }

    // ── Asynchronous State (LMAX Disruptor) ────────────────────────────

    @State(Scope.Benchmark)
    public static class AsyncState {
        Logger logger;
        Path tempDir;
        RuntimeException testException;

        @Setup(Level.Trial)
        public void setup() throws IOException {
            tempDir = Files.createTempDirectory("log4j2-async-bench");
            System.setProperty("benchmark.logFile", tempDir.resolve("benchmark.log").toString());
            System.setProperty("log4j2.configurationFile", "log4j2-bench-async.xml");

            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            ctx.reconfigure();

            logger = LogManager.getLogger("com.benchmark.Log4j2Async");
            testException = new RuntimeException("Test exception for benchmark");
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            Configurator.shutdown((LoggerContext) LogManager.getContext(false));
            deleteTempDir(tempDir);
        }
    }

    // ── Sync Benchmarks ────────────────────────────────────────────────

    @Benchmark
    @Fork(2)
    public void syncSimple(SyncState state) {
        state.logger.info("Simple log message for benchmark testing");
    }

    @Benchmark
    @Fork(2)
    public void syncParameterized(SyncState state) {
        state.logger.info("User {} logged in from {} at port {}", "john", "192.168.1.1", 8080);
    }

    @Benchmark
    @Fork(2)
    public void syncException(SyncState state) {
        state.logger.error("Error processing request", state.testException);
    }

    @Benchmark
    @Fork(2)
    public void syncDisabledLevel(SyncState state) {
        state.logger.debug("This message should not be logged at INFO level");
    }

    // ── Async Benchmarks (LMAX Disruptor) ──────────────────────────────

    @Benchmark
    @Fork(value = 2, jvmArgsAppend = {
        "-Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector"
    })
    public void asyncSimple(AsyncState state) {
        state.logger.info("Simple log message for benchmark testing");
    }

    @Benchmark
    @Fork(value = 2, jvmArgsAppend = {
        "-Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector"
    })
    public void asyncParameterized(AsyncState state) {
        state.logger.info("User {} logged in from {} at port {}", "john", "192.168.1.1", 8080);
    }

    @Benchmark
    @Fork(value = 2, jvmArgsAppend = {
        "-Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector"
    })
    public void asyncException(AsyncState state) {
        state.logger.error("Error processing request", state.testException);
    }

    // ── Utility ────────────────────────────────────────────────────────

    static void deleteTempDir(Path dir) {
        if (dir == null) return;
        try {
            Files.walk(dir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        } catch (IOException e) {
            // ignore cleanup errors
        }
    }
}
