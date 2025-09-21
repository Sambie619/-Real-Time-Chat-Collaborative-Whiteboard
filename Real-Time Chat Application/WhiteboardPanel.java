import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class WhiteboardPanel extends JPanel {
    private List<DrawAction> actions = new ArrayList<>();
    private Point lastPoint = null;
    private PrintWriter out;
    private boolean isEraser = false;

    public WhiteboardPanel(PrintWriter out) {
        this.out = out;
        setBackground(Color.WHITE);

        // Toggle Eraser Button
        JToggleButton eraserButton = new JToggleButton("Eraser");
        eraserButton.addActionListener(e -> isEraser = eraserButton.isSelected());

        this.setLayout(new BorderLayout());
        this.add(eraserButton, BorderLayout.NORTH);

        // Mouse Drawing
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                lastPoint = e.getPoint();
            }

            public void mouseReleased(MouseEvent e) {
                lastPoint = null;
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                Point currentPoint = e.getPoint();
                Color color = isEraser ? Color.WHITE : Color.BLACK;
                int strokeSize = isEraser ? 20 : 2;

                DrawAction action = new DrawAction(lastPoint, currentPoint, color, strokeSize);
                actions.add(action);
                repaint();

                // Send draw command
                if (out != null) {
                    out.println("DRAW " + lastPoint.x + "," + lastPoint.y + "," +
                            currentPoint.x + "," + currentPoint.y + "," +
                            color.getRGB() + "," + strokeSize);
                }

                lastPoint = currentPoint;
            }
        });
    }

    public void receiveDrawCommand(String command) {
        try {
            String[] parts = command.substring(5).split(",");
            int x1 = Integer.parseInt(parts[0]);
            int y1 = Integer.parseInt(parts[1]);
            int x2 = Integer.parseInt(parts[2]);
            int y2 = Integer.parseInt(parts[3]);
            Color color = new Color(Integer.parseInt(parts[4]));
            int strokeSize = parts.length >= 6 ? Integer.parseInt(parts[5]) : 2;

            DrawAction action = new DrawAction(new Point(x1, y1), new Point(x2, y2), color, strokeSize);
            actions.add(action);
            repaint();
        } catch (Exception e) {
            System.out.println("Invalid DRAW command: " + command);
        }
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        for (DrawAction a : actions) {
            g2.setColor(a.color);
            g2.setStroke(new BasicStroke(a.strokeSize));
            g2.drawLine(a.from.x, a.from.y, a.to.x, a.to.y);
        }
    }

    private static class DrawAction {
        Point from, to;
        Color color;
        int strokeSize;

        DrawAction(Point from, Point to, Color color, int strokeSize) {
            this.from = from;
            this.to = to;
            this.color = color;
            this.strokeSize = strokeSize;
        }
    }
}
