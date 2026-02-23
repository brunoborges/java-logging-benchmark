package com.benchmark.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import org.openjdk.jmh.annotations.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmarks for Logback logging framework.
 * Tests both synchronous and asynchronous (AsyncAppender) logging.
 * Uses programmatic configuration to avoid SLF4J binding conflicts with Log4j2.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(2)
public class LogbackBenchmark {

    private static final String LOG_PATTERN = "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n";

    // ── Synchronous State ──────────────────────────────────────────────

    @State(Scope.Benchmark)
    public static class SyncState {
        Logger logger;
        LoggerContext loggerContext;
        Path tempDir;
        RuntimeException testException;

        @Setup(org.openjdk.jmh.annotations.Level.Trial)
        public void setup() throws IOException {
            tempDir = Files.createTempDirectory("logback-sync-bench");
            String logFile = tempDir.resolve("benchmark.log").toString();

            loggerContext = new LoggerContext();
            loggerContext.setName("logback-sync-benchmark");

            PatternLayoutEncoder encoder = new PatternLayoutEncoder();
            encoder.setContext(loggerContext);
            encoder.setPattern(LOG_PATTERN);
            encoder.start();

            FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
            fileAppender.setContext(loggerContext);
            fileAppender.setName("file");
            fileAppender.setFile(logFile);
            fileAppender.setEncoder(encoder);
            fileAppender.setAppend(true);
            fileAppender.setImmediateFlush(true);
            fileAppender.start();

            logger = loggerContext.getLogger("com.benchmark.LogbackSync");
            logger.setLevel(ch.qos.logback.classic.Level.INFO);
            logger.addAppender(fileAppender);
            logger.setAdditive(false);

            testException = new RuntimeException("Test exception for benchmark");
        }

        @TearDown(org.openjdk.jmh.annotations.Level.Trial)
        public void tearDown() {
            if (loggerContext != null) loggerContext.stop();
            deleteTempDir(tempDir);
        }
    }

    // ── Asynchronous State ─────────────────────────────────────────────

    @State(Scope.Benchmark)
    public static class AsyncState {
        Logger logger;
        LoggerContext loggerContext;
        Path tempDir;
        RuntimeException testException;

        @Setup(org.openjdk.jmh.annotations.Level.Trial)
        public void setup() throws IOException {
            tempDir = Files.createTempDirectory("logback-async-bench");
            String logFile = tempDir.resolve("benchmark.log").toString();

            loggerContext = new LoggerContext();
            loggerContext.setName("logback-async-benchmark");

            PatternLayoutEncoder encoder = new PatternLayoutEncoder();
            encoder.setContext(loggerContext);
            encoder.setPattern(LOG_PATTERN);
            encoder.start();

            FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
            fileAppender.setContext(loggerContext);
            fileAppender.setName("file");
            fileAppender.setFile(logFile);
            fileAppender.setEncoder(encoder);
            fileAppender.setAppend(true);
            fileAppender.setImmediateFlush(false);
            fileAppender.start();

            AsyncAppender asyncAppender = new AsyncAppender();
            asyncAppender.setContext(loggerContext);
            asyncAppender.setName("async");
            asyncAppender.addAppender(fileAppender);
            asyncAppender.setQueueSize(1024);
            asyncAppender.setDiscardingThreshold(0);
            asyncAppender.setIncludeCallerData(false);
            asyncAppender.start();

            logger = loggerContext.getLogger("com.benchmark.LogbackAsync");
            logger.setLevel(ch.qos.logback.classic.Level.INFO);
            logger.addAppender(asyncAppender);
            logger.setAdditive(false);

            testException = new RuntimeException("Test exception for benchmark");
        }

        @TearDown(org.openjdk.jmh.annotations.Level.Trial)
        public void tearDown() {
            if (loggerContext != null) loggerContext.stop();
            deleteTempDir(tempDir);
        }
    }

    // ── Sync Benchmarks ────────────────────────────────────────────────

    @Benchmark
    public void syncSimple(SyncState state) {
        state.logger.info("Simple log message for benchmark testing");
    }

    @Benchmark
    public void syncParameterized(SyncState state) {
        state.logger.info("User {} logged in from {} at port {}", "john", "192.168.1.1", 8080);
    }

    @Benchmark
    public void syncException(SyncState state) {
        state.logger.error("Error processing request", state.testException);
    }

    @Benchmark
    public void syncDisabledLevel(SyncState state) {
        state.logger.debug("This message should not be logged at INFO level");
    }

    // ── Async Benchmarks ───────────────────────────────────────────────

    @Benchmark
    public void asyncSimple(AsyncState state) {
        state.logger.info("Simple log message for benchmark testing");
    }

    @Benchmark
    public void asyncParameterized(AsyncState state) {
        state.logger.info("User {} logged in from {} at port {}", "john", "192.168.1.1", 8080);
    }

    @Benchmark
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
