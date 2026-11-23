package org.networkcalculus.dnc.SPPAnalyzer;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Parses traffic.json: TX -> RX -> family -> {IAT, Size} -> {99pct, mean, std}. */
final class TrafficParser {
    private TrafficParser() {}

    static List<FlowSpec> parse(JsonNode root) throws SpplogFormatException {
        List<FlowSpec> flows = new ArrayList<>();

        for (Iterator<Map.Entry<String, JsonNode>> txIt = root.fields(); txIt.hasNext(); ) {
            Map.Entry<String, JsonNode> tx = txIt.next();
            requireObject(tx.getValue(), tx.getKey());

            for (Iterator<Map.Entry<String, JsonNode>> rxIt = tx.getValue().fields(); rxIt.hasNext(); ) {
                Map.Entry<String, JsonNode> rx = rxIt.next();
                requireObject(rx.getValue(), tx.getKey() + " -> " + rx.getKey());

                // TX == RX pairs have no network path in a star topology.
                if (tx.getKey().equals(rx.getKey())) continue;

                for (Iterator<Map.Entry<String, JsonNode>> fIt = rx.getValue().fields(); fIt.hasNext(); ) {
                    Map.Entry<String, JsonNode> f = fIt.next();
                    String where = tx.getKey() + " -> " + rx.getKey() + " -> " + f.getKey();
                    requireObject(f.getValue(), where);

                    JsonNode iat = f.getValue().get("IAT");
                    JsonNode size = f.getValue().get("Size");
                    if (iat == null || size == null) {
                        throw new SpplogFormatException("traffic.json: flow '" + where + "' lacks IAT and/or Size.");
                    }
                    flows.add(new FlowSpec(tx.getKey(), rx.getKey(), f.getKey(), new FlowSpec.Stats(iat), new FlowSpec.Stats(size)));
                }
            }
        }

        if (flows.isEmpty()) {
            throw new SpplogFormatException("traffic.json does not contain any flows.");
        }
        return flows;
    }

    private static void requireObject(JsonNode n, String where) throws SpplogFormatException {
        if (!n.isObject()) {
            throw new SpplogFormatException("traffic.json: expected an object at '" + where + "'.");
        }
    }
}
