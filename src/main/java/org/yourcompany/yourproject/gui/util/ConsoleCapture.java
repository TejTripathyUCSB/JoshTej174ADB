package org.yourcompany.yourproject.gui.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public final class ConsoleCapture {

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }

    private ConsoleCapture() {
    }

    public static synchronized String capture(ThrowingRunnable action) throws Exception {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream tempOut = new PrintStream(buffer, true, StandardCharsets.UTF_8);

        try {
            System.setOut(tempOut);
            action.run();
        } finally {
            System.out.flush();
            System.setOut(originalOut);
            tempOut.close();
        }

        String output = buffer.toString(StandardCharsets.UTF_8);
        if (output.isBlank()) {
            return "Done.\n";
        }
        return output;
    }
}
