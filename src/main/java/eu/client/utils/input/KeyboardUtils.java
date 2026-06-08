package eu.client.utils.input;

import net.minecraft.client.util.InputUtil;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.glfw.GLFW;

public class KeyboardUtils {
    public static String getKeyName(int key) {
        return switch (key) {
            case GLFW.GLFW_KEY_UNKNOWN -> "Unknown";
            case GLFW.GLFW_KEY_ESCAPE -> "Esc";
            case GLFW.GLFW_KEY_GRAVE_ACCENT -> "Grave Accent";
            case GLFW.GLFW_KEY_WORLD_1 -> "World 1";
            case GLFW.GLFW_KEY_WORLD_2 -> "World 2";
            case GLFW.GLFW_KEY_PRINT_SCREEN -> "Print Screen";
            case GLFW.GLFW_KEY_PAUSE -> "Pause";
            case GLFW.GLFW_KEY_INSERT -> "Insert";
            case GLFW.GLFW_KEY_DELETE -> "Delete";
            case GLFW.GLFW_KEY_HOME -> "Home";
            case GLFW.GLFW_KEY_PAGE_UP -> "Page Up";
            case GLFW.GLFW_KEY_PAGE_DOWN -> "Page Down";
            case GLFW.GLFW_KEY_END -> "End";
            case GLFW.GLFW_KEY_TAB -> "Tab";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "Left Control";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "Right Control";
            case GLFW.GLFW_KEY_LEFT_ALT -> "Left Alt";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "Right Alt";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "Left Shift";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "Right Shift";
            case GLFW.GLFW_KEY_UP -> "Arrow Up";
            case GLFW.GLFW_KEY_DOWN -> "Arrow Down";
            case GLFW.GLFW_KEY_LEFT -> "Arrow Left";
            case GLFW.GLFW_KEY_RIGHT -> "Arrow Right";
            case GLFW.GLFW_KEY_APOSTROPHE -> "Apostrophe";
            case GLFW.GLFW_KEY_BACKSPACE -> "Backspace";
            case GLFW.GLFW_KEY_CAPS_LOCK -> "Caps Lock";
            case GLFW.GLFW_KEY_MENU -> "Menu";
            case GLFW.GLFW_KEY_LEFT_SUPER -> "Left Super";
            case GLFW.GLFW_KEY_RIGHT_SUPER -> "Right Super";
            case GLFW.GLFW_KEY_ENTER -> "Enter";
            case GLFW.GLFW_KEY_KP_ENTER -> "Numpad Enter";
            case GLFW.GLFW_KEY_NUM_LOCK -> "Num Lock";
            case GLFW.GLFW_KEY_SPACE -> "Space";
            case GLFW.GLFW_KEY_F1 -> "F1";
            case GLFW.GLFW_KEY_F2 -> "F2";
            case GLFW.GLFW_KEY_F3 -> "F3";
            case GLFW.GLFW_KEY_F4 -> "F4";
            case GLFW.GLFW_KEY_F5 -> "F5";
            case GLFW.GLFW_KEY_F6 -> "F6";
            case GLFW.GLFW_KEY_F7 -> "F7";
            case GLFW.GLFW_KEY_F8 -> "F8";
            case GLFW.GLFW_KEY_F9 -> "F9";
            case GLFW.GLFW_KEY_F10 -> "F10";
            case GLFW.GLFW_KEY_F11 -> "F11";
            case GLFW.GLFW_KEY_F12 -> "F12";
            case GLFW.GLFW_KEY_F13 -> "F13";
            case GLFW.GLFW_KEY_F14 -> "F14";
            case GLFW.GLFW_KEY_F15 -> "F15";
            case GLFW.GLFW_KEY_F16 -> "F16";
            case GLFW.GLFW_KEY_F17 -> "F17";
            case GLFW.GLFW_KEY_F18 -> "F18";
            case GLFW.GLFW_KEY_F19 -> "F19";
            case GLFW.GLFW_KEY_F20 -> "F20";
            case GLFW.GLFW_KEY_F21 -> "F21";
            case GLFW.GLFW_KEY_F22 -> "F22";
            case GLFW.GLFW_KEY_F23 -> "F23";
            case GLFW.GLFW_KEY_F24 -> "F24";
            case GLFW.GLFW_KEY_F25 -> "F25";
            case -2 -> "Right Click";
            case -3 -> "Middle Click";
            case -4 -> "Button 3";
            case -5 -> "Button 4";
            case 0 -> "None";
            default -> {
                String keyName = GLFW.glfwGetKeyName(key, 0);
                if (keyName == null) yield "Unknown";
                yield StringUtils.capitalize(keyName);
            }
        };
    }

    public static int getKeyNumber(String name) {
        return switch (name.toLowerCase()) {
            case "esc" -> GLFW.GLFW_KEY_ESCAPE;
            case "grave" -> GLFW.GLFW_KEY_GRAVE_ACCENT;
            case "world1" -> GLFW.GLFW_KEY_WORLD_1;
            case "world2" -> GLFW.GLFW_KEY_WORLD_2;
            case "prtscr" -> GLFW.GLFW_KEY_PRINT_SCREEN;
            case "pause" -> GLFW.GLFW_KEY_PAUSE;
            case "insert" -> GLFW.GLFW_KEY_INSERT;
            case "delete" -> GLFW.GLFW_KEY_DELETE;
            case "home" -> GLFW.GLFW_KEY_HOME;
            case "pgup" -> GLFW.GLFW_KEY_PAGE_UP;
            case "pgdown" -> GLFW.GLFW_KEY_PAGE_DOWN;
            case "end" -> GLFW.GLFW_KEY_END;
            case "tab" -> GLFW.GLFW_KEY_TAB;
            case "lctrl" -> GLFW.GLFW_KEY_LEFT_CONTROL;
            case "rctrl" -> GLFW.GLFW_KEY_RIGHT_CONTROL;
            case "lalt" -> GLFW.GLFW_KEY_LEFT_ALT;
            case "ralt" -> GLFW.GLFW_KEY_RIGHT_ALT;
            case "lshift" -> GLFW.GLFW_KEY_LEFT_SHIFT;
            case "rshift" -> GLFW.GLFW_KEY_RIGHT_SHIFT;
            case "up" -> GLFW.GLFW_KEY_UP;
            case "down" -> GLFW.GLFW_KEY_DOWN;
            case "left" -> GLFW.GLFW_KEY_LEFT;
            case "right" -> GLFW.GLFW_KEY_RIGHT;
            case "apostrophe" -> GLFW.GLFW_KEY_APOSTROPHE;
            case "backspace" -> GLFW.GLFW_KEY_BACKSPACE;
            case "capslock" -> GLFW.GLFW_KEY_CAPS_LOCK;
            case "menu" -> GLFW.GLFW_KEY_MENU;
            case "lsuper" -> GLFW.GLFW_KEY_LEFT_SUPER;
            case "rsuper" -> GLFW.GLFW_KEY_RIGHT_SUPER;
            case "enter" -> GLFW.GLFW_KEY_ENTER;
            case "numenter" -> GLFW.GLFW_KEY_KP_ENTER;
            case "numlock" -> GLFW.GLFW_KEY_NUM_LOCK;
            case "space" -> GLFW.GLFW_KEY_SPACE;
            case "f1" -> GLFW.GLFW_KEY_F1;
            case "f2" -> GLFW.GLFW_KEY_F2;
            case "f3" -> GLFW.GLFW_KEY_F3;
            case "f4" -> GLFW.GLFW_KEY_F4;
            case "f5" -> GLFW.GLFW_KEY_F5;
            case "f6" -> GLFW.GLFW_KEY_F6;
            case "f7" -> GLFW.GLFW_KEY_F7;
            case "f8" -> GLFW.GLFW_KEY_F8;
            case "f9" -> GLFW.GLFW_KEY_F9;
            case "f10" -> GLFW.GLFW_KEY_F10;
            case "f11" -> GLFW.GLFW_KEY_F11;
            case "f12" -> GLFW.GLFW_KEY_F12;
            case "f13" -> GLFW.GLFW_KEY_F13;
            case "f14" -> GLFW.GLFW_KEY_F14;
            case "f15" -> GLFW.GLFW_KEY_F15;
            case "f16" -> GLFW.GLFW_KEY_F16;
            case "f17" -> GLFW.GLFW_KEY_F17;
            case "f18" -> GLFW.GLFW_KEY_F18;
            case "f19" -> GLFW.GLFW_KEY_F19;
            case "f20" -> GLFW.GLFW_KEY_F20;
            case "f21" -> GLFW.GLFW_KEY_F21;
            case "f22" -> GLFW.GLFW_KEY_F22;
            case "f23" -> GLFW.GLFW_KEY_F23;
            case "f24" -> GLFW.GLFW_KEY_F24;
            case "f25" -> GLFW.GLFW_KEY_F25;
            case "rclick" -> -2;
            case "mclick" -> -3;
            case "button3" -> -4;
            case "button4" -> -5;
            case "none" -> 0;
            default -> {
                try {
                    yield InputUtil.fromTranslationKey("key.keyboard." + name).getCode();
                } catch (NumberFormatException exception) {
                    yield 0;
                }
            }
        };
    }
}
