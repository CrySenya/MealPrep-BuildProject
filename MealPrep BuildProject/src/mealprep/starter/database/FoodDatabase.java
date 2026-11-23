package mealprep.starter.database;

import mealprep.starter.algorithm.MealSearchEngine;
import mealprep.starter.model.FoodItem;
import mealprep.starter.model.SearchResult;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class FoodDatabase {

    private ArrayList<FoodItem> allFoods = new ArrayList<>();
    private MealSearchEngine searchEngine;

    // Loads a CSV file and replaces the current list of foods with the new data.
    public boolean loadFromCSV(String filePath) {
        allFoods.clear();
        searchEngine = null;            // Invalidate the search engine so it will be rebuilt with the new data
        return appendFromCSV(filePath);
    }

    // Adds the rows of another CSV file to the existing list.
    // Used for data/fastfood_foods.csv.
    public boolean appendFromCSV(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {        // "try-with-resources" = open file, PROMISE to auto-close it when done.  FileReader opens the file, BufferedReader makes reading faster
            String line;
            boolean firstLine = true;           // Flag to skip the header row (first line of CSV)
            int lineNumber = 0;                 // Counter for error messages (which line had a problem)
            int added = 0;                      // Counter for how many foods we successfully loaded

            // Read the file line by line until we reach the end (null)
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();

                // Skip if blank line OR starts with #
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                // Skip the first line (header row) and continue to the next iteration of the loop
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                // Parse the line into a FoodItem object and add it to the list. If there's an error, print a warning but keep going.
                try {
                    FoodItem food = parseCSVLine(line);
                    if (food != null) {
                        allFoods.add(food);
                        added++;
                    }
                } catch (Exception e) {
                    System.err.println("Warning: could not read line " + lineNumber
                            + " of " + filePath + ": " + e.getMessage());
                }
            }
            
            searchEngine = null;

            System.out.println("Loaded " + added + " food items from " + filePath);
            return true;

        } catch (FileNotFoundException e) {
            System.err.println("Error: food file not found: " + filePath);
            return false;
        } catch (IOException e) {
            System.err.println("Error reading food file: " + e.getMessage());
            return false;
        }
    }

    // Splits one CSV line, respecting quoted fields.
    private FoodItem parseCSVLine(String line) {
        ArrayList<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean insideQuotes = false;

        // Split the line into fields, handling quoted commas
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            // If we see a quote, toggle the insideQuotes flag. If we see a comma and we're not inside quotes, it's a field separator. Otherwise, add the character to the current field.
            if (c == '"') {
                insideQuotes = !insideQuotes;
            } else if (c == ',' && !insideQuotes) {
                fields.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString().trim());

        // Check that we have the expected number of fields (10). If not, throw an exception.
        if (fields.size() < 10) {
            throw new IllegalArgumentException("expected 10 fields, got " + fields.size());
        }
        // Convert each field from String to the right type
        int id = Integer.parseInt(fields.get(0));           // Column 0: id → int
        String name = fields.get(1).replace("\"", "");      // Column 1: name → String, remove any leftover quotes
        String category = fields.get(2).replace("\"", "");  // Column 2: category
        String mealType = fields.get(3).replace("\"", "");  // Column 3: meal_type
        int calories = Integer.parseInt(fields.get(4));     // Column 4: calories → int
        double protein = Double.parseDouble(fields.get(5)); // Column 5: protein → double
        double carbs = Double.parseDouble(fields.get(6));   // Column 6: carbs → double
        double fat = Double.parseDouble(fields.get(7));     // Column 7: fat → double
        String servingSize = fields.get(8).replace("\"", ""); // Column 8: serving_size
        String tags = fields.get(9).replace("\"", "");      // Column 9: tags

        // Create and return a new FoodItem object with the parsed values
        return new FoodItem(id, name, category, mealType, calories,
                protein, carbs, fat, servingSize, tags);
    }

    // A copy of the loaded list, so callers cannot change the database.
    public ArrayList<FoodItem> getAllFoods() {
        return new ArrayList<>(allFoods);
    }

    // Every food that belongs to one course.
    public ArrayList<FoodItem> getFoodsByMealType(FoodItem.MealType type) {
        ArrayList<FoodItem> result = new ArrayList<>();
        for (FoodItem food : allFoods) {
            if (food.getMealType() == type) {
                result.add(food);
            }
        }
        return result;
    }

    // Searches for foods that match a query string in their name, category, or tags. Optionally filters by meal type.
    public ArrayList<FoodItem> searchFoodsBaseline(String query, FoodItem.MealType type) {
        ArrayList<FoodItem> result = new ArrayList<>();
        if (query == null) return result;

        String needle = query.trim().toLowerCase();
        if (needle.isEmpty()) return result;

        for (FoodItem food : allFoods) {
            if (type != null && food.getMealType() != type) {
                continue;
            }

            boolean matches = food.getName().toLowerCase().contains(needle)
                    || food.getCategory().toLowerCase().contains(needle)
                    || food.getTags().contains(needle);
            if (matches) {
                result.add(food);
            }
        }
        return result;
    }

    // Searches for foods using the TF-IDF index, returning ranked results. Optionally filters by meal type.
    public ArrayList<SearchResult> searchRanked(String query, FoodItem.MealType type) {
        return getSearchEngine().search(query, type);
    }

    // returns a corrected word for a query, or null.
    public String suggestQuery(String query) {
        return getSearchEngine().suggestQuery(query);
    }

    // builds the search engine once and reuses it.
    private MealSearchEngine getSearchEngine() {
        if (searchEngine == null) {
            searchEngine = new MealSearchEngine();
            searchEngine.build(allFoods, "data/food_aliases.csv");
        }
        return searchEngine;
    }

    // Looks a food up by id, or returns null when it is missing.
    public FoodItem getFoodById(int id) {
        for (FoodItem food : allFoods) {
            if (food.getId() == id) {
                return food;
            }
        }
        return null;
    }

    // Number of rows that came from the fast-food CSV.
    public int countFastFoods() {
        int count = 0;
        for (FoodItem food : allFoods) {
            if (food.isFastFood()) count++;
        }
        return count;
    }

    // Total number of loaded foods.
    public int size() {
        return allFoods.size();
    }

    // Number of foods in one course.
    public int size(FoodItem.MealType type) {
        return getFoodsByMealType(type).size();
    }

    // One-line summary of what is loaded, used by the console output.
    @Override
    public String toString() {
        return String.format("FoodDatabase[total=%d, fastfood=%d, breakfast=%d, lunch=%d, dinner=%d, snacks=%d]",
                allFoods.size(),
                countFastFoods(),
                size(FoodItem.MealType.BREAKFAST),
                size(FoodItem.MealType.LUNCH),
                size(FoodItem.MealType.DINNER),
                size(FoodItem.MealType.SNACK));
    }
}