package de.yanwittmann.bingo.generator;

import de.yanwittmann.bingo.BingoBoard;
import de.yanwittmann.bingo.BingoTile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BingoGenerator {

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    private BingoConfiguration configuration;
    private double difficulty = 2;
    private int width = 5;
    private int height = 5;
    private int maxGenerationAttempts = -1;

    public BingoGenerator(File configurationFile) throws FileNotFoundException {
        this.configuration = new BingoConfiguration(configurationFile);
    }

    public BingoGenerator(BingoConfiguration configuration) {
        this.configuration = configuration;
    }

    public BingoBoard generateBingoBoard() {
        return generateBingoBoard(new Random());
    }

    public BingoBoard generateBingoBoard(Random random) {
        if (configuration == null) {
            throw new IllegalStateException("Bingo configuration is not set.");
        }

        List<BingoTile> tiles = new ArrayList<>();
        int maxAttempts = this.maxGenerationAttempts == -1 ? (2000 / Math.max(1, width * height - 10)) + 10 : this.maxGenerationAttempts;
        LOG.info("Generation attempts [{}]", maxAttempts);
        LOG.info("Generating board with [{}]x[{}]", width, height);
        LOG.info("Difficulty [{}]", difficulty);
        if (difficulty == -1) {
            fillBoard(tiles, random);
        } else {
            for (int i = 0; i < maxAttempts; i++) {
                createAndRemoveTiles(tiles, width * height, random);
            }
        }

        BingoBoard board = new BingoBoard(width, height);
        board.populate(tiles, configuration.getCategories(), random);
        board.setBoardMetadata(configuration.getBoardMetadata());
        board.setCategoryCount(configuration.countCategories(tiles));
        board.setDifficulty(calculateDifficulty(tiles));

        LOG.info("Board difficulty is [{}]", calculateDifficulty(tiles));
        LOG.info("Categories [{}]", board.getCategoryCount());

        return board;
    }

    private void createAndRemoveTiles(List<BingoTile> tiles, int maxTileCount, Random random) {
        for (int i = 0; i < maxTileCount; i++) {
            fillBoard(tiles, random);
            ArrayList<BingoTile> backup = new ArrayList<>(tiles);
            if (difficulty != -1) {
                removeByDifficulty(tiles, (width + height) / 2);
            } else {
                removeRandom(tiles, (width + height) / 2, random);
            }
            fillBoard(tiles, random);
            double newDifficultyDistance = distanceToDestinationDifficulty(calculateDifficulty(tiles));
            double oldDifficultyDistance = distanceToDestinationDifficulty(calculateDifficulty(backup));
            if (newDifficultyDistance > oldDifficultyDistance) {
                tiles.clear();
                tiles.addAll(backup);
                removeRandom(tiles, 2, random);
                fillBoard(tiles, random);
                newDifficultyDistance = distanceToDestinationDifficulty(calculateDifficulty(tiles));
                oldDifficultyDistance = distanceToDestinationDifficulty(calculateDifficulty(backup));
                if (newDifficultyDistance > oldDifficultyDistance) {
                    tiles.clear();
                    tiles.addAll(backup);
                } else if (newDifficultyDistance < oldDifficultyDistance) {
                    LOG.info("Found better board [{}] -> [{}] [{}]", oldDifficultyDistance, newDifficultyDistance, calculateDifficulty(tiles));
                }
            } else if (newDifficultyDistance < oldDifficultyDistance) {
                LOG.info("Found better board [{}] -> [{}] [{}]", oldDifficultyDistance, newDifficultyDistance, calculateDifficulty(tiles));
            }
        }
    }

    private void removeByDifficulty(List<BingoTile> tiles, int amount) {
        tiles.sort((o1, o2) -> Double.compare(o2.getDifficulty(), o1.getDifficulty()));
        for (int i = 0; i < amount && tiles.size() > 0; i++) {
            double currentDifficulty = calculateDifficulty(tiles);
            if (currentDifficulty > difficulty) {
                tiles.remove(0);
            } else {
                tiles.remove(tiles.size() - 1);
            }
        }
    }

    private void removeRandom(List<BingoTile> tiles, int amount, Random random) {
        for (int i = 0; i < amount && tiles.size() > 0; i++) {
            tiles.remove(random.nextInt(tiles.size()));
        }
    }

    private void fillBoard(List<BingoTile> tiles, Random random) {
        int tileCount = width * height;
        while (tiles.size() < tileCount)
            tiles.add(configuration.generateTile(tiles, width * height, difficulty, random));
    }

    private double calculateDifficulty(List<BingoTile> tiles) {
        return tiles.stream().mapToDouble(BingoTile::getDifficulty).average().orElse(0.0);
    }

    private double distanceToDestinationDifficulty(double difficulty) {
        return Math.abs(difficulty - this.difficulty);
    }

    public BingoConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(BingoConfiguration configuration) {
        this.configuration = configuration;
    }

    public double getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(double difficulty) {
        this.difficulty = difficulty;
    }

    public void setDifficultyLevel(int difficulty) {
        this.difficulty = configuration.getDifficultyForLevel(difficulty);
    }

    public void setDifficultyLevel(String difficulty) {
        this.difficulty = configuration.getDifficultyForLevel(difficulty);
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setMaxGenerationAttempts(int maxGenerationAttempts) {
        this.maxGenerationAttempts = maxGenerationAttempts;
    }

    public int getMaxGenerationAttempts() {
        return maxGenerationAttempts;
    }
}
