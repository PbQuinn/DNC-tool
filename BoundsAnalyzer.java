package org.networkcalculus.dnc.SPPAnalyzer;

import org.networkcalculus.dnc.AnalysisConfig;
import org.networkcalculus.dnc.network.server_graph.Flow;
import org.networkcalculus.dnc.network.server_graph.ServerGraph;
import org.networkcalculus.dnc.tandem.analyses.SeparateFlowAnalysis;
import org.networkcalculus.dnc.tandem.analyses.TotalFlowAnalysis;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Runs TFA/SFA x ARB/FIFO for every flow and collects the delay and backlog bounds. */
final class BoundsAnalyzer {

    enum Method {
        TFA_FIFO(true, true), SFA_FIFO(false, true);

        final boolean tfa, fifo;

        Method(boolean tfa, boolean fifo) {
            this.tfa = tfa;
            this.fifo = fifo;
        }
    }

    /** Delay and backlog bound; null values mean the analysis failed. */
    static final class Bounds {
        final Double delay, backlog;

        Bounds(Double delay, Double backlog) {
            this.delay = delay;
            this.backlog = backlog;
        }

        static final Bounds FAILED = new Bounds(null, null);
    }

    static final class FlowResult {
        final FlowSpec spec;
        final Map<Method, Bounds> bounds = new EnumMap<>(Method.class);

        FlowResult(FlowSpec spec) {
            this.spec = spec;
        }
    }

    private final ServerGraph sg;
    private final AnalysisConfig cfgArb = new AnalysisConfig();
    private final AnalysisConfig cfgFifo = new AnalysisConfig();

    BoundsAnalyzer(ServerGraph sg) {
        this.sg = sg;
        cfgArb.enforceMultiplexing(AnalysisConfig.MultiplexingEnforcement.GLOBAL_ARBITRARY);
        cfgFifo.enforceMultiplexing(AnalysisConfig.MultiplexingEnforcement.GLOBAL_FIFO);
    }

    /** @param specsByAlias maps traffic demand alias -> FlowSpec, to relate DNC flows back to the config */
    List<FlowResult> analyzeAll(Map<String, FlowSpec> specsByAlias) {
        List<FlowResult> results = new ArrayList<>();
        int i = 0, total = sg.getFlows().size();

        for (Flow foi : sg.getFlows()) {
            i++;
            FlowSpec spec = specsByAlias.get(foi.getAlias());
            if (spec == null) {
                System.err.println("No config entry for flow alias '" + foi.getAlias() + "', skipping.");
                continue;
            }
            System.out.printf("[%d/%d] %s%n", i, total, foi.getAlias());

            FlowResult r = new FlowResult(spec);
            // Run experiment for each method
            for (Method m : Method.values()) {
                System.out.printf("Running %s%n", m.toString()); // Debug
                r.bounds.put(m, run(m, foi));
            }
            results.add(r);
        }
        return results;
    }

    private Bounds run(Method m, Flow foi) {
        AnalysisConfig cfg = m.fifo ? cfgFifo : cfgArb;
        try {
            System.out.println("Creating Analysis"); // Debug
            if (m.tfa) {
                TotalFlowAnalysis a = new TotalFlowAnalysis(sg, cfg);
                System.out.println("Performing Analysis"); // Debug
                a.performAnalysis(foi);
                return new Bounds(a.getDelayBound().toDouble(), a.getBacklogBound().toDouble());
            } else {
                SeparateFlowAnalysis a = new SeparateFlowAnalysis(sg, cfg);
                System.out.println("Performing Analysis"); // Debug
                a.performAnalysis(foi);
                return new Bounds(a.getDelayBound().toDouble(), a.getBacklogBound().toDouble());
            }
        } catch (Exception e) {
            System.err.println(m + " failed for " + foi.getAlias() + ": " + e.getMessage());
            return Bounds.FAILED;
        }
    }
}
