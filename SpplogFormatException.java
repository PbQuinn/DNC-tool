package org.networkcalculus.dnc.SPPAnalyzer;

/** Thrown when a .spplog file does not have the expected structure. Shown to the user as an error popup. */
class SpplogFormatException extends Exception {
    SpplogFormatException(String message) {
        super(message);
    }

    SpplogFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}