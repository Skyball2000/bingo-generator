package de.yanwittmann.bingo;

import de.yanwittmann.bingo.generator.Category;
import de.yanwittmann.bingo.interfaces.Jsonable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BingoTile implements Jsonable {

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    private final String text;
    private final String tooltip;
    private final double difficulty;
    private final List<Category> categories = new ArrayList<>();

    public BingoTile(String text, String tooltip, double difficulty) {
        this.text = text;
        this.tooltip = tooltip;
        this.difficulty = difficulty;
    }

    public BingoTile(JSONObject jsonObject) {
        text = jsonObject.getString("text");
        tooltip = jsonObject.optString("tooltip", null);
        difficulty = jsonObject.getDouble("difficulty");
        JSONArray categories = jsonObject.optJSONArray("categories");
        if (categories != null) {
            this.categories.addAll(categories.toList().stream().map(s -> new Category(String.valueOf(s))).collect(Collectors.toList()));
        }
    }

    public String getText() {
        return text;
    }

    public double getDifficulty() {
        return difficulty;
    }

    public String getTooltip() {
        if (tooltip == null || tooltip.length() == 0) return null;
        return tooltip;
    }

    public void addCategory(Category category) {
        if (category == null || categories.contains(category)) return;
        categories.add(category);
    }

    public List<Category> getCategories() {
        return categories;
    }

    public boolean isTextEqual(String text) {
        if (text == null && this.text == null) return true;
        if (text == null || this.text == null) return false;
        String compare = text.replaceAll("-?\\d", "");
        String self = this.text.replaceAll("-?\\d", "");
        return compare.equalsIgnoreCase(self);
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("text", text);
        json.put("tooltip", getTooltip());
        json.put("difficulty", difficulty);
        json.put("categories", categories.stream().map(Category::getName).collect(Collectors.toList()));
        return json;
    }

    @Override
    public String toString() {
        return text;
    }
}
