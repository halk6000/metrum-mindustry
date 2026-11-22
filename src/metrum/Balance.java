package metrum;

import java.io.InputStream;
import java.util.Properties;

/** Tunable numbers from balance.properties (inside the jar). Edit with tools/balance_editor.py. */
public class Balance {
    private static final Properties props = new Properties();

    static {
        try (InputStream in = Balance.class.getResourceAsStream("/balance.properties")) {
            if (in != null) props.load(in);
        } catch (Exception ignored) {
        }
    }

    public static float f(String key, float def) {
        String v = props.getProperty(key);
        if (v == null) return def;
        int hash = v.indexOf('#');
        if (hash >= 0) v = v.substring(0, hash);
        try {
            return Float.parseFloat(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static int i(String key, int def) {
        return Math.round(f(key, def));
    }
}
