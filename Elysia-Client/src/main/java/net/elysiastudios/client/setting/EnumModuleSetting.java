package net.elysiastudios.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class EnumModuleSetting<E extends Enum<E>> extends ModuleSetting<E> {
    private final Supplier<E> getter;
    private final Consumer<E> setter;
    private final E defaultValue;
    private final E[] values;
    private final Function<E, String> formatter;

    public EnumModuleSetting(
        String id,
        String label,
        String description,
        String section,
        boolean persisted,
        Supplier<E> getter,
        Consumer<E> setter,
        E defaultValue,
        E[] values,
        Function<E, String> formatter
    ) {
        super(id, label, description, section, persisted);
        this.getter = getter;
        this.setter = setter;
        this.defaultValue = defaultValue;
        this.values = values;
        this.formatter = formatter == null ? Enum::name : formatter;
    }

    @Override
    public E getValue() {
        return getter.get();
    }

    @Override
    public void setValue(E value) {
        setter.accept(value == null ? defaultValue : value);
    }

    @Override
    public E getDefaultValue() {
        return defaultValue;
    }

    @Override
    public String getDisplayValue() {
        return formatter.apply(getValue());
    }

    @Override
    public void stepForward() {
        cycle(1);
    }

    @Override
    public void stepBackward() {
        cycle(-1);
    }

    @Override
    protected JsonElement serializeValue() {
        return new JsonPrimitive(getValue().name());
    }

    @Override
    protected void deserializeValue(JsonElement element) {
        try {
            setValue(Enum.valueOf(defaultValue.getDeclaringClass(), element.getAsString()));
        } catch (IllegalArgumentException ignored) {
            setValue(defaultValue);
        }
    }

    private void cycle(int direction) {
        E current = getValue();
        int index = current.ordinal() + direction;
        if (index < 0) {
            index = values.length - 1;
        } else if (index >= values.length) {
            index = 0;
        }
        setValue(values[index]);
    }
}
