package org.networkcalculus.dnc.SPPAnalyzer;


import com.fasterxml.jackson.databind.JsonNode;

/** One instance of a flow (TX, RX, flow type), a triple from the config. */
final class FlowSpec {

    /** 99th percentile / mean / std of one characteristic (IAT or Size). */
    static final class Stats {
        final double p99, mean, std;

        Stats(JsonNode n) {
            this.p99 = n.path("99pct").asDouble();
            this.mean = n.path("mean").asDouble();
            this.std = n.path("std").asDouble();
        }

        @Override
        public String toString() {
            return String.format("99pct=%.6g mean=%.6g std=%.6g", p99, mean, std);
        }
    }



    final String tx, rx, type;
    final Stats iat, size;

    FlowSpec(String tx, String rx, String type, Stats iat, Stats size) {
        this.tx = tx;
        this.rx = rx;
        this.type = type;
        this.iat = iat;
        this.size = size;
    }

    String alias() {
        return String.format("%s_%s->%s", type, tx, rx);
    }
}