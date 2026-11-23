package org.networkcalculus.dnc.SPPAnalyzer;

import org.networkcalculus.dnc.network.device_graph.DeviceGraph;
import org.networkcalculus.dnc.network.device_graph.acyclic_routing_strategy.SpanningTreeProtocol;
import org.networkcalculus.dnc.network.device_graph.path_routing_strategy.ShortestPath;
import org.networkcalculus.dnc.network.server_graph.ServerGraph;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Entry point: select .spplog files -> validate -> acknowledge warnings -> analyze -> CSV next to each input.
 * Default minimum sample count: -Dspp.minSamples=N (default 30), editable in the first dialog.
 */
public class SppAnalyzerApp {
    private static final int DEFAULT_MIN_SAMPLES = 30;

    public static void main(String[] args) {
        Integer minSamples = DEFAULT_MIN_SAMPLES;

        File[] files = Dialogs.chooseSpplogFiles();
        if (files.length == 0) return;

        int ok = 0;
        for (File f : files) {
            if (process(f, minSamples)) ok++;
        }
        Dialogs.showInfo("Done", ok + " of " + files.length + " file(s) processed.");
        System.exit(0);   // Swing keeps the JVM alive otherwise
    }

    private static boolean process(File spplog, int minSamples) {
        try {
            // 1. Structure check (error popup on failure, via SpplogFormatException)
            SpplogArchive archive = SpplogArchive.open(spplog);
            List<FlowSpec> flows = TrafficParser.parse(archive.traffic);

            // 2. Sample count check (warning popup)
            List<SampleValidator.Warning> warnings = SampleValidator.validate(flows, archive.meta, minSamples);
            if (!warnings.isEmpty()) {
                List<String> lines = warnings.stream().map(SampleValidator.Warning::message).collect(Collectors.toList());
                if (!Dialogs.confirmWarnings(spplog.getName(), lines)) {
                    System.out.println("Skipped " + spplog.getName());
                    return false;
                }
            }

            // 3. Build network + analyze
            System.out.println("Analyzing " + spplog.getName() + " (" + flows.size() + " flows)");
            DeviceGraph dg = SppDeviceGraph.fromFlowSpecs(flows);
            ServerGraph sg = dg.convertToServerGraph(new SpanningTreeProtocol(), new ShortestPath());

            Map<String, FlowSpec> byAlias = new LinkedHashMap<>();
            for (FlowSpec f : flows) byAlias.put(f.alias(), f);

            List<BoundsAnalyzer.FlowResult> results = new BoundsAnalyzer(sg).analyzeAll(byAlias);

            // 4. CSV
            File out = new File(spplog.getParentFile(), spplog.getName().replaceFirst("(?i)\\.spplog$", "") + "_NC_Analysis.csv");
            CsvResultWriter.write(out, results);
            System.out.println("Wrote " + out.getAbsolutePath());
            return true;

        } catch (SpplogFormatException e) {
            Dialogs.showError("Invalid .spplog file: " + spplog.getName(), e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            Dialogs.showError("Analysis failed: " + spplog.getName(), String.valueOf(e));
        }
        return false;
    }
}