package org.yourcompany.yourproject.gui.util;

import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import java.util.concurrent.Callable;

public final class GuiTaskRunner {

    private GuiTaskRunner() {
    }

    public static void run(String title, JTextArea outputArea, Callable<String> task, JComponent... controls) {
        setEnabled(controls, false);
        outputArea.append("\n>>> " + title + "\n");

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return task.call();
            }

            @Override
            protected void done() {
                try {
                    outputArea.append(get());
                } catch (Exception e) {
                    outputArea.append("ERROR: " + e.getMessage() + "\n");
                } finally {
                    setEnabled(controls, true);
                }
            }
        }.execute();
    }

    private static void setEnabled(JComponent[] controls, boolean enabled) {
        for (JComponent control : controls) {
            if (control != null) {
                control.setEnabled(enabled);
            }
        }
    }
}
