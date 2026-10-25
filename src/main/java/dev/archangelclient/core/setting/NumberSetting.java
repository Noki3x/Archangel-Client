package dev.archangelclient.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Numeric option within [min, max], snapped to {@code step}. Shown as a slider. */
public class NumberSetting extends Setting<Double> {
    private final double min;
    private final double max;
    private final double step;
    private String suffix = "";

    public NumberSetting(String name, String description, double defaultValue, double min, double max, double step) {
        super(name, description, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    /** Unit shown after the value in the GUI, e.g. "%" or " FPS". */
    public NumberSetting suffix(String suffix) {
        this.suffix = suffix;
        return this;
    }

    @Override
    protected Double sanitize(Double newValue) {
        double v = Math.max(min, Math.min(max, newValue));
        if (step > 0) v = min + Math.round((v - min) / step) * step;
        // drop floating point noise from the snapping, e.g. 0.30000000000000004
        return Math.round(v * 1e6) / 1e6;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getStep() {
        return step;
    }

    public float getFloat() {
        return value.floatValue();
    }

    public int getInt() {
        return (int) Math.round(value);
    }

    /** Value as a 0..1 fraction of the range, for slider rendering. */
    public double getFraction() {
        return max == min ? 0 : (value - min) / (max - min);
    }

    public void setFraction(double fraction) {
        set(min + (max - min) * fraction);
    }

    public String format() {
        boolean whole = step >= 1 && step == Math.floor(step);
        String number = whole ? String.valueOf(getInt()) : String.format(java.util.Locale.ROOT, "%.2f", value)
                .replaceAll("0+$", "").replaceAll("\\.$", "");
        return number + suffix;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value);
    }

    @Override
    public void fromJson(JsonElement json) {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) set(json.getAsDouble());
    }
}
