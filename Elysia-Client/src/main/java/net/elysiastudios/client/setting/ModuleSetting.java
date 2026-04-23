package net.elysiastudios.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public abstract class ModuleSetting<T> {
    private final String id;
    private final String label;
    private final String description;
    private final String section;
    private final boolean persisted;

    protected ModuleSetting(String id, String label, String description, String section, boolean persisted) {
        this.id = id;
        this.label = label;
        this.description = description;
        this.section = section;
        this.persisted = persisted;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public String getSection() {
        return section;
    }

    public boolean isPersisted() {
        return persisted;
    }

    public abstract T getValue();

    public abstract void setValue(T value);

    public abstract T getDefaultValue();

    public abstract String getDisplayValue();

    public abstract void stepForward();

    public abstract void stepBackward();

    protected abstract JsonElement serializeValue();

    protected abstract void deserializeValue(JsonElement element);

    public void reset() {
        setValue(getDefaultValue());
    }

    public void load(JsonObject json) {
        if (persisted && json != null && json.has(id)) {
            deserializeValue(json.get(id));
        }
    }

    public void save(JsonObject json) {
        if (persisted) {
            json.add(id, serializeValue());
        }
    }

    public boolean isKeybind() {
        return false;
    }
}
