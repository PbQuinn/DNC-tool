package org.networkcalculus.dnc.SPPAnalyzer;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Checks meta.json against the parsed flows. meta.json is keyed by traffic family, then IAT/Size,
 * each with n_samples. A null n_samples means "not measured / fixed value" and is not warned about.
 */
final class SampleValidator {
    private static final String[] CHARACTERISTICS = {"IAT", "Size"};

    private SampleValidator() {}

    static final class Warning {
        final String family, characteristic, reason;
        final int affectedFlows;

        Warning(String family, String characteristic, String reason, int affectedFlows) {
            this.family = family;
            this.characteristic = characteristic;
            this.reason = reason;
            this.affectedFlows = affectedFlows;
        }

        String message() {
            return String.format("%s / %s: %s (%d flow%s affected)",
                    family, characteristic, reason, affectedFlows, affectedFlows == 1 ? "" : "s");
        }
    }

    static List<Warning> validate(List<FlowSpec> flows, JsonNode meta, int minSamples) {
        Map<String, Integer> flowsPerFamily = new LinkedHashMap<>();
        for (FlowSpec f : flows) flowsPerFamily.merge(f.type, 1, Integer::sum);

        List<Warning> warnings = new ArrayList<>();
        for (Map.Entry<String, Integer> fam : flowsPerFamily.entrySet()) {
            JsonNode famMeta = meta.get(fam.getKey());
            if (famMeta == null) {
                warnings.add(new Warning(fam.getKey(), "IAT/Size", "no entry in meta.json", fam.getValue()));
                continue;
            }
            for (String c : CHARACTERISTICS) {
                JsonNode n = famMeta.path(c).path("n_samples");
                if (n.isMissingNode()) {
                    warnings.add(new Warning(fam.getKey(), c, "no n_samples in meta.json", fam.getValue()));
                } else if (n.isNumber() && n.asInt() < minSamples) {
                    warnings.add(new Warning(fam.getKey(), c,
                            "only " + n.asInt() + " samples (minimum " + minSamples + ")", fam.getValue()));
                }
                // n_samples == null -> intentionally skipped
            }
        }
        return warnings;
    }
}