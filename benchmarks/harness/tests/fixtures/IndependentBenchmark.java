package calibration.controls;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class IndependentBenchmark {
    @State(Scope.Thread)
    public static class Arithmetic {
        @Param({"64", "128"})
        public int rounds;
        public long value = 0x56f1c9a87d32e041L;
    }

    @State(Scope.Thread)
    public static class Allocation {
        @Param({"16", "272"})
        public int bytes;
        public volatile byte[] escaped;
    }

    @Benchmark
    public long arithmetic(Arithmetic input) {
        long x = input.value;
        for (int i = 0; i < input.rounds; i++) {
            x = Long.rotateLeft(x * 0x9e3779b97f4a7c15L + 17, 23);
        }
        input.value = x;
        return x;
    }

    @Benchmark
    public void allocate(Allocation input) {
        input.escaped = new byte[input.bytes];
    }
}
