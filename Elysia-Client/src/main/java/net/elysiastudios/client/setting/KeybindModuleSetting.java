package net.elysiastudios.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.lwjgl.glfw.GLFW;

import java.util.Map;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public class KeybindModuleSetting extends ModuleSetting<Integer> {
    private static final Map<Integer, String> SPECIAL_KEYS = Map.ofEntries(
        Map.entry(GLFW.GLFW_KEY_RIGHT_SHIFT, "RSHIFT"),
        Map.entry(GLFW.GLFW_KEY_LEFT_SHIFT, "LSHIFT"),
        Map.entry(GLFW.GLFW_KEY_RIGHT_CONTROL, "RCTRL"),
        Map.entry(GLFW.GLFW_KEY_LEFT_CONTROL, "LCTRL"),
        Map.entry(GLFW.GLFW_KEY_RIGHT_ALT, "RALT"),
        Map.entry(GLFW.GLFW_KEY_LEFT_ALT, "LALT"),
        Map.entry(GLFW.GLFW_KEY_SPACE, "SPACE"),
        Map.entry(GLFW.GLFW_KEY_TAB, "TAB"),
        Map.entry(GLFW.GLFW_KEY_ENTER, "ENTER"),
        Map.entry(GLFW.GLFW_KEY_BACKSPACE, "BACK"),
        Map.entry(GLFW.GLFW_KEY_DELETE, "SUPPR"),
        Map.entry(GLFW.GLFW_KEY_UP, "HAUT"),
        Map.entry(GLFW.GLFW_KEY_DOWN, "BAS"),
        Map.entry(GLFW.GLFW_KEY_LEFT, "GAUCHE"),
        Map.entry(GLFW.GLFW_KEY_RIGHT, "DROITE")
    );

    private final IntSupplier getter;
    private final IntConsumer setter;
    private final int defaultValue;

    public KeybindModuleSetting(
        String id,
        String label,
        String description,
        String section,
        boolean persisted,
        IntSupplier getter,
        IntConsumer setter,
        int defaultValue
    ) {
        super(id, label, description, section, persisted);
        this.getter = getter;
        this.setter = setter;
        this.defaultValue = defaultValue;
    }

    @Override
    public Integer getValue() {
        return getter.getAsInt();
    }

    @Override
    public void setValue(Integer value) {
        setter.accept(value == null ? 0 : Math.max(0, value));
    }

    @Override
    public Integer getDefaultValue() {
        return defaultValue;
    }

    @Override
    public String getDisplayValue() {
        return formatKey(getValue());
    }

    @Override
    public void stepForward() {
    }

    @Override
    public void stepBackward() {
    }

    @Override
    protected JsonElement serializeValue() {
        return new JsonPrimitive(getValue());
    }

    @Override
    protected void deserializeValue(JsonElement element) {
        setValue(element.getAsInt());
    }

    @Override
    public boolean isKeybind() {
        return true;
    }

    public void clearBinding() {
        setValue(0);
    }

    public static String formatKey(int keyCode) {
        if (keyCode <= 0) {
            return "Aucun";
        }
        if (SPECIAL_KEYS.containsKey(keyCode)) {
            return SPECIAL_KEYS.get(keyCode);
        }
        String keyName = GLFW.glfwGetKeyName(keyCode, 0);
        if (keyName != null && !keyName.isBlank()) {
            return keyName.toUpperCase();
        }
        if (keyCode >= GLFW.GLFW_KEY_F1 && keyCode <= GLFW.GLFW_KEY_F25) {
            return "F" + (keyCode - GLFW.GLFW_KEY_F1 + 1);
        }
        return "KEY " + keyCode;
    }
}
