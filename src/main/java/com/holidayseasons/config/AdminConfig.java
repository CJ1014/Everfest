package com.holidayseasons.config;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.holidayseasons.EverFest;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Remote editing of the server config by operators. NeoForge's own config screen cannot change a SERVER config while
 * connected to a remote server, so EverFest describes its settings to an operator's client and applies the edits it
 * sends back - all or nothing, each value checked against the config's own type, range and validators.
 */
public final class AdminConfig {
    public enum Type { BOOL, INT, LONG, DOUBLE, ENUM, STRING, INT_LIST, STRING_LIST }

    /** One setting as shown to the operator. Values travel as strings; lists are comma-separated. */
    public record Entry(String path, Type type, String value, String min, String max, List<String> options, String comment, String translationKey) {
    }

    private AdminConfig() {
    }

    static Map<String, ModConfigSpec.ConfigValue<?>> values() {
        Map<String, ModConfigSpec.ConfigValue<?>> out = new LinkedHashMap<>();
        collect(ServerConfig.SPEC.getValues(), out);
        return out;
    }

    private static void collect(UnmodifiableConfig config, Map<String, ModConfigSpec.ConfigValue<?>> out) {
        for (UnmodifiableConfig.Entry entry : config.entrySet()) {
            Object v = entry.getValue();
            if (v instanceof UnmodifiableConfig sub) collect(sub, out);
            else if (v instanceof ModConfigSpec.ConfigValue<?> cv) out.put(String.join(".", cv.getPath()), cv);
        }
    }

    static Type typeOf(ModConfigSpec.ConfigValue<?> v) {
        if (v instanceof ModConfigSpec.BooleanValue) return Type.BOOL;
        if (v instanceof ModConfigSpec.IntValue) return Type.INT;
        if (v instanceof ModConfigSpec.LongValue) return Type.LONG;
        if (v instanceof ModConfigSpec.DoubleValue) return Type.DOUBLE;
        if (v instanceof ModConfigSpec.EnumValue<?>) return Type.ENUM;
        Object d = v.getDefault();
        if (d instanceof List<?> l) {
            Object probe = !l.isEmpty() ? l.get(0) : (v.get() instanceof List<?> cur && !cur.isEmpty() ? cur.get(0) : "");
            return probe instanceof Number ? Type.INT_LIST : Type.STRING_LIST;
        }
        return Type.STRING;
    }

    static String format(Object value) {
        if (value instanceof List<?> l) return String.join(", ", l.stream().map(String::valueOf).toList());
        if (value instanceof Enum<?> e) return e.name();
        return String.valueOf(value);
    }

    /** Every server setting with its current value. */
    public static List<Entry> describe() {
        List<Entry> out = new ArrayList<>();
        if (!ServerConfig.SPEC.isLoaded()) return out;
        values().forEach((path, v) -> {
            ModConfigSpec.ValueSpec spec = v.getSpec();
            Type type = typeOf(v);
            String min = "";
            String max = "";
            ModConfigSpec.Range<?> range = spec.getRange();
            if (range != null) {
                min = String.valueOf(range.getMin());
                max = String.valueOf(range.getMax());
            }
            List<String> options = List.of();
            if (v instanceof ModConfigSpec.EnumValue<?> ev) {
                options = Arrays.stream(ev.get().getDeclaringClass().getEnumConstants()).map(Enum::name).toList();
            }
            String comment = spec.getComment() == null ? "" : spec.getComment();
            String key = spec.getTranslationKey() == null ? "" : spec.getTranslationKey();
            out.add(new Entry(path, type, format(v.get()), min, max, options, comment, key));
        });
        return out;
    }

    /** Parses {@code text} as a value of {@code type} (for {@code v}), or throws IllegalArgumentException. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static Object parse(ModConfigSpec.ConfigValue<?> v, Type type, String text) {
        String s = text.strip();
        return switch (type) {
            case BOOL -> {
                if (!s.equalsIgnoreCase("true") && !s.equalsIgnoreCase("false")) throw new IllegalArgumentException("not true/false");
                yield Boolean.parseBoolean(s);
            }
            case INT -> Integer.parseInt(s);
            case LONG -> Long.parseLong(s);
            case DOUBLE -> Double.parseDouble(s);
            case ENUM -> Enum.valueOf((Class) ((Enum<?>) v.get()).getDeclaringClass(), s);
            case STRING -> s;
            case INT_LIST -> s.isEmpty() ? List.of() : Arrays.stream(s.split(",")).map(String::strip).map(Integer::parseInt).toList();
            case STRING_LIST -> s.isEmpty() ? List.of() : Arrays.stream(s.split(",")).map(String::strip).filter(x -> !x.isEmpty()).toList();
        };
    }

    /**
     * Applies {@code changes} (path -> text). Either every change is valid and all are saved, or nothing changes.
     * Returns null on success, or a message naming the first invalid setting.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static String apply(Map<String, String> changes) {
        if (!ServerConfig.SPEC.isLoaded()) return "the server config is not loaded";
        Map<String, ModConfigSpec.ConfigValue<?>> all = values();
        Map<ModConfigSpec.ConfigValue, Object> parsed = new LinkedHashMap<>();
        for (Map.Entry<String, String> c : changes.entrySet()) {
            ModConfigSpec.ConfigValue<?> v = all.get(c.getKey());
            if (v == null) return "unknown setting " + c.getKey();
            Object value;
            try {
                value = parse(v, typeOf(v), c.getValue());
            } catch (RuntimeException e) {
                return c.getKey() + ": cannot read '" + c.getValue() + "'";
            }
            if (!v.getSpec().test(value)) return c.getKey() + ": '" + c.getValue() + "' is not allowed";
            parsed.put(v, value);
        }
        parsed.forEach(ModConfigSpec.ConfigValue::set);
        if (!parsed.isEmpty()) ServerConfig.SPEC.save();
        EverFest.LOGGER.info("EverFest: {} server settings changed: {}", parsed.size(), changes.keySet());
        return null;
    }
}
