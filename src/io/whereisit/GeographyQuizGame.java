package io.whereisit;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

@SuppressWarnings("serial")
public class GeographyQuizGame extends JPanel implements ActionListener {

	public class Card {
		public String front;
		public String back;

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof Card)) {
				return false;
			}
			Card c = (Card) o;
			return (front.equals(c.front) && back.equals(c.back));
		}
	}

	public class Stack {
		public String label;
		public Vector<Card> cards = new Vector<Card>();

		@Override
		public boolean equals(Object o) {
			if (o instanceof Stack) {
				return ((Stack) o).label.equals(this.label);
			} else {
				return false;
			}
		}
	}

	class Country {
		public Vector<Point> border = new Vector<Point>();
		public int extent;
		public String name;
		public Point center;
	}

	Vector<Country> countries = new Vector<Country>();
	Vector<Point> borderDisp = null;
	Vector<Point> quizBorder = null;
	private final Vector<Stack> stacks = new Vector<Stack>();

	BufferedImage br = null;

	String prevCountry = "";

	private final JLabel status;
	JCheckBox[] checkBoxes;
	boolean[] prevCheckStatus;
	JButton nextButton, flipButton;
	Card currentCard = null;
	JRadioButton fronts, backs, both, neither, random;
	boolean front = false;
	private HashSet<Card> drillStack = new HashSet<Card>();

	public void reformatStatusBar() {
		if (currentCard == null && borderDisp == null) {
			return;
		}
		String status = "";
		if (borderDisp != null) {
			status += "In Blue: " + prevCountry + "     ";
		}
		if (currentCard != null) {
			status += "Quiz ";
			String keyString = "";
			if (quizBorder != null) {
				keyString = " (in red)";
			} else {
				keyString = " (no border shown)";
			}
			if (neither.isSelected()) {
				if (quizBorder == null) {
					if (front) {
						status += "Capital: " + currentCard.front;
					} else {
						status += "Province: " + currentCard.back;
					}
				} else {
					status += "Country" + keyString + ": ?";
				}
			} else if (both.isSelected()) {
				status += "Region" + keyString + ": " + currentCard.front
						+ ", " + currentCard.back;
			} else if (front) {
				status += "Capital City" + keyString + ": " + currentCard.front;
			} else {
				status += "Region " + keyString + ": " + currentCard.back;
			}
		}
		this.status.setText(status);
	}

	@Override
	protected void paintComponent(Graphics g) {
		g.drawImage(br, 0, 0, null);
		if (quizBorder != null) {
			g.setColor(Color.red);
			for (Point pi : quizBorder) {
				g.fillRect((int) pi.getX(), (int) pi.getY(), 1, 1);
			}
		}
		if (borderDisp != null) {
			g.setColor(Color.blue);
			for (Point pi : borderDisp) {
				g.fillRect((int) pi.getX(), (int) pi.getY(), 1, 1);
			}
		}
	}

	public GeographyQuizGame(JFrame parent) {
		try {
			br = ImageIO.read(getResource("./src/Newworldmap.png"));
			setPreferredSize(new Dimension(br.getWidth(), br.getHeight()));
			loadCards("./src/capitals.txt");
			loadCountries("./src/borders.txt");
		} catch (IOException ioe) {
			ioe.printStackTrace();
			System.exit(-1);
		}

		status = new JLabel(
				"To begin, select the stacks you want to\n draw from, and whether you want to be quizzed on fronts of cards (capitals),\n backs of cards (states, provinces, countries), both, or neither.  Mouse over a country to get its name");
		JPanel statusPanel = new JPanel();
		statusPanel.add(status);

		parent.getContentPane().setLayout(new BorderLayout());
		parent.getContentPane().add(this, BorderLayout.CENTER);
		parent.getContentPane().add(statusPanel, BorderLayout.SOUTH);
		parent.getContentPane().add(makeButtonPanel(), BorderLayout.WEST);

		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseMoved(MouseEvent me) {
				int x = (int) me.getPoint().getX();
				int y = (int) me.getPoint().getY();
				checkMouseOverCountry(x, y);
			}
		});
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource() == flipButton && currentCard != null) {
			if (front) {
				front = false;
			} else {
				front = true;
			}
			reformatStatusBar();
		} else if (ae.getSource() == nextButton) {
			boolean changed = false;
			for (int i = 0; i < checkBoxes.length; i++) {
				if (checkBoxes[i].isSelected() != prevCheckStatus[i]) {
					changed = true;
					prevCheckStatus[i] = checkBoxes[i].isSelected();
				}

			}
			if (changed || drillStack.size() == 0) {
				drillStack = new HashSet<Card>();
				for (int i = 0; i < checkBoxes.length; i++) {
					if (checkBoxes[i].isSelected()) {
						drillStack.addAll(stacks.get(i).cards);
					}
				}
			}
			if (drillStack.size() == 0) {
				status.setText("You must check some stacks to draw from before clicking next");
				return;
			} else {
				int card = (int) (Math.random() * drillStack.size());
				currentCard = drillStack.toArray(new Card[0])[card];
				drillStack.remove(currentCard);
				quizBorder = new Vector<Point>();
				for (Country ci : countries) {
					if (ci.name.equalsIgnoreCase(currentCard.back)) {
						for (Point pi : ci.border) {
							quizBorder.add(pi);
						}
					}
				}
				if (quizBorder.size() == 0) {
					quizBorder = null;
				} else {
					repaint();
				}
				if (random.isSelected()) {
					if (Math.random() < .5) {
						front = true;
					} else {
						front = false;
					}
				}
			}
			reformatStatusBar();
		} else {
			if (fronts.isSelected()) {
				front = true;
			} else if (backs.isSelected()) {
				front = false;
			} else if (random.isSelected()) {
				if (Math.random() < .5) {
					front = true;
				} else {
					front = false;
				}
			} else if (neither.isSelected()) {
				front = false;
			}
			reformatStatusBar();
		}

	}

	public void checkMouseOverCountry(int x, int y) {
		// int x = (int)arg0.getPoint().getX();
		// int y = (int)arg0.getPoint().getY();
		borderDisp = new Vector<Point>();
		for (Country c : countries) {
			int cx = (int) c.center.getX();
			int cy = (int) c.center.getY();
			double dist = Math.sqrt(Math.pow(cx - x, 2) + Math.pow(cy - y, 2));
			if (dist < c.extent) {
				int minx = Integer.MAX_VALUE;
				int maxx = Integer.MIN_VALUE;
				int miny = minx, maxy = maxx;
				for (Point pi : c.border) {
					if ((int) pi.getX() == x) {
						miny = Math.min((int) pi.getY(), miny);
						maxy = Math.max((int) pi.getY(), maxy);
					}
					if ((int) pi.getY() == y) {
						minx = Math.min((int) pi.getX(), minx);
						maxx = Math.max((int) pi.getX(), maxx);
					}

					borderDisp.add(pi);
				}
				if (minx < x && x < maxx && miny < y && y < maxy) {
					if (!prevCountry.equals(c.name)) {
						prevCountry = c.name;
						for (Country ci : countries) {
							if (ci.name.equals(c.name)
									&& !ci.center.equals(c.center)) {
								for (Point pii : ci.border) {
									borderDisp.add(pii);
								}
							}
						}
						reformatStatusBar();
						repaint();
					}
					return;
				} else {
					borderDisp.clear();
				}
			}
		}
		borderDisp = null;
	}

	public void loadCountries(String borderFile) throws IOException {
		Scanner scan = new Scanner(getResource(borderFile));
		while (scan.hasNextLine()) {
			Country toAdd = new Country();
			toAdd.name = scan.nextLine();
			String points = scan.nextLine();
			String[] pts = points.split(" ");
			int maxx = Integer.MIN_VALUE;
			int minx = Integer.MAX_VALUE;
			int maxy = maxx;
			int miny = minx;
			double cx = 0, cy = 0;
			int numpts = 0;
			for (int i = 0; i < pts.length; i += 2) {
				int x = Integer.parseInt(pts[i]);
				int y = Integer.parseInt(pts[i + 1]);
				maxx = Math.max(x, maxx);
				maxy = Math.max(y, maxy);
				miny = Math.min(y, miny);
				minx = Math.min(x, minx);
				numpts++;
				cx += x;
				cy += y;
				toAdd.border.add(new Point(x, y));
			}
			cx /= numpts;
			cy /= numpts;
			toAdd.center = new Point((int) cx, (int) cy);
			toAdd.extent = Math.max((maxx - minx), (maxy - miny));
			countries.add(toAdd);
		}
		scan.close();
	}

	private InputStream getResource(String path) throws IOException {
		InputStream toRet = new FileInputStream(new File(path));
		return toRet;
		// TODO: eventually gotta fix this to do JAR resources again
		// InputStream toRet = this.getClass().getResourceAsStream(path);
		// if (toRet == null) {
		// toRet = new FileInputStream(new File("Newworldmap.png"));
		// }
		// return toRet;
	}

	public void loadCards(String dataFile) throws IOException {

		Scanner scan = new Scanner(getResource(dataFile));
		Vector<Stack> curStacks = new Vector<Stack>();
		while (scan.hasNextLine()) {
			String line = scan.nextLine();
			String[] arr = line.split("\t");
			Card c = new Card();
			c.front = arr[0];
			c.back = arr[1];
			if (arr.length > 2) {
				System.out.println("new attributes");
				curStacks.removeAllElements();
				for (int i = 2; i < arr.length; i++) {
					Stack s = new Stack();
					System.out.println("label " + arr[i]);
					s.label = arr[i];
					if (stacks.contains(s)) {
						System.out.println("it's old");
						stacks.get(stacks.indexOf(s)).cards.add(c);
						curStacks.add(stacks.get(stacks.indexOf(s)));
					} else {
						System.out.println("it's new");
						s.cards.add(c);
						stacks.add(s);
						curStacks.add(s);
					}
				}
			} else {
				System.out.println("adding " + c.front + " " + c.back
						+ " to the current stacks");
				for (Stack s : curStacks) {
					s.cards.add(c);
				}
			}
		}
		scan.close();

	}

	public JPanel makeButtonPanel() {
		JPanel checkBoxPanel = new JPanel();
		checkBoxPanel.setLayout(new BoxLayout(checkBoxPanel, BoxLayout.Y_AXIS));

		nextButton = new JButton("next");

		flipButton = new JButton("flip");
		nextButton.addActionListener(this);
		flipButton.addActionListener(this);
		checkBoxPanel.add(nextButton);
		checkBoxPanel.add(Box.createRigidArea(new Dimension(0, 4)));
		checkBoxPanel.add(flipButton);
		checkBoxes = new JCheckBox[stacks.size()];
		prevCheckStatus = new boolean[stacks.size()];
		checkBoxPanel.add(Box.createRigidArea(new Dimension(0, 10)));
		for (int i = 0; i < checkBoxes.length; i++) {
			checkBoxes[i] = new JCheckBox(stacks.get(i).label);
			prevCheckStatus[i] = false;
			checkBoxPanel.add(checkBoxes[i]);
		}

		backs = new JRadioButton("show region names", true);
		fronts = new JRadioButton("show capital cities");
		both = new JRadioButton("show both");
		random = new JRadioButton("show random");
		neither = new JRadioButton("show neither");

		fronts.addActionListener(this);
		backs.addActionListener(this);
		both.addActionListener(this);
		random.addActionListener(this);
		neither.addActionListener(this);

		ButtonGroup bg = new ButtonGroup();
		checkBoxPanel.add(Box.createRigidArea(new Dimension(0, 10)));
		bg.add(fronts);
		bg.add(backs);
		bg.add(both);
		bg.add(random);
		bg.add(neither);
		checkBoxPanel.add(fronts);
		checkBoxPanel.add(backs);
		checkBoxPanel.add(both);
		checkBoxPanel.add(random);
		checkBoxPanel.add(neither);

		return checkBoxPanel;

	}

	public static void main(String[] argv) {
		JFrame frame = new JFrame("Countries of the World");
		new GeographyQuizGame(frame);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}
}
