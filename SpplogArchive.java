package org.networkcalculus.dnc.SPPAnalyzer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * A .spplog file is a renamed ZIP archive that must contain traffic.json and meta.json
 * (either at the root or inside a single top-level folder).
 */
final class SpplogArchive {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    final File file;
    final JsonNode traffic;
    final JsonNode meta;

    private SpplogArchive(File file, JsonNode traffic, JsonNode meta) {
        this.file = file;
        this.traffic = traffic;
        this.meta = meta;
    }

    static SpplogArchive open(File file) throws SpplogFormatException {
        try (ZipFile zip = new ZipFile(file)) {
            JsonNode traffic = readJson(zip, "traffic.json");
            JsonNode meta = readJson(zip, "meta.json");
            return new SpplogArchive(file, traffic, meta);
        } catch (ZipException e) {
            throw new SpplogFormatException("'" + file.getName() + "' is not a valid ZIP-based .spplog file.", e);
        } catch (IOException e) {
            throw new SpplogFormatException("Could not read '" + file.getName() + "': " + e.getMessage(), e);
        }
    }

    private static JsonNode readJson(ZipFile zip, String name) throws IOException, SpplogFormatException {
        ZipEntry best = null;
        int bestDepth = Integer.MAX_VALUE;

        for (Enumeration<? extends ZipEntry> en = zip.entries(); en.hasMoreElements(); ) {
            ZipEntry e = en.nextElement();
            String path = e.getName();
            if (e.isDirectory() || path.startsWith("__MACOSX/")) continue;

            String base = path.substring(path.lastIndexOf('/') + 1);
            if (!base.equals(name)) continue;

            int depth = path.length() - path.replace("/", "").length();
            if (depth < bestDepth) {   // prefer the shallowest match
                best = e;
                bestDepth = depth;
            }
        }

        if (best == null) {
            throw new SpplogFormatException("The archive does not contain a '" + name + "' file.");
        }

        try (InputStream in = zip.getInputStream(best)) {
            JsonNode root = MAPPER.readTree(in);
            if (root == null || !root.isObject()) {
                throw new SpplogFormatException("'" + name + "' must contain a JSON object at top level.");
            }
            return root;
        } catch (JsonProcessingException e) {
            throw new SpplogFormatException("'" + name + "' is not valid JSON: " + e.getOriginalMessage(), e);
        }
    }
}