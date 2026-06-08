package eu.client.utils.miscellaneous;

import java.util.List;

public class ListUtils {
    public static int getIndex(List<String> list, String target) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equalsIgnoreCase(target)) return i;
        }

        return -1;
    }
}
