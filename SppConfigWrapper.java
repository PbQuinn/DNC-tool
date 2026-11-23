package org.networkcalculus.dnc.SPPAnalyzer;

import org.networkcalculus.dnc.AnalysisConfig;
import org.networkcalculus.dnc.network.device_graph.DeviceGraph;
import org.networkcalculus.dnc.network.device_graph.acyclic_routing_strategy.SpanningTreeProtocol;
import org.networkcalculus.dnc.network.device_graph.path_routing_strategy.ShortestPath;
import org.networkcalculus.dnc.network.server_graph.Flow;
import org.networkcalculus.dnc.network.server_graph.ServerGraph;
import org.networkcalculus.dnc.tandem.analyses.SeparateFlowAnalysis;
import org.networkcalculus.dnc.tandem.analyses.TotalFlowAnalysis;

import java.io.File;


public class SppConfigWrapper {

    static void runTfa(ServerGraph sg, AnalysisConfig cfg, String label, Flow foi) {
        System.out.println("--- Total Flow Analysis " + label + " ---");
        try {
            TotalFlowAnalysis tfa = new TotalFlowAnalysis(sg, cfg);
            tfa.performAnalysis(foi);
            System.out.println("delay bound     : " + tfa.getDelayBound());
            System.out.println("     per server : " + tfa.getServerDelayBoundMapString());
            System.out.println("backlog bound   : " + tfa.getBacklogBound());
            System.out.println("     per server : " + tfa.getServerBacklogBoundMapString());
            System.out.println("alpha per server: " + tfa.getServerAlphasMapString());
        } catch (Exception e) {
            System.out.println("TFA analysis failed with: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println();
    }

    static void runSfa(ServerGraph sg, AnalysisConfig cfg, String label, Flow foi) {
        System.out.println("--- Separated Flow Analysis " + label + " ---");
        try {
            SeparateFlowAnalysis sfa = new SeparateFlowAnalysis(sg, cfg);
            sfa.performAnalysis(foi);
            System.out.println("e2e SFA SCs     : " + sfa.getLeftOverServiceCurves());
            System.out.println("     per server : " + sfa.getServerLeftOverBetasMapString());
            System.out.println("xtx per server  : " + sfa.getServerAlphasMapString());
            System.out.println("delay bound     : " + sfa.getDelayBound());
            System.out.println("backlog bound   : " + sfa.getBacklogBound());
        } catch (Exception e) {
            System.out.println("SFA analysis failed with: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println();
    }

    static void analyze(ServerGraph sg) {
        AnalysisConfig cfgArb = new AnalysisConfig();
        cfgArb.enforceMultiplexing(AnalysisConfig.MultiplexingEnforcement.GLOBAL_ARBITRARY);
        AnalysisConfig cfgFifo = new AnalysisConfig();
        cfgFifo.enforceMultiplexing(AnalysisConfig.MultiplexingEnforcement.GLOBAL_FIFO);

        for (Flow foi : sg.getFlows()) {
            System.out.println("Flow of interest : " + foi + "\n");
            runTfa(sg, cfgArb, "ARB", foi);
            runTfa(sg, cfgFifo, "FIFO", foi);
            runSfa(sg, cfgArb, "ARB", foi);
            runSfa(sg, cfgFifo, "FIFO", foi);
        }
    }

}