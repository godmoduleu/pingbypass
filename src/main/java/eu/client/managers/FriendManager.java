package eu.client.managers;

import lombok.Getter;
import eu.client.Pingbypass;

import java.awt.*;
import java.util.ArrayList;

@Getter
public class FriendManager {
    private final ArrayList<String> friends = new ArrayList<>();

    public boolean contains(String name) {
        if (getFriendFire())
            return false;
        return friends.stream().anyMatch(name::equalsIgnoreCase);
    }

    public void add(String name) {
        if (contains(name))
            return;
        friends.add(name);
    }

    public void remove(String name) {
        friends.removeIf(name::equalsIgnoreCase);
    }

    public void clear() {
        friends.clear();
    }

    public boolean getFriendFire() {
        return false;
    }

    public void sendFriendMessage(String name) {
    }

    public Color getDefaultFriendColor() {
        return getDefaultFriendColor(255);
    }

    public Color getDefaultFriendColor(int alpha) {
        return new Color(85, 255, 255, alpha);
    }
}
