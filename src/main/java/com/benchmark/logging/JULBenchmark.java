package com.benchmark.logging;

import org.openjdk.jmh.annotations.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * JMH benchmarks for java.util.logging (JUL).
 * JUL has no built-in async appender, so only synchronous logging is tested.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(2)
public class JULBenchmark {

    // ── Synchronous State ──────────────────────────────────────────────

    @State(Scope.Benchmark)
    public static class SyncState {
        Logger logger;
        Path tempDir;
        FileHandler fileHandler;
        RuntimeException testException;

        @Setup(Level.Trial)
        public void setup() throws IOException {
            tempDir = Files.createTempDirectory("jul-sync-bench");
            String logFile = tempDir.resolve("benchmark.log").toString();

            logger = Logger.getLogger("com.benchmark.JULSync");
            logger.setUseParentHandlers(false);
            logger.setLevel(java.util.logging.Level.INFO);

            // Remove any existing handlers
            for (var h : logger.getHandlers()) {
                logger.removeHandler(h);
            }

            fileHandler = new FileHandler(logFile, true);
            fileHandler.setFormatter(new BenchmarkFormatter());
            logger.addHandler(fileHandler);

            testException = new RuntimeException("Test exception for benchmark");
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            if (fileHandler != null) {
                fileHandler.close();
                logger.removeHandler(fileHandler);
            }
            deleteTempDir(tempDir);
        }
    }

    /**
     * Custom formatter matching the pattern used by Log4j2 and Logback benchmarks.
     */
    static class BenchmarkFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            return String.format("%1$tF %1$tT.%1$tL [%2$s] %3$-7s %4$s - %5$s%n",
                record.getMillis(),
                Thread.currentThread().getName(),
                record.getLevel().getName(),
                record.getLoggerName(),
                formatMessage(record));
        }
    }

    // ── Sync Benchmarks ────────────────────────────────────────────────

    @Benchmark
    public void syncSimple(SyncState state) {
        state.logger.info("Simple log message for benchmark testing");
    }

    @Benchmark
    public void syncParameterized(SyncState state) {
        state.logger.log(java.util.logging.Level.INFO,
            "User {0} logged in from {1} at port {2}",
            new Object[]{"john", "192.168.1.1", 8080});
    }

    @Benchmark
    public void syncException(SyncState state) {
        state.logger.log(java.util.logging.Level.SEVERE,
            "Error processing request", state.testException);
    }

    @Benchmark
    public void syncDisabledLevel(SyncState state) {
        state.logger.fine("This message should not be logged at INFO level");
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
