package org.networkcalculus.dnc.SPPAnalyzer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.networkcalculus.dnc.backend.Curve;
import org.networkcalculus.dnc.backend.CurveFactory;
import org.networkcalculus.dnc.backend.number.Number;
import org.networkcalculus.dnc.network.device_graph.Device;
import org.networkcalculus.dnc.network.device_graph.DeviceGraph;

import java.io.File;
import java.util.*;

public class SppDeviceGraph extends DeviceGraph {

    static Curve buildServiceCurve() {
        System.out.println("Building Service Curve for device");
        return CurveFactory.createTokenBucket(new Number(0.1e6), new Number(0.1 * 0.1e6));
    }

    static Curve buildArrivalCurve(FlowSpec flowInstance) {
        System.out.printf("Building Arrival Curve for flow instance of %s with IAT(%s) and Size (%s)\n", flowInstance.type, flowInstance.iat, flowInstance.size);
        return CurveFactory.createTokenBucket(new Number(0.1e6), new Number(0.1 * 0.1e6));
    }

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

    /** One instance of a flow (TX, RX, flow type), a triple from the config. */
    static final class FlowSpec {
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

    private static final String SWITCH_NAME = "Switch";

    // ------------------------------------------------------------------
    // Config parsing
    // ------------------------------------------------------------------

    /** Parses  TX -> RX -> flowType -> {IAT, Size} -> {99pct, mean, std}. */
    static List<FlowSpec> loadFlows(File configFile) throws Exception {
        JsonNode root = new ObjectMapper().readTree(configFile);
        List<FlowSpec> flows = new ArrayList<>();

        for (Iterator<Map.Entry<String, JsonNode>> txIt = root.fields(); txIt.hasNext(); ) {
            Map.Entry<String, JsonNode> tx = txIt.next();

            for (Iterator<Map.Entry<String, JsonNode>> rxIt = tx.getValue().fields(); rxIt.hasNext(); ) {
                Map.Entry<String, JsonNode> rx = rxIt.next();

                // The config contains TX == RX pairs (e.g. "2" -> "2"); there is no network path for those.
                if (tx.getKey().equals(rx.getKey())) {
                    System.out.printf("Skipping self-pair %s -> %s%n", tx.getKey(), rx.getKey());
                    continue;
                }

                for (Iterator<Map.Entry<String, JsonNode>> fIt = rx.getValue().fields(); fIt.hasNext(); ) {
                    Map.Entry<String, JsonNode> f = fIt.next();
                    flows.add(new FlowSpec(
                            tx.getKey(), rx.getKey(), f.getKey(),
                            new Stats(f.getValue().path("IAT")),
                            new Stats(f.getValue().path("Size"))));
                }
            }
        }
        return flows;
    }

    static DeviceGraph fromFile(File spp_config_file) throws Exception {
        List<FlowSpec> flows = loadFlows(spp_config_file);

        DeviceGraph dg = new DeviceGraph();
        Curve serviceCurve = buildServiceCurve();

        // Every name that appears as TX or RX is an end device (insertion order preserved).
        Set<String> names = new LinkedHashSet<>();
        for (FlowSpec f : flows) {
            names.add(f.tx);
            names.add(f.rx);
        }

        Map<String, Device> devices = new LinkedHashMap<>();
        for (String name : names) {
            devices.put(name, dg.addDevice(name));
        }
        Device sw = dg.addDevice(SWITCH_NAME);

        // Star topology: bidirectional link between every device and the switch
        for (Device d : devices.values()) {
            dg.addLink(d, sw, serviceCurve);
            dg.addLink(sw, d, serviceCurve);
        }

        // One traffic demand per flow
        for (FlowSpec f : flows) {
            System.out.printf("%-45s IAT[%s]  Size[%s]%n", f.alias(), f.iat, f.size);
            dg.addTrafficDemand(f.alias(), devices.get(f.tx), devices.get(f.rx), buildArrivalCurve(f));
        }

        return dg;
    }
}
