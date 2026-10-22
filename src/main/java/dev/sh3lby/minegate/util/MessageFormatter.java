package dev.sh3lby.minegate.util;

import dev.sh3lby.minegate.config.PluginConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class MessageFormatter {
    private final PluginConfig.MessageFormatsConfig formats;

    public MessageFormatter(PluginConfig.MessageFormatsConfig formats) {
        this.formats = formats != null ? formats : getDefaultFormats();
    }

    private PluginConfig.MessageFormatsConfig getDefaultFormats() {
        PluginConfig.MessageFormatsConfig defaultFormats = new PluginConfig.MessageFormatsConfig();
        return defaultFormats;
    }

    public String formatMinecraftToTelegram(String playerName, String message) {
        String template = formats.getMinecraftToTelegram();
        // Strip color codes from Minecraft message before sending to Telegram
        String cleanMessage = stripColorCodes(message);
        String cleanPlayerName = stripColorCodes(playerName);
        return template
                .replace("{player}", escapeHtml(cleanPlayerName))
                .replace("{message}", escapeHtml(cleanMessage));
    }

    public String formatTelegramToMinecraft(String telegramName, String message) {
        String template = formats.getTelegramToMinecraft();
        return template
                .replace("{name}", telegramName)
                .replace("{message}", message);
    }

    /**
     * Formats Telegram message to Minecraft Component with support for color codes in template
     */
    public Component formatTelegramToMinecraftComponent(String telegramName, String message, 
                                                       boolean enableColorCodes) {
        String template = formats.getTelegramToMinecraft();
        
        if (enableColorCodes) {
            // Build component by parsing template with colors and replacing placeholders
            return buildComponentFromTemplate(template, 
                java.util.Map.of("{name}", Component.text(telegramName), "{message}", Component.text(message)),
                enableColorCodes);
        } else {
            // Simple string replacement for plain text
            String formattedText = template
                    .replace("{name}", telegramName)
                    .replace("{message}", message);
            return Component.text(formattedText);
        }
    }

    public String formatSystemMessage(String message) {
        String template = formats.getSystemMessage();
        return template.replace("{message}", message);
    }

    /**
     * Formats system message to Component with support for color codes in template
     */
    public Component formatSystemMessageComponent(String message, boolean enableColorCodes) {
        String template = formats.getSystemMessage();
        
        if (enableColorCodes) {
            return buildComponentFromTemplate(template,
                java.util.Map.of("{message}", Component.text(message)),
                enableColorCodes);
        } else {
            return Component.text(template.replace("{message}", message));
        }
    }

    public String formatReplyIndicator(String playerName) {
        String template = formats.getReplyIndicator();
        return template.replace("{player}", playerName);
    }

    /**
     * Formats reply indicator to Component with support for color codes in template
     */
    public Component formatReplyIndicatorComponent(String playerName, boolean enableColorCodes) {
        String template = formats.getReplyIndicator();
        
        if (enableColorCodes) {
            return buildComponentFromTemplate(template,
                java.util.Map.of("{player}", Component.text(playerName)),
                enableColorCodes);
        } else {
            return Component.text(template.replace("{player}", playerName));
        }
    }

    public String formatReply(String telegramName, String replyIndicator, String message) {
        String template = formats.getReplyFormat();
        return template
                .replace("{telegramName}", telegramName)
                .replace("{replyIndicator}", replyIndicator)
                .replace("{message}", message);
    }

    /**
     * Formats reply message to Component with support for color codes in template
     */
    public Component formatReplyComponent(String telegramName, Component replyIndicator, Component message, boolean enableColorCodes) {
        String template = formats.getReplyFormat();
        
        if (enableColorCodes) {
            return buildComponentFromTemplate(template,
                java.util.Map.of(
                    "{telegramName}", Component.text(telegramName),
                    "{replyIndicator}", replyIndicator,
                    "{message}", message
                ),
                enableColorCodes);
        } else {
            // For plain text, convert components to strings
            String replyIndicatorText = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(replyIndicator);
            String messageText = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(message);
            String formattedText = template
                    .replace("{telegramName}", telegramName)
                    .replace("{replyIndicator}", replyIndicatorText)
                    .replace("{message}", messageText);
            return Component.text(formattedText);
        }
    }

    public static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public static String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    /**
     * Strips Minecraft color codes (&0-9, &a-f, &k-o, &r) from text
     */
    public static String stripColorCodes(String text) {
        if (text == null) {
            return "";
        }
        // Remove &x&R&R&G&G&B&B format (hex colors)
        String result = text.replaceAll("&x(&[0-9a-fA-F]){6}", "");
        // Remove & followed by 0-9, a-f, k-o, r
        result = result.replaceAll("&[0-9a-fk-or]", "");
        return result;
    }

    /**
     * Converts text with & color codes to Adventure Component
     */
    public static Component parseColorCodes(String text) {
        if (text == null) {
            return Component.empty();
        }
        // Use LegacyComponentSerializer to parse & color codes (ampersand format)
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    /**
     * Parses text with color codes support
     */
    public static Component parseFormattedText(String text, boolean enableColorCodes) {
        if (text == null) {
            return Component.empty();
        }
        
        if (enableColorCodes) {
            // Parse color codes directly
            return parseColorCodes(text);
        } else {
            // Plain text
            return Component.text(text);
        }
    }

    /**
     * Builds a Component from a template by parsing colors and replacing placeholders with components.
     * Colors in the template (e.g. &c{name}) are applied to the placeholder component when it has
     * no explicit color of its own, so the placeholder value inherits the template's active color.
     */
    private Component buildComponentFromTemplate(String template, java.util.Map<String, Component> placeholders, boolean enableColorCodes) {
        if (template == null) {
            return Component.empty();
        }

        // Find all placeholders in template
        java.util.regex.Pattern placeholderPattern = java.util.regex.Pattern.compile("\\{([^}]+)\\}");
        java.util.regex.Matcher matcher = placeholderPattern.matcher(template);
        java.util.List<java.util.Map.Entry<Integer, String>> placeholderList = new java.util.ArrayList<>();

        while (matcher.find()) {
            String fullPlaceholder = matcher.group(0); // e.g. {name}
            placeholderList.add(new java.util.AbstractMap.SimpleEntry<>(matcher.start(), fullPlaceholder));
        }

        if (placeholderList.isEmpty()) {
            // No placeholders, just parse colors
            return parseFormattedText(template, enableColorCodes);
        }

        // Build component piece by piece
        Component result = Component.empty();
        int lastIndex = 0;

        for (java.util.Map.Entry<Integer, String> entry : placeholderList) {
            int pos = entry.getKey();
            String fullPlaceholder = entry.getValue();

            // Add text before placeholder (with colors)
            if (pos > lastIndex) {
                String beforeText = template.substring(lastIndex, pos);
                Component beforeComponent = parseFormattedText(beforeText, enableColorCodes);
                result = result.append(beforeComponent);
            }

            // Resolve placeholder component
            Component placeholderComponent = placeholders.get(fullPlaceholder);
            if (placeholderComponent == null) {
                // Fallback: try without braces
                String placeholderKey = fullPlaceholder.substring(1, fullPlaceholder.length() - 1);
                placeholderComponent = placeholders.get(placeholderKey);
            }

            if (placeholderComponent != null) {
                // If color codes are enabled and this component has no explicit color, apply the
                // color that is currently active at this position in the template.
                if (enableColorCodes && placeholderComponent.color() == null) {
                    TextColor activeColor = resolveActiveColor(template, pos);
                    if (activeColor != null) {
                        placeholderComponent = placeholderComponent.color(activeColor);
                    }
                }
                result = result.append(placeholderComponent);
            } else {
                // Placeholder not found, add as text
                result = result.append(Component.text(fullPlaceholder));
            }

            lastIndex = pos + fullPlaceholder.length();
        }

        // Add remaining text after last placeholder
        if (lastIndex < template.length()) {
            String afterText = template.substring(lastIndex);
            Component afterComponent = parseFormattedText(afterText, enableColorCodes);
            result = result.append(afterComponent);
        }

        return result;
    }

    /**
     * Scans the template string from the beginning up to {@code upToPos} and returns the last
     * active color code found (e.g. the {@code &c} before {@code {name}} → RED).
     * Returns {@code null} if no color code is found or the last code was {@code &r} (reset).
     */
    private TextColor resolveActiveColor(String template, int upToPos) {
        char lastCode = 0;
        for (int i = 0; i < upToPos - 1; i++) {
            if (template.charAt(i) == '&') {
                char next = Character.toLowerCase(template.charAt(i + 1));
                if ((next >= '0' && next <= '9') || (next >= 'a' && next <= 'f') || next == 'r') {
                    lastCode = next;
                    i++; // skip the code character
                }
            }
        }
        if (lastCode == 0 || lastCode == 'r') {
            return null;
        }
        return legacyCharToColor(lastCode);
    }

    /**
     * Maps a single legacy color character (0-9, a-f) to the corresponding {@link TextColor}.
     */
    private TextColor legacyCharToColor(char code) {
        switch (code) {
            case '0': return NamedTextColor.BLACK;
            case '1': return NamedTextColor.DARK_BLUE;
            case '2': return NamedTextColor.DARK_GREEN;
            case '3': return NamedTextColor.DARK_AQUA;
            case '4': return NamedTextColor.DARK_RED;
            case '5': return NamedTextColor.DARK_PURPLE;
            case '6': return NamedTextColor.GOLD;
            case '7': return NamedTextColor.GRAY;
            case '8': return NamedTextColor.DARK_GRAY;
            case '9': return NamedTextColor.BLUE;
            case 'a': return NamedTextColor.GREEN;
            case 'b': return NamedTextColor.AQUA;
            case 'c': return NamedTextColor.RED;
            case 'd': return NamedTextColor.LIGHT_PURPLE;
            case 'e': return NamedTextColor.YELLOW;
            case 'f': return NamedTextColor.WHITE;
            default:  return null;
        }
    }
}
