package net.maunium.maucapture;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/**
 * Contains a method that opens the MIS settings dialog.
 *
 * @author tulir
 * @since 2.0.0
 */
public class Preferences {
	public static void preferences(MauCapture host) {
		JDialog frame = new JDialog(host.getFrame(), "MIS Settings");
		frame.setLayout(null);
		frame.setResizable(false);
		frame.setLocationRelativeTo(host.getFrame());
		frame.getContentPane().setPreferredSize(new Dimension(500, 215));
		frame.pack();
		frame.setFont(MauCapture.lato);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent evt) {
				frame.setVisible(false);
				frame.dispose();
			}
		});

		JLabel addrl = new JLabel("Address");
		addrl.setLocation(5, 5);
		addrl.setSize(70, 30);
		addrl.setFont(MauCapture.lato);

		JTextField addr = new JTextField(host.getAddress());
		addr.setLocation(75, 5);
		addr.setSize(170, 30);
		addr.setFont(MauCapture.lato);

		JLabel authtokenl = new JLabel("Auth token");
		authtokenl.setLocation(5, 40);
		authtokenl.setSize(70, 30);
		authtokenl.setFont(MauCapture.lato);

		JTextField authtoken = new JTextField(host.getAuthToken());
		authtoken.setLocation(75, 40);
		authtoken.setSize(170, 30);
		authtoken.setEditable(false);
		authtoken.setFont(MauCapture.lato);

		JLabel usernamel = new JLabel("Username");
		usernamel.setLocation(255, 5);
		usernamel.setSize(60, 30);
		usernamel.setFont(MauCapture.lato);

		JTextField username = new JTextField(host.getUsername());
		username.setLocation(320, 5);
		username.setSize(175, 30);
		username.setFont(MauCapture.lato);

		JLabel passwordl = new JLabel("Password");
		passwordl.setLocation(255, 40);
		passwordl.setSize(60, 30);
		passwordl.setFont(MauCapture.lato);

		JPasswordField password = new JPasswordField(String.valueOf(host.getPassword()));
		password.setLocation(320, 40);
		password.setSize(175, 30);
		password.setFont(MauCapture.lato);

		JCheckBox savePassword = new JCheckBox("Save password (not recommended)", host.savePassword());
		savePassword.setLocation(5, 75);
		savePassword.setSize(240, 30);
		savePassword.setFont(MauCapture.lato);

		JButton login = new JButton("Log in (get auth token)");
		login.setLocation(255, 75);
		login.setSize(240, 30);
		login.addActionListener(e -> {
			if (addr.getText().length() <= 0 || username.getText().length() <= 0 || password.getPassword().length <= 0) {
				JOptionPane.showMessageDialog(frame, "You must fill all the fillable fields to log in.", "Incomplete details", JOptionPane.ERROR_MESSAGE);
				return;
			}
			String status = host.login(addr.getText(), username.getText(), String.valueOf(password.getPassword()));
			switch (status) {
			case "success":
				if (!savePassword.isSelected()) {
					password.setText("");
				}
				authtoken.setText(host.getAuthToken());
				JOptionPane.showMessageDialog(frame, "Successfully logged in as " + username.getText(), "Logged in", JOptionPane.INFORMATION_MESSAGE);
				break;
			case "err:incorrectpassword":
				JOptionPane.showMessageDialog(frame, "The password you entered was incorrect.", "Incorrect password", JOptionPane.ERROR_MESSAGE);
				break;
			case "err:servererror":
				JOptionPane.showMessageDialog(frame, "The server encountered an internal server error.", "Internal server error",
						JOptionPane.ERROR_MESSAGE);
				break;
			case "err:exception":
				JOptionPane.showMessageDialog(frame, "Failed to contact server.", "Connection error", JOptionPane.ERROR_MESSAGE);
				break;
			}
		});
		login.setFont(MauCapture.lato);

		JLabel formatl = new JLabel("Image Upload Format");
		formatl.setSize(130, 30);
		formatl.setLocation(5, 110);
		formatl.setFont(MauCapture.lato);

		JComboBox<String> format = new JComboBox<String>(MauCapture.imageTypes);
		format.setSelectedItem(host.getUploadFormat());
		format.setSize(110, 30);
		format.setLocation(135, 110);
		format.setFont(MauCapture.lato);

		JCheckBox hidden = new JCheckBox("Hide image from search", host.hideImage());
		hidden.setLocation(320, 110);
		hidden.setSize(240, 30);
		hidden.setFont(MauCapture.lato);


		JLabel mxURLl = new JLabel("Matrix");
		mxURLl.setLocation(5, 145);
		mxURLl.setSize(60, 30);
		mxURLl.setFont(MauCapture.lato);

		JTextField mxURL = new JTextField(host.getMatrixURL());
		mxURL.setLocation(75, 145);
		mxURL.setSize(175, 30);
		mxURL.setFont(MauCapture.lato);

		JLabel mxAccessTokenl = new JLabel("Token");
		mxAccessTokenl.setLocation(255, 145);
		mxAccessTokenl.setSize(70, 30);
		mxAccessTokenl.setFont(MauCapture.lato);

		JTextField mxAccessToken = new JTextField(host.getMxAccessToken());
		mxAccessToken.setLocation(320, 145);
		mxAccessToken.setSize(170, 30);
		mxAccessToken.setFont(MauCapture.lato);


		JButton save = new JButton("Save");
		save.setSize(240, 30);
		save.setLocation(5, 180);
		save.addActionListener(e -> {
			host.setAddress(addr.getText());
			if (savePassword.isSelected()) { host.setPassword(String.valueOf(password.getPassword())); }
			host.setSavePassword(savePassword.isSelected());
			host.setHideImage(hidden.isSelected());
			host.setUsername(username.getText());
			host.setUploadFormat((String) format.getSelectedItem());
			host.setMxAccessToken(mxAccessToken.getText());
			host.setMatrixURL(mxURL.getText());
			try {
				host.saveConfig();
			} catch (IOException e1) {
				System.err.println("Failed to save config:");
				e1.printStackTrace();
			}
			frame.dispose();
		});
		save.setFont(MauCapture.lato);

		JButton cancel = new JButton("Cancel");
		cancel.setSize(240, 30);
		cancel.setLocation(255, 180);
		cancel.addActionListener(e -> frame.dispose());
		cancel.setFont(MauCapture.lato);

		frame.add(addr);
		frame.add(addrl);
		frame.add(authtoken);
		frame.add(authtokenl);
		frame.add(username);
		frame.add(usernamel);
		frame.add(password);
		frame.add(passwordl);
		frame.add(savePassword);
		frame.add(login);
		frame.add(format);
		frame.add(formatl);
		frame.add(hidden);
		frame.add(mxURLl);
		frame.add(mxURL);
		frame.add(mxAccessTokenl);
		frame.add(mxAccessToken);
		frame.add(save);
		frame.add(cancel);
		frame.setVisible(true);
	}
}
