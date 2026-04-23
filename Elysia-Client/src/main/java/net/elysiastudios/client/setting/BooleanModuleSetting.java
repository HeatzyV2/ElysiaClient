package net.elysiastudios.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class BooleanModuleSetting extends ModuleSetting<Boolean> {
    private final Supplier<Boolean> getter;
    private final Consumer<Boolean> setter;
    private final boolean defaultValue;

    public BooleanModuleSetting(
        String id,
        String label,
        String description,
        String section,
        boolean persisted,
        Supplier<Boolean> getter,
        Consumer<Boolean> setter,
        boolean defaultValue
    ) {
        super(id, label, description, section, persisted);
        this.getter = getter;
        this.setter = setter;
        this.defaultValue = defaultValue;
    }

    @Override
    public Boolean getValue() {
        return getter.get();
    }

    @Override
    public void setValue(Boolean value) {
        setter.accept(Boolean.TRUE.equals(value));
    }

    @Override
    public Boolean getDefaultValue() {
        return defaultValue;
    }

    @Override
    public String getDisplayValue() {
        return getValue() ? "Activé" : "Désactivé";
    }

    @Override
    public void stepForward() {
        setValue(!getValue());
    }

    @Override
    public void stepBackward() {
        setValue(!getValue());
    }

    @Override
    protected JsonElement serializeValue() {
        return new JsonPrimitive(getValue());
    }

    @Override
    protected void deserializeValue(JsonElement element) {
        setValue(element.getAsBoolean());
    }
}
