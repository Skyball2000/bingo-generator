package de.yanwittmann.bingo.generator.config;

import de.yanwittmann.bingo.generator.BingoTile;
import de.yanwittmann.bingo.generator.Weightable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BingoConfiguration {

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    private final List<TileGenerator> tileGenerators = new ArrayList<>();
    private final Map<String, List<TextSnippet>> textSnippets = new HashMap<>();
    private final Map<String, ValueProvider> numberValueProviders = new HashMap<>();
    private final Map<String, Category> categories = new HashMap<>();
    private final List<Difficulty> difficulties = new ArrayList<>();

    public BingoConfiguration(File file) throws FileNotFoundException {
        parse(new Yaml().load(new FileInputStream(file)));
    }

    public List<TileGenerator> getTileGenerators() {
        return tileGenerators;
    }

    public Map<String, List<TextSnippet>> getTextSnippets() {
        return textSnippets;
    }

    public Map<String, Category> getCategories() {
        return categories;
    }

    public List<Difficulty> getDifficulties() {
        return difficulties;
    }

    private void parse(Object rootObject) {
        if (rootObject instanceof Map) {
            Map<String, Object> rootMap = (Map<String, Object>) rootObject;
            if (rootMap.containsKey((KEY_CATEGORY))) {
                Object optionsObject = rootMap.get(KEY_CATEGORY);
                if (optionsObject instanceof Map) {
                    Map<String, Object> optionsMap = (Map<String, Object>) optionsObject;
                    for (Map.Entry<String, Object> entry : optionsMap.entrySet()) {
                        if (entry.getValue() instanceof Map) {
                            Map<String, Object> optionMap = (Map<String, Object>) entry.getValue();
                            if (Category.validate(optionMap)) {
                                categories.put(entry.getKey(), new Category(optionMap, categories));
                            }
                        }
                    }
                }
            }
        }

        if (rootObject instanceof Map) {
            Map<String, Object> rootMap = (Map<String, Object>) rootObject;
            if (rootMap.containsKey(KEY_TEXT_SNIPPETS)) {
                Object optionsObject = rootMap.get(KEY_TEXT_SNIPPETS);
                if (optionsObject instanceof Map) {
                    Map<String, Object> optionsMap = (Map<String, Object>) optionsObject;
                    for (Map.Entry<String, Object> entry : optionsMap.entrySet()) {
                        if (entry.getValue() instanceof List) {
                            List<Object> optionsList = (List<Object>) entry.getValue();
                            for (Object optionObject : optionsList) {
                                if (optionObject instanceof Map) {
                                    Map<String, Object> optionMap = (Map<String, Object>) optionObject;
                                    if (TextSnippet.validate(optionMap)) {
                                        textSnippets.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(new TextSnippet(optionMap, categories));
                                    } else {
                                        LOG.error("Invalid text snippet: {}", optionMap);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (rootObject instanceof Map) {
            Map<String, Object> rootMap = (Map<String, Object>) rootObject;
            if (rootMap.containsKey(KEY_TILE_GENERATOR)) {
                Object optionsObject = rootMap.get(KEY_TILE_GENERATOR);
                if (optionsObject instanceof List) {
                    List<Object> optionsList = (List<Object>) optionsObject;
                    for (Object optionObject : optionsList) {
                        if (optionObject instanceof Map) {
                            Map<String, Object> optionMap = (Map<String, Object>) optionObject;
                            if (TileGenerator.validate(optionMap)) {
                                tileGenerators.add(new TileGenerator(optionMap, categories, textSnippets));
                            } else {
                                LOG.error("Invalid tile generator: {}", optionMap);
                            }
                        }
                    }
                }
            }
        }

        if (rootObject instanceof Map) {
            Map<String, Object> rootMap = (Map<String, Object>) rootObject;
            if (rootMap.containsKey(KEY_DIFFICULTY)) {
                Object optionsObject = rootMap.get(KEY_DIFFICULTY);
                if (optionsObject instanceof List) {
                    List<Object> optionsList = (List<Object>) optionsObject;
                    for (Object optionObject : optionsList) {
                        if (optionObject instanceof Map) {
                            Map<String, Object> optionMap = (Map<String, Object>) optionObject;
                            if (Difficulty.validate(optionMap)) {
                                difficulties.add(new Difficulty(optionMap));
                            }
                        }
                    }
                }
            }
        }

        if (rootObject instanceof Map) {
            Map<String, Object> rootMap = (Map<String, Object>) rootObject;
            if (rootMap.containsKey(KEY_VALUE_PROVIDERS)) {
                Object optionsObject = rootMap.get(KEY_VALUE_PROVIDERS);
                if (optionsObject instanceof Map) {
                    Map<String, Object> optionsMap = (Map<String, Object>) optionsObject;
                    for (Map.Entry<String, Object> entry : optionsMap.entrySet()) {
                        if (entry.getValue() instanceof Map) {
                            Map<String, Object> optionMap = (Map<String, Object>) entry.getValue();
                            if (ValueProvider.validate(optionMap)) {
                                numberValueProviders.put(entry.getKey(), new ValueProvider(optionMap));
                            }
                        }
                    }
                }
            }
        }

        for (Map.Entry<String, List<TextSnippet>> snippet : textSnippets.entrySet()) {
            for (TextSnippet textSnippet : snippet.getValue()) {
                textSnippet.deriveSnippetCategories(snippet.getKey(), textSnippets);
            }
        }

        LOG.info("Loaded [{}] tile generators", tileGenerators.size());
        LOG.info("Loaded [{}] text snippet types with a total of [{}] snippets", textSnippets.size(), textSnippets.values().stream().mapToInt(List::size).sum());
        LOG.info("Loaded [{}] value providers", numberValueProviders.size());
        LOG.info("Loaded [{}] categories", categories.size());
        LOG.info("Loaded [{}] difficulty levels", difficulties.size());
    }

    public double getDifficultyForLevel(int level) {
        if (difficulties.isEmpty()) return 1.0;
        if (difficulties.size() < level) level = difficulties.size();
        return difficulties.get(level - 1).getScore();
    }

    public double getDifficultyForLevel(String level) {
        return difficulties.stream().filter(d -> d.getName().equalsIgnoreCase(level)).findFirst().map(Difficulty::getScore).orElse(1.0);
    }

    public Map<Category, Integer> countCategories(List<BingoTile> tiles) {
        Map<Category, Integer> counts = new HashMap<>();
        for (BingoTile tile : tiles) {
            for (Category category : tile.getCategories()) {
                counts.compute(category, (k, v) -> v == null ? 1 : v + 1);
            }
        }
        for (Category category : categories.values()) {
            counts.computeIfAbsent(category, k -> 0);
        }
        return counts;
    }

    public BingoTile generateTile(List<BingoTile> existingTiles, int destAmount, double destinationDifficulty) {
        // find what categories are allowed and not allowed in the existing tiles
        Map<Category, Integer> categoryCount = countCategories(existingTiles);
        Set<Category> createdMustBeCategories = new HashSet<>();
        Set<Category> createdMayNotBeCategories = new HashSet<>();
        for (Map.Entry<Category, Integer> category : categoryCount.entrySet()) {
            double min = Math.max(category.getKey().getMinAbsolute(), category.getKey().getMinRelative() * 0.01 * destAmount);
            double max = Math.ceil(Math.min(category.getKey().getMaxAbsolute(), category.getKey().getMaxRelative() * 0.01 * destAmount));
            int currentCount = category.getValue();
            if (currentCount < min) {
                createdMustBeCategories.add(category.getKey());
            } else if (currentCount >= max) {
                createdMayNotBeCategories.add(category.getKey());
            }
        }

        List<TileGenerator> filteredTileGenerators = new ArrayList<>(this.tileGenerators);
        if (!createdMustBeCategories.isEmpty()) {
            for (int i = filteredTileGenerators.size() - 1; i >= 0; i--) {
                TileGenerator tileGenerator = filteredTileGenerators.get(i);
                Set<Category> derivedCategories = tileGenerator.getDerivedCategories();
                if (!tileGenerator.containsAnyCategory(createdMustBeCategories) && derivedCategories.stream().noneMatch(createdMustBeCategories::contains)) {
                    filteredTileGenerators.remove(i);
                }
            }
        }
        if (!createdMayNotBeCategories.isEmpty()) {
            for (int i = filteredTileGenerators.size() - 1; i >= 0; i--) {
                TileGenerator tileGenerator = filteredTileGenerators.get(i);
                if (tileGenerator.containsAnyCategory(createdMayNotBeCategories)) {
                    filteredTileGenerators.remove(i);
                }
            }
        }

        //LOG.info("[{}/{}] [MUST {}]  [CANNOT {}]  [{}]", existingTiles.size() + 1, destAmount, createdMustBeCategories, createdMayNotBeCategories, filteredTileGenerators.size());
        if (filteredTileGenerators.isEmpty()) {
            LOG.warn("No tile generators matching the criteria, using all [{}] tile generators", this.tileGenerators.size());
            filteredTileGenerators = new ArrayList<>(this.tileGenerators);
        }
        TileGenerator selectedGenerator = getRandom(filteredTileGenerators);
        String text = selectedGenerator.getText();

        String currentClosestText = null;
        double currentClosestDifficulty = Double.MAX_VALUE;
        Set<Category> bestTileCategories = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            AtomicReference<Double> currentDifficulty = new AtomicReference<>(0.0);
            Set<Category> currentTileCategories = new HashSet<>();
            String tmp = insertSnippets(text, createdMustBeCategories, createdMayNotBeCategories, currentDifficulty, destinationDifficulty, currentTileCategories);
            double currentDistance = Math.abs(currentDifficulty.get() - destinationDifficulty);
            double currentClosestDistance = Math.abs(currentClosestDifficulty - destinationDifficulty);
            if (currentDistance < currentClosestDistance) {
                currentClosestText = tmp;
                currentClosestDifficulty = currentDifficulty.get();
                bestTileCategories = currentTileCategories;
            }
        }
        if (currentClosestText != null) text = currentClosestText;

        BingoTile bingoTile = new BingoTile(text, currentClosestDifficulty + selectedGenerator.getDifficulty());
        selectedGenerator.getCategories().forEach(bingoTile::addCategory);
        bestTileCategories.forEach(bingoTile::addCategory);
        return bingoTile;
    }

    private String insertSnippets(String text, Set<Category> createdMustBeCategories, Set<Category> createdMayNotBeCategories, AtomicReference<Double> currentDifficulty, double destinationDifficulty, Set<Category> tileCategories) {
        boolean found;
        do {
            found = false;
            Matcher snippetsMatcher = TextSnippet.SNIPPET_PATTERN.matcher(text);
            while (snippetsMatcher.find()) {
                String snippetType = snippetsMatcher.group(1);
                if (textSnippets.containsKey(snippetType)) {
                    List<TextSnippet> snippets = new ArrayList<>(textSnippets.get(snippetType));
                    if (!createdMustBeCategories.isEmpty()) {
                        for (int i = snippets.size() - 1; i >= 0; i--) {
                            TextSnippet snippet = snippets.get(i);
                            if (snippet.getCategories().stream().noneMatch(createdMustBeCategories::contains)) {
                                snippets.remove(i);
                            }
                        }
                    }
                    if (!createdMayNotBeCategories.isEmpty()) {
                        for (int i = snippets.size() - 1; i >= 0; i--) {
                            TextSnippet snippet = snippets.get(i);
                            if (snippet.getCategories().stream().anyMatch(createdMayNotBeCategories::contains)) {
                                snippets.remove(i);
                            }
                        }
                    }
                    if (snippets.size() == 0) {
                        snippets = textSnippets.getOrDefault(snippetType, new ArrayList<>());
                    }
                    TextSnippet selectedSnippet = getRandom(snippets);
                    currentDifficulty.set(currentDifficulty.get() + selectedSnippet.getDifficulty());
                    tileCategories.addAll(selectedSnippet.getCategories());
                    text = text.replaceFirst(Pattern.quote(snippetsMatcher.group()), selectedSnippet.getText());
                    found = true;
                } else {
                    ValueProvider valueProvider = numberValueProviders.getOrDefault(snippetType, null);
                    if (valueProvider != null) {
                        Difficulty difficulty = getDifficulty(destinationDifficulty);
                        ValueProvider.NumberProviderResult result = valueProvider.getValue(difficulty.getName());
                        if (result != null) {
                            text = text.replaceFirst(Pattern.quote(snippetsMatcher.group()), String.valueOf(result.getValue()));
                            currentDifficulty.set(currentDifficulty.get() + difficulty.getScore());
                        }
                    }
                }
            }
        } while (found);
        return text;
    }

    public Difficulty getDifficulty(double difficulty) {
        double closestDifficulty = Double.MAX_VALUE;
        Difficulty closestDifficultyDifficulty = null;
        for (Difficulty difficultyDifficulty : difficulties) {
            double currentDifficulty = Math.abs(difficultyDifficulty.getScore() - difficulty);
            if (currentDifficulty < closestDifficulty) {
                closestDifficulty = currentDifficulty;
                closestDifficultyDifficulty = difficultyDifficulty;
            }
        }
        return closestDifficultyDifficulty;
    }

    public <T extends Weightable> T getRandom(Collection<T> collection) {
        double totalWeight = collection.stream().mapToDouble(Weightable::getWeight).sum();
        double random = new Random().nextDouble() * totalWeight;
        double currentWeight = 0;
        for (T weightable : collection) {
            currentWeight += weightable.getWeight();
            if (currentWeight >= random) {
                return weightable;
            }
        }
        return collection.stream().findAny().orElse(null);
    }

    static void validateContained(Map<String, Object> optionMap, String key, boolean forcePresent, Class<?>... clazz) {
        if (!optionMap.containsKey(key)) {
            if (forcePresent) {
                throw new IllegalArgumentException("Missing required option: [" + key + "] in " + optionMap);
            }
            return;
        }
        if (clazz.length > 0) {
            for (Class<?> clazz1 : clazz) {
                if (clazz1.isInstance(optionMap.get(key))) {
                    return;
                }
            }
            throw new IllegalArgumentException("Option [" + key + "] is not of type " + clazz[0].getName() + " in " + optionMap);
        }
    }

    final static String KEY_TILE_GENERATOR = "tile generators";
    final static String KEY_TILE_GENERATOR_TEXT = "text";
    final static String KEY_TILE_GENERATOR_DIFFICULTY = "difficulty";
    final static String KEY_TILE_GENERATOR_DIFFICULTIES = "difficulties";
    final static String KEY_TILE_GENERATOR_WEIGHT = "weight";
    final static String KEY_TILE_GENERATOR_CATEGORIES = "categories";

    final static String KEY_TEXT_SNIPPETS = "snippets";
    final static String KEY_TEXT_SNIPPETS_CATEGORIES = "categories";
    final static String KEY_TEXT_SNIPPETS_TEXT = "text";
    final static String KEY_TEXT_SNIPPETS_DIFFICULTY = "difficulty";
    final static String KEY_TEXT_SNIPPETS_WEIGHT = "weight";

    final static String KEY_CATEGORY = "categories";
    final static String KEY_CATEGORY_NAME = "name";
    final static String KEY_CATEGORY_MAX = "max";
    final static String KEY_CATEGORY_MIN = "min";
    final static String KEY_CATEGORY_ABSOLUTE = "absolute";
    final static String KEY_CATEGORY_RELATIVE = "relative";
    final static String KEY_CATEGORY_ANTISYNERGY = "antisynergy";
    final static String KEY_CATEGORY_SYNERGY = "synergy";

    final static String KEY_DIFFICULTY = "difficulty";
    final static String KEY_DIFFICULTY_NAME = "name";
    final static String KEY_DIFFICULTY_SCORE = "score";

    final static String KEY_VALUE_PROVIDERS = "value providers";
    final static String KEY_VALUE_PROVIDERS_MIN = "min";
    final static String KEY_VALUE_PROVIDERS_MAX = "max";
    final static String KEY_VALUE_PROVIDERS_SCORE = "score";
}
