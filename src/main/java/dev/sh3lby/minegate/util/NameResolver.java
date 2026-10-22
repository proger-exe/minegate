package dev.sh3lby.minegate.util;

import dev.sh3lby.minegate.telegram.model.User;

public class NameResolver {
    public static String resolveTelegramName(User user) {
        if (user == null) {
            return "TelegramUser";
        }

        // Priority: @username > first_name last_name > first_name > TelegramUser
        if (user.getUsername() != null && !user.getUsername().isEmpty()) {
            return "@" + user.getUsername();
        }

        String firstName = user.getFirstName() != null ? user.getFirstName() : "";
        String lastName = user.getLastName() != null ? user.getLastName() : "";

        if (!firstName.isEmpty() && !lastName.isEmpty()) {
            return firstName + " " + lastName;
        }

        if (!firstName.isEmpty()) {
            return firstName;
        }

        return "TelegramUser";
    }
}
