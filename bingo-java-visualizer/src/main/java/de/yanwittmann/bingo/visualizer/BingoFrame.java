package de.yanwittmann.bingo.visualizer;

import de.yanwittmann.bingo.BingoBoard;
import de.yanwittmann.bingo.generator.BingoConfiguration;
import de.yanwittmann.bingo.generator.BingoGenerator;
import de.yanwittmann.bingo.generator.Category;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Random;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class BingoFrame extends JFrame {

    private final JPanel bingoGridPanel;
    private final JPanel toolBarPanel;
    private final JTextField seedField;
    private final JTextField searchField;
    private final JButton regenerateButton;

    public BingoFrame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Bingo");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        bingoGridPanel = new JPanel();
        bingoGridPanel.setLayout(new GridLayout(5, 5));
        bingoGridPanel.add(new JLabel("Loading..."), 0, 0);
        add(bingoGridPanel, BorderLayout.NORTH);

        toolBarPanel = new JPanel();
        toolBarPanel.setLayout(new GridLayout(1, 2));
        add(toolBarPanel, BorderLayout.SOUTH);

        seedField = new JTextField();
        seedField.setText("");
        seedField.setToolTipText("Leave empty for random seed");
        toolBarPanel.add(seedField, 0, 0);

        regenerateButton = new JButton("Regenerate");
        regenerateButton.addActionListener(e -> {
            try {
                generateAndShow();
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            }
        });
        toolBarPanel.add(regenerateButton, 0, 1);

        searchField = new JTextField();
        searchField.setText("");
        searchField.setToolTipText("Search for a word");
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                searchFor(searchField.getText());
            }
        });
        toolBarPanel.add(searchField, 0, 2);

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK), "search");
        getRootPane().getActionMap().put("search", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchField.requestFocus();
            }
        });
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.CTRL_DOWN_MASK), "generate");
        getRootPane().getActionMap().put("generate", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    generateAndShow();
                } catch (FileNotFoundException e1) {
                    e1.printStackTrace();
                }
            }
        });

        setVisible(true);
    }

    private void searchFor(String text) {
        for (Component component : bingoGridPanel.getComponents()) {
            if (component instanceof JLabel) {
                JLabel label = (JLabel) component;
                if (searchMatches(text, label)) {
                    label.setBackground(new Color(222, 158, 227, 255));
                } else {
                    label.setBackground(new Color(255, 217, 217));
                }
            }
        }
    }

    private boolean searchMatches(String text, JLabel label) {
        if (text.length() == 0) return false;
        if (label.getText() != null && label.getText().toLowerCase().contains(text.toLowerCase()))
            return true;
        if (label.getToolTipText() != null && label.getToolTipText().toLowerCase().contains(text.toLowerCase()))
            return true;
        return false;
    }

    public void generateAndShow() throws FileNotFoundException {
        BingoConfiguration configuration = new BingoConfiguration(new File("bingo-core/src/test/resources/bingo/generate/outer_wilds.yaml"));
        BingoGenerator generator = new BingoGenerator(configuration);
        generator.setWidth(10);
        generator.setHeight(7);
        generator.setMaxGenerationAttempts(1);
        generator.setDifficulty(2);
        Random random;
        if (seedField.getText().isEmpty() || !seedField.getText().matches("[0-9]+")) {
            random = new Random();
        } else {
            random = new Random(Long.parseLong(seedField.getText()));
        }
        BingoBoard bingoBoard = generator.generateBingoBoard(random);
        showBoard(bingoBoard);
        System.out.println(bingoBoard.toJson());
    }

    public void showBoard(BingoBoard bingoBoard) {
        bingoGridPanel.removeAll();
        bingoGridPanel.setLayout(new GridLayout(bingoBoard.getWidth(), bingoBoard.getHeight()));
        EmptyBorder eBorder = new EmptyBorder(10, 10, 10, 10);
        LineBorder lBorder = new LineBorder(new Color(100, 100, 100));
        for (int i = 0; i < bingoBoard.getWidth(); i++) {
            for (int j = 0; j < bingoBoard.getHeight(); j++) {
                JLabel bingoTile = new JLabel("<html><center>" + bingoBoard.get(i, j).getText() + "</center></html>");
                StringJoiner tooltip = new StringJoiner("<br>");
                if (bingoBoard.get(i, j).getTooltip() != null) {
                    tooltip.add(bingoBoard.get(i, j).getTooltip().replace("\n", "<br>").replace("\\n", "<br>"));
                }
                if (bingoBoard.get(i, j).getCategories() != null && bingoBoard.get(i, j).getCategories().size() > 0) {
                    tooltip.add(bingoBoard.get(i, j).getCategories().stream().map(Category::getName).collect(Collectors.joining(", ")).replace("\n", "<br>").replace("\\n", "<br>"));
                }
                if (tooltip.length() > 0) {
                    bingoTile.setToolTipText("<html>" + tooltip + "</html>");
                }
                bingoTile.setHorizontalAlignment(SwingConstants.CENTER);
                bingoTile.setVerticalAlignment(SwingConstants.CENTER);
                bingoTile.setBackground(new Color(255, 217, 217));
                bingoTile.setOpaque(true);
                bingoTile.setVisible(true);
                bingoTile.setBorder(BorderFactory.createCompoundBorder(lBorder, eBorder));
                bingoGridPanel.add(bingoTile);
            }
        }
        searchFor(searchField.getText());
        bingoGridPanel.revalidate();
    }

    public static void main(String[] args) throws FileNotFoundException {
        new BingoFrame().generateAndShow();
    }
}
