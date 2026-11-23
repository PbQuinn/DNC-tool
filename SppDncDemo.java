/*
 * This file is NOT part of the Deterministic Network Calculator (DNC).
 */

package org.networkcalculus.dnc.SPPAnalyzer;

import org.networkcalculus.dnc.AnalysisConfig;
import org.networkcalculus.dnc.backend.Curve;
import org.networkcalculus.dnc.backend.CurveFactory;
import org.networkcalculus.dnc.backend.number.Number;
import org.networkcalculus.dnc.network.device_graph.Device;
import org.networkcalculus.dnc.network.device_graph.DeviceGraph;
import org.networkcalculus.dnc.network.device_graph.acyclic_routing_strategy.SpanningTreeProtocol;
import org.networkcalculus.dnc.network.device_graph.path_routing_strategy.ShortestPath;
import org.networkcalculus.dnc.network.server_graph.Flow;
import org.networkcalculus.dnc.network.server_graph.ServerGraph;
import org.networkcalculus.dnc.tandem.analyses.SeparateFlowAnalysis;
import org.networkcalculus.dnc.tandem.analyses.TotalFlowAnalysis;

import java.io.File;
import java.util.ArrayList;

public class SppDncDemo {

    public SppDncDemo() {
    }

    public static void main(String[] args) {
        SppDncDemo demo = new SppDncDemo();

        try {
            demo.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() throws Exception {
        Curve service_curve = CurveFactory.createRateLatency(new Number(100.0e6), new Number(0.01));
        Curve max_service_curve = CurveFactory.createRateLatency(new Number(100.0e6), new Number(0.001));
        Curve arrival_curve = CurveFactory.createTokenBucket(new Number(0.1e6), new Number(0.1 * 0.1e6));

//        DeviceGraph dg = new DeviceGraph();
        DeviceGraph dg = SppDeviceGraph.fromFile(new File("traffic.json"));

//        // ============ Nodes ==============
//        Device DPMain = dg.addDevice("DPMain");
//        Device DSP = dg.addDevice("DSP");
//
//        ArrayList<Device> PUs = new ArrayList<Device>();
//        ArrayList<Device> RCs = new ArrayList<Device>();
//
//        for (int i=0; i < 7; i++) {
//            PUs.add(dg.addDevice(String.format("PU_%d", i)));
//        }
//        for (int i=0; i < 5 ; i++) {
//            RCs.add(dg.addDevice(String.format("RC_%d", i)));
//        }
//
//        Device networkSwitch = dg.addDevice("Switch");
//
//
//        // ============ Links ==============
//        dg.addLink(DPMain, networkSwitch, service_curve);
//        dg.addLink(networkSwitch, DPMain, service_curve);
//        dg.addLink(DSP, networkSwitch, service_curve);
//        dg.addLink(networkSwitch, DSP, service_curve);
//
//        for (Device pu : PUs) {
//            dg.addLink(pu, networkSwitch, service_curve);
//            dg.addLink(networkSwitch, pu, service_curve);
//        }
//        for (Device rc: RCs) {
//            dg.addLink(rc, networkSwitch, service_curve);
//            dg.addLink(networkSwitch, rc, service_curve);
//        }
//
//
//        // ============ Traffic demands ==============
//        for (Device pu : PUs) {
//            // Send to all RCs
//            for (Device rc : RCs) {
//                dg.addTrafficDemand(String.format("Halftones_%s->%s", pu.getAlias(), rc.getAlias()), pu, rc, arrival_curve);
//            }
//            // Send to all other PUs
//            for (Device pu_other : PUs) {
//                if (!pu.equals(pu_other)) {
//                    dg.addTrafficDemand(String.format("Contones_%s->%s", pu.getAlias(), pu_other.getAlias()), pu, pu_other, arrival_curve);
//                }
//            }
//
//            dg.addTrafficDemand(String.format("MiniBitmaps_%s", pu.getAlias()), pu, DSP, arrival_curve);
//            dg.addTrafficDemand(String.format("TableUpdates_%s", pu.getAlias()), DSP, pu, arrival_curve);
//            dg.addTrafficDemand(String.format("PDLs_%s", pu.getAlias()), DPMain, pu, arrival_curve);
//        }
//        for (Device rc : RCs) {
//            dg.addTrafficDemand(String.format("PAINt_%s", rc.getAlias()), rc, DSP, arrival_curve);
//            dg.addTrafficDemand(String.format("TableUpdates_%s", rc.getAlias()), DSP, rc, arrival_curve);
//        }

        System.out.println(dg.toString());


        ServerGraph sg = dg.convertToServerGraph(new SpanningTreeProtocol(), new ShortestPath());


        for (Flow flow_of_interest : sg.getFlows()) {

            System.out.println("Flow of interest : " + flow_of_interest.toString());
            System.out.println();

            // Analyze the server graph
            // TFA ARB
            AnalysisConfig cfg_arb = new AnalysisConfig();
            cfg_arb.enforceMultiplexing(AnalysisConfig.MultiplexingEnforcement.GLOBAL_ARBITRARY);
            TotalFlowAnalysis tfa_arb = new TotalFlowAnalysis(sg, cfg_arb);
            System.out.println("--- Total Flow Analysis ARB ---");
            try {
                tfa_arb.performAnalysis(flow_of_interest);
                System.out.println("delay bound     : " + tfa_arb.getDelayBound());
                System.out.println("     per server : " + tfa_arb.getServerDelayBoundMapString());
                System.out.println("backlog bound   : " + tfa_arb.getBacklogBound());
                System.out.println("     per server : " + tfa_arb.getServerBacklogBoundMapString());
                System.out.println("alpha per server: " + tfa_arb.getServerAlphasMapString());
            } catch (Exception e) {
                System.out.println("TFA analysis failed with: " + e.getMessage());
                e.printStackTrace();
            }

            System.out.println();

            // TFA FIFO
            AnalysisConfig cfg_fifo = new AnalysisConfig();
            cfg_fifo.enforceMultiplexing(AnalysisConfig.MultiplexingEnforcement.GLOBAL_FIFO);
            TotalFlowAnalysis tfa_fifo = new TotalFlowAnalysis(sg, cfg_fifo);
            System.out.println("--- Total Flow Analysis FIFO ---");
            try {
                tfa_fifo.performAnalysis(flow_of_interest);
                System.out.println("delay bound     : " + tfa_fifo.getDelayBound());
                System.out.println("     per server : " + tfa_fifo.getServerDelayBoundMapString());
                System.out.println("backlog bound   : " + tfa_fifo.getBacklogBound());
                System.out.println("     per server : " + tfa_fifo.getServerBacklogBoundMapString());
                System.out.println("alpha per server: " + tfa_fifo.getServerAlphasMapString());
            } catch (Exception e) {
                System.out.println("TFA analysis failed with: " + e.getMessage());
                e.printStackTrace();
            }

            System.out.println();

            // SFA ARB
            SeparateFlowAnalysis sfa_arb = new SeparateFlowAnalysis(sg, cfg_arb);
            System.out.println("--- Separated Flow Analysis ARB ---");
            try {
                sfa_arb.performAnalysis(flow_of_interest);
                System.out.println("e2e SFA SCs     : " + sfa_arb.getLeftOverServiceCurves());
                System.out.println("     per server : " + sfa_arb.getServerLeftOverBetasMapString());
                System.out.println("xtx per server  : " + sfa_arb.getServerAlphasMapString());
                System.out.println("delay bound     : " + sfa_arb.getDelayBound());
                System.out.println("backlog bound   : " + sfa_arb.getBacklogBound());
            } catch (Exception e) {
                System.out.println("SFA analysis failed with: " + e.getMessage());
                e.printStackTrace();
            }

            System.out.println();

            // SFA FIFO
            System.out.println("--- Separated Flow Analysis FIFO ---");
            SeparateFlowAnalysis sfa_fifo = new SeparateFlowAnalysis(sg, cfg_fifo);

            try {
                sfa_fifo.performAnalysis(flow_of_interest);
                System.out.println("e2e SFA SCs     : " + sfa_fifo.getLeftOverServiceCurves());
                System.out.println("     per server : " + sfa_fifo.getServerLeftOverBetasMapString());
                System.out.println("xtx per server  : " + sfa_fifo.getServerAlphasMapString());
                System.out.println("delay bound     : " + sfa_fifo.getDelayBound().toDouble());
                System.out.println("backlog bound   : " + sfa_fifo.getBacklogBound().toDouble());
            } catch (Exception e) {
                System.out.println("SFA analysis failed with: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println();

        }
    }
}