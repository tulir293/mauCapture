package net.maunium.maucapture;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.UIManager;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;

import net.maunium.maucapture.swing.JDrawPlate;
import net.maunium.maucapture.swing.JSelectableImage;
import net.maunium.maucapture.uploaders.ImgurUploader;
import net.maunium.maucapture.uploaders.MISUploader;
import net.maunium.maucapture.uploaders.MatrixUploader;
import net.maunium.maucapture.uploaders.Uploader;
import net.maunium.maucapture.util.TransferableImage;

/**
 * MauCapture 2 main class.
 *
 * @author tulir
 * @since 2.0.0
 */
public class MauCapture {
	/**
	 * The Random instance used to generate image names
	 */
	private Random r = new Random(System.nanoTime());
	public static final String[] imageTypes = ImageIO.getWriterFileSuffixes();
	/**
	 * Main font of MauCapture
	 */
	public static final Font lato = createLato();
	/**
	 * Configuration path
	 */
	public static final File config = new File(new File(System.getProperty("user.home")), ".maucapture.json");
	/**
	 * Version string
	 */
	public static final String version = "2.1";
	/**
	 * Main frame
	 */
	private JFrame frame;
	/**
	 * Non-toggle button
	 */
	private JButton capture, preferences, uploadMIS, uploadMatrix, uploadImgur, color;
	/**
	 * Togglebutton (editing)
	 */
	private JToggleButton arrow, rectangle, circle, pencil, text, erase, crop;
	/**
	 * Button panel
	 */
	private JPanel top, side;
	/**
	 * Scroll pane for drawing area
	 */
	private JScrollPane jsp;
	/**
	 * Drawing area
	 */
	private JDrawPlate jdp;

	/**
	 * Config value
	 */
	private String username = "", authtoken = "", url = "", password = "",
			saveLocation = System.getProperty("user.home"), uploadFormat = "png",
			mxAccessToken, matrixURL = "https://matrix.org";
	/**
	 * Config value
	 */
	private boolean savePassword = false, hideImage = false;

	public MauCapture() {
		frame = new JFrame("mauCapture " + version);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLocationRelativeTo(null);
		frame.setIconImage(getIcon("maucapture.png").getImage());
		/*
		 * Add component listener for changing sizes of the button panels and locations of the
		 * buttons on the right side of the top panel.
		 */
		frame.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent evt) {
				int width = frame.getContentPane().getWidth(), height = frame.getContentPane().getHeight();
				if (width < 520 || height < 440) {
					if (width < 520) {
						width = 520;
					}
					if (height < 440) {
						height = 440;
					}
					frame.getContentPane().setPreferredSize(new Dimension(width, height));
					frame.pack();
					return;
				}
				top.setSize(width, 48);
				side.setSize(48, height);
				jsp.setSize(width - 48, height - 48);

				preferences.setLocation(width - 48, 0);
				uploadImgur.setLocation(width - 48 - 1 * 96, 0);
				uploadMatrix.setLocation(width - 48 - 2 * 96, 0);
				uploadMIS.setLocation(width - 48 - 3 * 96, 0);
			}
		});
		/*
		 * Add window listener to save config before closing.
		 */
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				try {
					saveConfig();
				} catch (IOException e1) {
					System.err.println("Failed to save config:");
					e1.printStackTrace();
				}
			}
		});
		/*
		 * Add application-wide key listener for keybinds
		 * 	CTRL+S - Save image to disk
		 *  CTRL+C - Copy image to clipboard
		 *  CTRL+I - Import image from disk
		 *  Escape - Quit MauCapture
		 */
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
			@Override
			public boolean dispatchKeyEvent(KeyEvent e) {
				// Ignore all non-keydown events.
				if (e.getID() != KeyEvent.KEY_PRESSED) {
					return false;
				}
				// Ignore keybinds if the screenshot or main frame is not focused.
				if (!frame.isFocused() && !Screenshot.takingScreenshot) {
					return false;
				}
				if (Screenshot.takingScreenshot) {
					return false;
				}
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					frame.dispose();
					System.exit(0);
					return true;
				}
				if (!e.isControlDown() || e.isAltDown() || e.isShiftDown()) {
					return false;
				}
				if (e.getKeyCode() == KeyEvent.VK_S && jdp.getImage() != null) {
					FileManager.save(MauCapture.this);
				} else if (e.getKeyCode() == KeyEvent.VK_I) {
					FileManager.load(MauCapture.this);
				} else if (e.getKeyCode() == KeyEvent.VK_C && jdp.getImage() != null) {
					Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
					TransferableImage timg = new TransferableImage(jdp.getImage());
					c.setContents(timg, timg);
					JOptionPane.showMessageDialog(
							getFrame(), "The image has been copied to your clipboard.", "Image copied",
							JOptionPane.INFORMATION_MESSAGE);
				} else if (e.getKeyCode() == KeyEvent.VK_U) {
					Uploader.upload(new MISUploader(getFrame(), url, randomize(5), uploadFormat, username, authtoken, hideImage), jdp.getImage());
				} else if (e.getKeyCode() == KeyEvent.VK_M) {
					Uploader.upload(new MatrixUploader(getFrame(), matrixURL, randomize(5) + ".png", mxAccessToken), jdp.getImage());
				} else {
					return false;
				}
				return true;
			}
		});
		frame.setLayout(null);

		top = new JPanel(null);
		top.setLocation(0, 0);

		side = new JPanel(null);
		side.setLocation(0, 48);

		capture = new JButton("New Capture", getIcon("capture.png"));
		capture.setSize(144, 48);
		capture.setLocation(0, 0);
		capture.setToolTipText("Take a new capture");
		capture.setActionCommand("CAPTURE");
		capture.addActionListener((ActionEvent e) -> {
			frame.setVisible(false);
			try {
				Thread.sleep(50);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			Screenshot.takeScreenshot(MauCapture.this);
		});

		preferences = createButton("preferences.png", 48, 48, 0, 0, "Preferences", settings, "PREFS");
		uploadMIS = createButton("mauImageServer.png", 96, 48, 0, 0, "Upload to a mauImageServer", export, "MIS");
		uploadMIS.setText("MIS");
		uploadMatrix = createButton("matrix.png", 96, 48, 0, 0, "Upload to Matrix", export, "MATRIX");
		uploadMatrix.setText("Matrix");
		uploadImgur = createButton("imgur.png", 96, 48, 0, 0, "Upload to Imgur", export, "IMGUR");
		uploadImgur.setText("Imgur");

		color = createButton("color.png", 48, 48, 0, 0 * 48, "Change draw/text color", settings, "COLOR");
		arrow = createToggleButton("arrow.png", 48, 48, 0, 1 * 48, "Draw an arrow", editors, "ARROW");
		rectangle = createToggleButton("rectangle.png", 48, 48, 0, 2 * 48, "Draw a rectangle", editors, "SQUARE");
		circle = createToggleButton("circle.png", 48, 48, 0, 3 * 48, "Draw a circle", editors, "CIRCLE");
		pencil = createToggleButton("pencil.png", 48, 48, 0, 4 * 48, "Freeform drawing", editors, "FREE");
		pencil.setSelected(true);
		text = createToggleButton("text.png", 48, 48, 0, 5 * 48, "Write text", editors, "TEXT");
		erase = createToggleButton("eraser.png", 48, 48, 0, 6 * 48, "Eraser", editors, "ERASE");
		crop = createToggleButton("crop.png", 48, 48, 0, 7 * 48, "Crop the image", cropListener, "CROP");

		jdp = new JDrawPlate(null);
		jdp.setLocation(0, 0);
		jdp.setFont(lato);

		jsp = new JScrollPane();
		jsp.setLocation(48, 48);
		jsp.setViewportView(jdp);
		jsp.setBackground(top.getBackground());

		text.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
				jdp.writeChar(e.getKeyChar());
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}

			@Override
			public void keyPressed(KeyEvent e) {
			}
		});

		top.add(capture);
		top.add(preferences);
		top.add(uploadMIS);
		top.add(uploadMatrix);
		top.add(uploadImgur);

		side.add(color);
		side.add(arrow);
		side.add(rectangle);
		side.add(circle);
		side.add(pencil);
		side.add(text);
		side.add(erase);
		side.add(crop);

		frame.add(top);
		frame.add(side);
		frame.add(jsp);
	}

	public void saveConfig() throws IOException {
		JsonObject config = new JsonObject();
		config.addProperty("username", username);
		config.addProperty("authtoken", authtoken);
		config.addProperty("address", url);
		if (savePassword) {
			config.addProperty("password", password);
		}
		config.addProperty("save-password", savePassword);
		config.addProperty("save-location", saveLocation);
		config.addProperty("upload-format", uploadFormat);
		config.addProperty("matrix-url", matrixURL);
		config.addProperty("matrix-access-token", mxAccessToken);
		JsonWriter writer = new JsonWriter(new FileWriter(MauCapture.config));
		Gson gson = new Gson();
		gson.toJson(config, writer);
		writer.close();
	}

	public void loadConfig() throws FileNotFoundException {
		if (!MauCapture.config.exists()) {
			return;
		}
		JsonParser parser = new JsonParser();
		JsonObject config = parser.parse(new FileReader(MauCapture.config)).getAsJsonObject();
		JsonElement e;

		e = config.get("username");
		if (e != null && e.isJsonPrimitive()) {
			username = e.getAsString();
		}
		e = config.get("authtoken");
		if (e != null && e.isJsonPrimitive()) {
			authtoken = e.getAsString();
		}
		e = config.get("address");
		if (e != null && e.isJsonPrimitive()) {
			url = e.getAsString();
		}
		e = config.get("password");
		if (e != null && e.isJsonPrimitive()) {
			password = e.getAsString();
		}
		e = config.get("save-password");
		if (e != null && e.isJsonPrimitive()) {
			savePassword = e.getAsBoolean();
		}
		e = config.get("save-location");
		if (e != null && e.isJsonPrimitive()) {
			saveLocation = e.getAsString();
		}
		e = config.get("upload-format");
		if (e != null && e.isJsonPrimitive()) {
			uploadFormat = e.getAsString();
		}
		e = config.get("matrix-url");
		if (e != null && e.isJsonPrimitive()) {
			matrixURL = e.getAsString();
		}
		e = config.get("matrix-access-token");
		if (e != null && e.isJsonPrimitive()) {
			mxAccessToken = e.getAsString();
		}
	}

	/**
	 * Create and configure a button.
	 */
	private JButton createButton(String icon, int width, int height, int x, int y, String tooltip, ActionListener aclis,
								 String actionCommand) {
		return configureButton(new JButton(getIcon(icon)), width, height, x, y, tooltip, aclis, actionCommand);
	}

	/**
	 * Create and configure a toggle button.
	 */
	private JToggleButton createToggleButton(String icon, int width, int height, int x, int y, String tooltip,
											 ActionListener aclis, String actionCommand) {
		return configureButton(new JToggleButton(getIcon(icon)), width, height, x, y, tooltip, aclis, actionCommand);
	}

	/**
	 * Configure the given button.
	 *
	 * @return The given button.
	 */
	private <T extends AbstractButton> T configureButton(T button, int width, int height, int x, int y, String tooltip,
														 ActionListener aclis, String actionCommand) {
		button.setFont(lato);
		button.setBorderPainted(false);
		button.setFocusPainted(false);
		button.setSize(width, height);
		button.setLocation(x, y);
		button.setToolTipText(tooltip);
		button.addActionListener(aclis);
		button.setActionCommand(actionCommand);
		return button;
	}

	/**
	 * Open the given buffered image in the MauCapture Editor.
	 */
	public void open(BufferedImage bi) {
		jdp.setForegroundImage(bi);
		jdp.setBackgroundImage(bi);
		jdp.setPreferredSize(new Dimension(bi.getWidth(), bi.getHeight()));
		int prefWidth = 1280, prefHeight = 720;
		if (bi.getWidth() < prefWidth) {
			prefWidth = bi.getWidth();
		}
		if (bi.getHeight() < prefHeight) {
			prefHeight = bi.getHeight();
		}
		jsp.setSize(prefWidth, prefWidth);
		frame.getContentPane().setPreferredSize(new Dimension(prefWidth + 50, prefHeight + 50));
		frame.pack();
		frame.setVisible(true);
	}

	/**
	 * Action Listener for editing buttons (erase, text, arrow, etc)
	 */
	private ActionListener editors = (ActionEvent evt) -> {
		arrow.setSelected(false);
		rectangle.setSelected(false);
		circle.setSelected(false);
		pencil.setSelected(false);
		text.setSelected(false);
		erase.setSelected(false);
		JToggleButton b = (JToggleButton) evt.getSource();
		b.setSelected(true);
		jdp.setDrawMode(JDrawPlate.DrawMode.valueOf(evt.getActionCommand()));
	};

	/**
	 * Action Listener for exporting buttons.
	 */
	private ActionListener export = (ActionEvent evt) -> {
		if (evt.getActionCommand().equals("MIS")) {
			Uploader.upload(new MISUploader(getFrame(), url, randomize(5), uploadFormat, username, authtoken, hideImage), jdp.getImage());
		} else if (evt.getActionCommand().equals("IMGUR")) {
			Uploader.upload(new ImgurUploader(getFrame()), jdp.getImage());
		} else if (evt.getActionCommand().equals("MATRIX")) {
			Uploader.upload(new MatrixUploader(getFrame(), matrixURL, randomize(5) + ".png", mxAccessToken), jdp.getImage());
		}
	};

	/**
	 * Cropping
	 */
	private ActionListener cropListener = new ActionListener() {
		private JSelectableImage si;

		@Override
		public void actionPerformed(ActionEvent e) {
			if (crop.isSelected()) {
				enterCrop();
			} else {
				exitCrop();
			}
		}

		private MouseAdapter siMouse = new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				// Make sure the cropped area is big enough.
				if (si.getSelectWidth() > 20 && si.getSelectHeight() > 5
						|| si.getSelectHeight() > 20 && si.getSelectWidth() > 5) {
					BufferedImage bi =
							jdp.getImage().getSubimage(si.xMin, si.yMin, si.getSelectWidth(), si.getSelectHeight());
					// Area is big enough. Set it as the image for the drawplate.
					jdp.setForegroundImage(bi);
					jdp.setBackgroundImage(
							jdp.getBackgroundImage().getSubimage(
									si.xMin, si.yMin, si.getSelectWidth(), si.getSelectHeight()));
					// Deselect the crop mode button.
					crop.setSelected(false);
					// Exit cropping mode.
					exitCrop();
					jdp.setPreferredSize(new Dimension(bi.getWidth(), bi.getHeight()));
					int prefWidth = 1280, prefHeight = 720;
					if (bi.getWidth() < prefWidth) {
						prefWidth = bi.getWidth();
					}
					if (bi.getHeight() < prefHeight) {
						prefHeight = bi.getHeight();
					}
					jsp.setSize(prefWidth, prefWidth);
					frame.getContentPane().setPreferredSize(new Dimension(prefWidth + 50, prefHeight + 50));
					frame.pack();
				} else {
					// Area too small. Reset selection.
					si.xMin = Integer.MIN_VALUE;
					si.xMax = Integer.MIN_VALUE;
					si.yMin = Integer.MIN_VALUE;
					si.yMax = Integer.MIN_VALUE;
					si.repaint();
				}
			}
		};

		private void enterCrop() {
			// Create a cropping pane with the image from the drawplate.
			si = new JSelectableImage(jdp.getImage());
			si.setSize(jdp.getImage().getWidth(), jdp.getImage().getHeight());
			si.setLocation(48, 48);
			// Add a mouse listener to detect when cropping is finished.
			si.addMouseListener(siMouse);
			// Disable all other editing buttons.
			color.setEnabled(false);
			arrow.setEnabled(false);
			rectangle.setEnabled(false);
			circle.setEnabled(false);
			pencil.setEnabled(false);
			text.setEnabled(false);
			erase.setEnabled(false);
			jsp.setViewportView(si);
			// Repaint the frame to make sure all changes are visible.
			frame.repaint();
		}

		private void exitCrop() {
			jsp.setViewportView(jdp);
			// Enable all other editing buttons.
			color.setEnabled(true);
			arrow.setEnabled(true);
			rectangle.setEnabled(true);
			circle.setEnabled(true);
			pencil.setEnabled(true);
			text.setEnabled(true);
			erase.setEnabled(true);
			si = null;
			// Repaint the frame to make sure all changes are visible.
			frame.repaint();
		}
	};

	/**
	 * Action Listener for the preferences and color buttons.
	 */
	private ActionListener settings = (ActionEvent evt) -> {
		if (evt.getActionCommand().equals("PREFS")) {
			Preferences.preferences(MauCapture.this);
		} else if (evt.getActionCommand().equals("COLOR")) {
			ColorSelector.colorSelector(MauCapture.this);
		}
	};

	/**
	 * Get an icon from the assets.
	 */
	private ImageIcon getIcon(String path) {
		path = "assets/" + path;
		URL url = MauCapture.class.getClassLoader().getResource(path);
		if (url != null) {
			return new ImageIcon(url);
		} else {
			System.err.println("Couldn't find file: " + path);
		}
		return null;
	}

	/**
	 * Get the main frame.
	 */
	public JFrame getFrame() {
		return frame;
	}

	/**
	 * Get the drawing plate.
	 */
	public JDrawPlate getDrawPlate() {
		return jdp;
	}

	/**
	 * Get the mauImageServer address.
	 */
	public String getAddress() {
		return url;
	}

	/**
	 * Set the mauImageServer address.
	 */
	public void setAddress(String url) {
		this.url = url;
	}

	/**
	 * Set the saved password.
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Get the saved password.
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Get whether or not the password should be saved.
	 */
	public boolean savePassword() {
		return savePassword;
	}

	/**
	 * Set whether or not the password should be saved.
	 */
	public void setSavePassword(boolean savePassword) {
		this.savePassword = savePassword;
	}

	public boolean hideImage() {
		return hideImage;
	}

	public void setHideImage(boolean hideImage) {
		this.hideImage = hideImage;
	}

	/**
	 * Get the username that should be used for MIS authentication.
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Set the username that should be used for MIS authentication.
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Try to log in to the configured MIS server using the given username and password.
	 *
	 * @return The authentication token, or a simple error word with "{@code err:}" as the prefix.
	 */
	public String login(String url, String username, String password) {
		String result = MISUploader.login(url, username, password);
		if (!result.startsWith("err:")) {
			authtoken = result;
			this.username = username;
			return "success";
		} else {
			return result;
		}
	}

	/**
	 * Get the saved authentication token.
	 */
	public String getAuthToken() {
		return authtoken;
	}

	public String getMxAccessToken() {
		return mxAccessToken;
	}

	public void setMxAccessToken(String newat) {
		this.mxAccessToken = newat;
	}

	public String getMatrixURL() {
		return matrixURL;
	}

	public void setMatrixURL(String newURL) {
		this.matrixURL = newURL;
	}

	/**
	 * Get the directory which will be open at first when saving (or importing) images to disk.
	 */
	public String getSaveLocation() {
		return saveLocation;
	}

	/**
	 * Set the directory which will be open at first when saving (or importing) images to disk.
	 */
	public void setSaveLocation(String saveLocation) {
		this.saveLocation = saveLocation;
	}

	public void setUploadFormat(String uploadFormat) {
		this.uploadFormat = uploadFormat;
	}

	public String getUploadFormat() {
		return uploadFormat;
	}

	private final char[] randomizeAllowed = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

	/**
	 * Generate a random string matching the regex [a-zA-Z0-9]{{@code chars}}
	 */
	private String randomize(int chars) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < chars; i++) {
			sb.append(randomizeAllowed[r.nextInt(randomizeAllowed.length)]);
		}
		return sb.toString();
	}

	/**
	 * Loads assets/lato.ttf and returns it as an AWT font.
	 */
	private static final Font createLato() {
		try {
			return Font
					.createFont(
							Font.TRUETYPE_FONT,
							MauCapture.class.getClassLoader().getResourceAsStream("assets/lato.ttf"))
					.deriveFont(Font.PLAIN, 13f);
		} catch (Throwable t) {
			t.printStackTrace();
			return new Font(Font.SANS_SERIF, Font.PLAIN, 11);
		}
	}

	public static void main(String[] args) {
		// Use native L&F
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Throwable t) {
		}
		// Create a MauCapture instance.
		MauCapture mc = new MauCapture();
		try {
			// Load config.
			mc.loadConfig();
		} catch (FileNotFoundException e) {
			System.err.println("Failed to read config:");
			e.printStackTrace();
		}
		// Open the screen grabbing view
		Screenshot.takeScreenshot(mc);
	}
}
