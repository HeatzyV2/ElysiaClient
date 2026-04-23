package net.elysiastudios.client.social;

import net.elysiastudios.ElysiaClient;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public final class ElysiaBadgeFormatter {
    private static final String BADGE_GLYPH = "\uE000";
    private static final FontDescription.Resource BADGE_FONT = new FontDescription.Resource(Identifier.fromNamespaceAndPath(ElysiaClient.MOD_ID, "ui"));
    private static final FontDescription.Resource DEFAULT_FONT = new FontDescription.Resource(Identifier.withDefaultNamespace("default"));
    private static final Component BADGE_PREFIX = Component.literal(BADGE_GLYPH + " ")
        .withStyle(style -> style.withFont(BADGE_FONT));

    private ElysiaBadgeFormatter() {
    }

    public static Component decorate(UUID uuid, Component original) {
        if (uuid == null || original == null || original.getString().startsWith(BADGE_GLYPH)) {
            return original;
        }

        if (!ElysiaPresenceManager.getInstance().isElysiaUser(uuid)) {
            return original;
        }

        return Component.empty()
            .append(Component.literal(BADGE_GLYPH).withStyle(style -> style.withFont(BADGE_FONT)))
            .append(Component.literal(" "))
            .append(original.copy().withStyle(style -> style.withFont(DEFAULT_FONT)));
    }
}
