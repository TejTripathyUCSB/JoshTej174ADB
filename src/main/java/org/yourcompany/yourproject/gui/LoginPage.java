package org.yourcompany.yourproject.gui;

import org.yourcompany.yourproject.AuthService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.function.Consumer;

public class LoginPage extends JFrame {
    private final AuthService authService = new AuthService();

    public LoginPage(Consumer<AuthService.Session> onLoginSuccess) {
        super("174A PROJECT: Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(420, 240);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        JTextField idField = new JTextField(16);
        JPasswordField pwField = new JPasswordField(16);
        JLabel status = new JLabel(" ");
        status.setForeground(new Color(170, 0, 0));
        status.setBorder(BorderFactory.createEmptyBorder(0, 16, 12, 16));

        g.gridx = 0; g.gridy = 0; g.weightx = 0; form.add(new JLabel("Customer ID:"), g);
        g.gridx = 1; g.weightx = 1; form.add(idField, g);
        g.gridx = 0; g.gridy = 1; g.weightx = 0; form.add(new JLabel("Password:"), g);
        g.gridx = 1; g.weightx = 1; form.add(pwField, g);

        JButton loginButton = new JButton("Log In");
        g.gridx = 1; g.gridy = 2; g.anchor = GridBagConstraints.EAST; g.fill = GridBagConstraints.NONE;
        form.add(loginButton, g);

        add(form, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);

        Runnable submit = () -> {
            status.setText(" ");
            loginButton.setEnabled(false);
            final String id = idField.getText().trim();
            final String pw = new String(pwField.getPassword());

            new SwingWorker<AuthService.Session, Void>() {
                @Override
                protected AuthService.Session doInBackground() {
                    return authService.login(id, pw);
                }

                @Override
                protected void done() {
                    try {
                        AuthService.Session session = get();
                        if (session == null) {
                            status.setText("Invalid customer ID or password.");
                            loginButton.setEnabled(true);
                            pwField.setText("");
                            return;
                        }
                        dispose();
                        onLoginSuccess.accept(session);
                    } catch (Exception ex) {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        status.setText("Login error: " + cause.getMessage());
                        loginButton.setEnabled(true);
                    }
                }
            }.execute();
        };

        loginButton.addActionListener(e -> submit.run());
        getRootPane().setDefaultButton(loginButton);
    }
}
