import net.minecraft.client.gui.GuiGraphics;
import java.lang.reflect.Method;
public class Test {
    public static void main(String[] args) {
        for (Method m : GuiGraphics.class.getMethods()) {
            if (m.getName().equals("blit")) {
                System.out.print("blit(");
                Class<?>[] params = m.getParameterTypes();
                for (int i = 0; i < params.length; i++) {
                    System.out.print(params[i].getSimpleName());
                    if (i < params.length - 1) System.out.print(", ");
                }
                System.out.println(")");
            }
        }
    }
}
