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

// FILE METHODS LARGELY GENERATED USING CLAUDE Sonnet 5.5

public class SppDeviceGraph extends DeviceGraph {

    static Curve buildServiceCurve() {
        System.out.println("Building Service Curve for device");
        return CurveFactory.createTokenBucket(new Number(10e6), new Number(0.1 * 0.1e6));
    }

    static Curve buildArrivalCurve(FlowSpec flowInstance) {
        System.out.printf("Building Arrival Curve for flow instance of %s with IAT(%s) and Size (%s)\n", flowInstance.type, flowInstance.iat, flowInstance.size);
        return CurveFactory.createTokenBucket(new Number(0.1e6), new Number(0.1 * 0.1e6));
    }


    private static final String SWITCH_NAME = "Switch";

    // ------------------------------------------------------------------
    // Config parsing
    // ------------------------------------------------------------------

    static DeviceGraph fromFlowSpecs(List<FlowSpec> flows) throws Exception {

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
