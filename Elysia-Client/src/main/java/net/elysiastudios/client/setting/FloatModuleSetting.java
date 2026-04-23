package net.elysiastudios.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class FloatModuleSetting extends ModuleSetting<Float> {
    private final Supplier<Float> getter;
    private final Consumer<Float> setter;
    private final float defaultValue;
    private final float min;
    private final float max;
    private final float step;
    private final Function<Float, String> formatter;

    public FloatModuleSetting(
        String id,
        String label,
        String description,
        String section,
        boolean persisted,
        Supplier<Float> getter,
        Consumer<Float> setter,
        float defaultValue,
        float min,
        float max,
        float step,
        Function<Float, String> formatter
    ) {
        super(id, label, description, section, persisted);
        this.getter = getter;
        this.setter = setter;
        this.defaultValue = defaultValue;
        this.min = min;
        this.max = max;
        this.step = Math.max(0.01F, step);
        this.formatter = formatter == null ? value -> String.format(Locale.ROOT, "%.2f", value) : formatter;
    }

    @Override
    public Float getValue() {
        return getter.get();
    }

    @Override
    public void setValue(Float value) {
        setter.accept(clamp(value == null ? defaultValue : value));
    }

    @Override
    public Float getDefaultValue() {
        return defaultValue;
    }

    @Override
    public String getDisplayValue() {
        return formatter.apply(getValue());
    }

    @Override
    public void stepForward() {
        setValue(getValue() + step);
    }

    @Override
    public void stepBackward() {
        setValue(getValue() - step);
    }

    @Override
    protected JsonElement serializeValue() {
        return new JsonPrimitive(getValue());
    }

    @Override
    protected void deserializeValue(JsonElement element) {
        setValue(element.getAsFloat());
    }

    private float clamp(float value) {
        return Math.max(min, Math.min(max, value));
    }
}
