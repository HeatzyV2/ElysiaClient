package net.elysiastudios.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.IntFunction;

public class IntModuleSetting extends ModuleSetting<Integer> {
    private final IntSupplier getter;
    private final IntConsumer setter;
    private final int defaultValue;
    private final int min;
    private final int max;
    private final int step;
    private final IntFunction<String> formatter;

    public IntModuleSetting(
        String id,
        String label,
        String description,
        String section,
        boolean persisted,
        IntSupplier getter,
        IntConsumer setter,
        int defaultValue,
        int min,
        int max,
        int step,
        IntFunction<String> formatter
    ) {
        super(id, label, description, section, persisted);
        this.getter = getter;
        this.setter = setter;
        this.defaultValue = defaultValue;
        this.min = min;
        this.max = max;
        this.step = Math.max(1, step);
        this.formatter = formatter == null ? Integer::toString : formatter;
    }

    @Override
    public Integer getValue() {
        return getter.getAsInt();
    }

    @Override
    public void setValue(Integer value) {
        setter.accept(clamp(value == null ? defaultValue : value));
    }

    @Override
    public Integer getDefaultValue() {
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
        setValue(element.getAsInt());
    }

    private int clamp(int value) {
        return Math.max(min, Math.min(max, value));
    }
}
