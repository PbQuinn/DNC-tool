package org.networkcalculus.dnc.SPPAnalyzer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;


/** Writes: TX, RX, Traffic Family, then delay and backlog bound for each analysis method. */
final class CsvResultWriter {
    private CsvResultWriter() {}

    static void write(File out, List<BoundsAnalyzer.FlowResult> results) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(out.toPath(), StandardCharsets.UTF_8)) {
            StringBuilder header = new StringBuilder("TX,RX,Traffic Family");
            for (BoundsAnalyzer.Method m : BoundsAnalyzer.Method.values()) {
                header.append(',').append(m).append(" delay,").append(m).append(" backlog");
            }
            w.write(header.toString());
            w.newLine();

            for (BoundsAnalyzer.FlowResult r : results) {
                StringBuilder row = new StringBuilder();
                row.append(esc(r.spec.tx)).append(',').append(esc(r.spec.rx)).append(',').append(esc(r.spec.type));
                for (BoundsAnalyzer.Method m : BoundsAnalyzer.Method.values()) {
                    BoundsAnalyzer.Bounds b = r.bounds.get(m);
                    row.append(',').append(fmt(b.delay)).append(',').append(fmt(b.backlog));
                }
                w.write(row.toString());
                w.newLine();
            }
        }
    }

    private static String fmt(Double d) {
        if (d == null) return "ERR";
        if (d.isInfinite()) return d > 0 ? "inf" : "-inf";
        return Double.toString(d);   // locale independent
    }

    private static String esc(String s) {
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}