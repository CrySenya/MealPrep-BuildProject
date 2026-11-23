package mealprep.starter.algorithm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * FoodAliases - step 3 of the NLP search pipeline (query expansion).
 *
 * A gazetteer is simply a dictionary of names. This one says that "tapsi"
 * means food id 1 and that "chickenjoy" means food id 901.
 *
 * Without it, a user who types "tapsi" gets nothing, because the CSV only
 * contains the word "Tapsilog". The alias list bridges the two vocabularies.
 *
 * File format (data/food_aliases.csv):
 *   alias,food_id
 *   tapsi,1
 *   chickenjoy,901
 */
public class FoodAliases {

    /** Every alias, already normalized, mapped to the food ids it points at. */
    private final HashMap<String, ArrayList<Integer>> aliases = new HashMap<>();

    /**
     * Reads the alias file. The file is optional, so a missing file only
     * returns false; the rest of the search still works without aliases.
     */
    public boolean load(String filePath) {
        // Start clean in case load() is called twice.
        aliases.clear();

        // Check first so a missing optional file does not print an ugly error.
        File file = new File(filePath);
        if (!file.exists()) {
            return false;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip blank lines and comment lines.
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Skip the header row: alias,food_id
                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                // Each row is alias,food_id. Aliases never contain a comma.
                String[] parts = line.split(",");
                if (parts.length < 2) {
                    continue;
                }

                // Normalize the alias so "Halo-Halo" and "halo halo" are the same key.
                String alias = TextNormalizer.normalize(parts[0]);
                if (alias.isEmpty()) {
                    continue;
                }

                // Bad numbers are skipped instead of stopping the whole load.
                int foodId;
                try {
                    foodId = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException e) {
                    continue;
                }

                // Create the id list the first time this alias is seen.
                ArrayList<Integer> ids = aliases.get(alias);
                if (ids == null) {
                    ids = new ArrayList<>();
                    aliases.put(alias, ids);
                }

                // Never store the same id twice for one alias.
                if (!ids.contains(foodId)) {
                    ids.add(foodId);
                }
            }
            return true;

        } catch (IOException e) {
            System.err.println("Warning: could not read alias file: " + e.getMessage());
            return false;
        }
    }

    /**
     * Returns the food ids for one word, or an empty list when it is not an alias.
     *
     * Returning an empty list (instead of null) keeps the caller simple.
     */
    public ArrayList<Integer> lookupToken(String token) {
        ArrayList<Integer> ids = aliases.get(token);
        if (ids == null) {
            return new ArrayList<>();
        }
        return ids;
    }

    /**
     * Looks up every single word and every pair of neighbouring words.
     *
     * The pair check is what makes two-word aliases work:
     * "halo-halo" becomes the words [halo, halo], and the pair "halo halo"
     * is the key stored in the file.
     */
    public ArrayList<Integer> lookupTokens(ArrayList<String> tokens) {
        ArrayList<Integer> found = new ArrayList<>();
        if (tokens == null) {
            return found;
        }

        // First pass: one-word aliases.
        for (String token : tokens) {
            ArrayList<Integer> ids = aliases.get(token);
            if (ids == null) {
                continue;
            }
            for (Integer id : ids) {
                if (!found.contains(id)) {
                    found.add(id);
                }
            }
        }

        // Second pass: two-word aliases.
        for (int i = 0; i < tokens.size() - 1; i++) {
            String pair = tokens.get(i) + " " + tokens.get(i + 1);
            ArrayList<Integer> ids = aliases.get(pair);
            if (ids == null) {
                continue;
            }
            for (Integer id : ids) {
                if (!found.contains(id)) {
                    found.add(id);
                }
            }
        }
        return found;
    }

    /** Number of aliases loaded. Used by the self-tests. */
    public int size() {
        return aliases.size();
    }
}
