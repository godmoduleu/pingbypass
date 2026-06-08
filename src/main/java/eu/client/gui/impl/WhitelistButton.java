package eu.client.gui.impl;

import lombok.AllArgsConstructor;
import eu.client.EUClient;
import eu.client.gui.ClickGuiScreen;
import eu.client.gui.api.Button;
import eu.client.gui.api.Frame;
import eu.client.settings.impl.WhitelistSetting;
import eu.client.utils.graphics.Renderer2D;
import eu.client.utils.minecraft.IdentifierUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.registry.Registries;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class WhitelistButton extends Button {
    private final WhitelistSetting setting;

    private boolean open = false;
    private String searchQuery = "";
    private boolean searching = false;
    private boolean selecting = false;

    private int cursorPos = 0;
    private String oldSearchQuery = "";

    private boolean draggingScrollbar = false;
    private int dragStartY = 0;
    private float dragStartScroll = 0;

    private final List<String> allElements;
    private List<String> visibleElements = new ArrayList<>();

    private final int maxDisplayed = 6;
    private float scrollOffset = 0;
    private float targetScrollOffset = 0;

    private final int searchBarHeight = 12;
    private final int elementHeight = 12;

    private final int offsetX = 2;
    private final int scrollbarWidth = 5;
    private final int symbolWidth = 15;

    public WhitelistButton(WhitelistSetting setting, Frame parent, int height) {
        super(setting, parent, height, setting.getDescription());
        this.setting = setting;
        this.allElements = getAllElements();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean queryChanged = !searchQuery.equals(oldSearchQuery);
        oldSearchQuery = searchQuery;

        float ratio = 0;
        if (queryChanged) {
            ratio = getScrollRatio();
        }
        updateVisibleElements();
        clampScrollValues();

        if (queryChanged) {
            applyScrollRatio(ratio);
            clampScrollValues();
        }

        scrollOffset += (targetScrollOffset - scrollOffset) * 0.5f;

        Renderer2D.renderQuad(context.getMatrices(), getX() + getPadding() + 1, getY(), getX() + getWidth() - getPadding() - 1, getY() + 13 - 1, ClickGuiScreen.getButtonColor(getY(), 100));
        Renderer2D.renderQuad(context.getMatrices(), getX() + getPadding() + 1, getY(), getX() + getPadding() + 2, getY() + getHeight() - 1, ClickGuiScreen.getButtonColor(getY(), 30));

        EUClient.FONT_MANAGER.drawTextWithShadow(context, setting.getTag(), getX() + getTextPadding() + 1, getY() + 2, Color.WHITE);
        EUClient.FONT_MANAGER.drawTextWithShadow(context, Formatting.GRAY + "" + setting.getWhitelist().size(), getX() + getWidth() - getTextPadding() - 1 - EUClient.FONT_MANAGER.getWidth(setting.getWhitelist().size() + ""), getY() + 2, Color.WHITE);

        if (open) {
            int contentY = getY() + getParent().getHeight();

            Renderer2D.renderQuad(context.getMatrices(), getX() + getPadding() + 1 + offsetX, contentY, getX() + getWidth() - getPadding() - 1, contentY + searchBarHeight, ClickGuiScreen.getButtonColor(getY(), 30));

            String displayedSearch = searchQuery;
            if (searching) {
                String cursorChar = EUClient.CLICK_GUI.isShowLine() ? "|" : " ";
                if (cursorPos <= displayedSearch.length()) {
                    displayedSearch = displayedSearch.substring(0, cursorPos) + cursorChar + displayedSearch.substring(cursorPos);
                }
            } else {
                displayedSearch = "Search...";
            }

            EUClient.FONT_MANAGER.drawTextWithShadow(context, displayedSearch, getX() + getTextPadding() + 2 + offsetX, contentY + (searchBarHeight / 2) - (EUClient.FONT_MANAGER.getHeight() / 2), searching ? (selecting ? ClickGuiScreen.getButtonColor(getY(), 255) : Color.WHITE) : Color.GRAY);

            int listStartY = contentY + searchBarHeight;
            int renderCount = Math.min(visibleElements.size(), maxDisplayed);
            int scrollbarX = getX() + getWidth() - getPadding() - scrollbarWidth - 1;
            int scrollbarFullHeight = elementHeight * maxDisplayed;

            List<String> whitelistIds = setting.getWhitelistIds();
            String[] words = searchQuery.isEmpty() ? new String[0] : searchQuery.toLowerCase().split(" ");

            for (int i = 0; i < renderCount; i++) {
                int index = (int) (i + scrollOffset);
                if (index < 0 || index >= visibleElements.size()) continue;

                String registryId = visibleElements.get(index);
                boolean inWhitelist = whitelistIds.contains(registryId);

                String renderText = registryId.replace("minecraft:", "");

                Color normalColor = inWhitelist ? Color.WHITE : Color.GRAY;
                Color highlightColor = inWhitelist ? Color.YELLOW : Color.LIGHT_GRAY;

                int itemY = listStartY + i * elementHeight;

                Renderer2D.renderQuad(context.getMatrices(), getX() + getPadding() + 1 + offsetX, itemY, getX() + getWidth() - getPadding() - 1, itemY + elementHeight, ClickGuiScreen.getButtonColor(getY(), 30));

                int textX = getX() + getTextPadding() + 2 + offsetX;
                int symbolX = scrollbarX - symbolWidth + 5;
                boolean textHovered = mouseX >= textX && mouseX <= symbolX && mouseY >= itemY && mouseY < itemY + elementHeight;

                List<ColoredSegment> segments = highlightMatches(renderText, words, normalColor, highlightColor);

                int textWidth = 0;
                for (ColoredSegment seg : segments) {
                    textWidth += EUClient.FONT_MANAGER.getWidth(seg.text);
                }

                int availableSpace = symbolX - textX;

                List<ColoredSegment> drawSegments;
                if (textHovered || textWidth <= availableSpace) {
                    drawSegments = segments;
                } else {
                    drawSegments = truncateSegments(segments, availableSpace);
                }

                int drawX = textX;
                for (ColoredSegment seg : drawSegments) {
                    EUClient.FONT_MANAGER.drawTextWithShadow(context, seg.text, drawX, itemY + 2, seg.color);
                    drawX += EUClient.FONT_MANAGER.getWidth(seg.text);
                }

                String symbol = (!searching || inWhitelist) ? "-" : "+";
                int symbolRenderX = scrollbarX - symbolWidth + 5;
                boolean symbolHovered = mouseX >= symbolRenderX && mouseX <= symbolRenderX + symbolWidth &&
                        mouseY >= itemY && mouseY < itemY + elementHeight;

                Color symbolColor;
                if (symbol.equals("+")) {
                    symbolColor = symbolHovered ? new Color(0, 255, 0) : Color.WHITE;
                } else {
                    symbolColor = symbolHovered ? new Color(255, 0, 0) : Color.WHITE;
                }

                EUClient.FONT_MANAGER.drawTextWithShadow(context, symbol, symbolRenderX, itemY + 2, symbolColor);
            }

            if (visibleElements.size() > maxDisplayed) {
                float maxScroll = Math.max(0, visibleElements.size() - maxDisplayed);
                float scrollPercent = (maxScroll == 0) ? 0 : (scrollOffset / maxScroll);

                int barHeight = (int)(scrollbarFullHeight * ((float)maxDisplayed / visibleElements.size()));
                if (barHeight < 20) barHeight = 20;

                int scrollbarPos = (int) ((scrollbarFullHeight - barHeight) * scrollPercent);

                Renderer2D.renderQuad(context.getMatrices(), scrollbarX, listStartY, scrollbarX + scrollbarWidth, listStartY + scrollbarFullHeight, ClickGuiScreen.getButtonColor(getY(), 40));
                Renderer2D.renderQuad(context.getMatrices(), scrollbarX, listStartY + scrollbarPos, scrollbarX + scrollbarWidth, listStartY + scrollbarPos + barHeight, ClickGuiScreen.getButtonColor(getY(), 200));
            }
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovering(mouseX, mouseY) && button == 1) {
            this.open = !open;
        }

        if (!open) return;

        if (button == 0) {
            int contentY = getY() + getParent().getHeight();

            if (mouseX >= getX() + getPadding() + 1 + offsetX && mouseX <= getX() + getWidth() - getPadding() - 1 && mouseY >= contentY && mouseY <= contentY + searchBarHeight) {
                searching = true;
                selecting = false;
                cursorPos = searchQuery.length();
                return;
            } else {
                if (!isWithinOpenArea(mouseX, mouseY)) {
                    searching = false;
                    selecting = false;
                    searchQuery = "";
                    cursorPos = 0;
                }
            }

            if (visibleElements.size() > maxDisplayed && isHoveringScrollbar(mouseX, mouseY)) {
                draggingScrollbar = true;
                dragStartY = (int) mouseY;
                dragStartScroll = scrollOffset;
                return;
            }

            if (!isWithinOpenArea(mouseX, mouseY)) return;

            int contentAreaX1 = getX() + getPadding() + 1 + offsetX;
            int contentAreaX2 = getX() + getWidth() - getPadding() - 1;

            if (mouseX < contentAreaX1 || mouseX > contentAreaX2) return;

            int contentYArea = getY() + getParent().getHeight();
            int listStartY = contentYArea + searchBarHeight;
            if (mouseY >= listStartY && mouseY < listStartY + elementHeight * maxDisplayed) {
                int relativeY = (int) (mouseY - listStartY);
                int index = (int) (scrollOffset) + relativeY / elementHeight;
                if (index >= 0 && index < visibleElements.size()) {
                    String registryId = visibleElements.get(index);

                    int scrollbarX = getX() + getWidth() - getPadding() - scrollbarWidth - 1;
                    int symbolX = scrollbarX - symbolWidth + 5;
                    int itemY = listStartY + (relativeY / elementHeight) * elementHeight;

                    if (mouseX >= symbolX && mouseX <= symbolX + symbolWidth && mouseY >= itemY && mouseY < itemY + elementHeight) {
                        boolean inWhitelist = setting.getWhitelistIds().contains(registryId);

                        if (inWhitelist) {
                            if (setting.getType() == WhitelistSetting.Type.BLOCKS) {
                                setting.remove(IdentifierUtils.getBlock(registryId));
                            } else {
                                setting.remove(IdentifierUtils.getItem(registryId));
                            }
                            setting.remove(registryId);
                        } else {
                            if (setting.getType() == WhitelistSetting.Type.BLOCKS) {
                                setting.add(IdentifierUtils.getBlock(registryId));
                            } else {
                                setting.add(IdentifierUtils.getItem(registryId));
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingScrollbar) {
            draggingScrollbar = false;
        }
    }

    @Override
    public void mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingScrollbar && visibleElements.size() > maxDisplayed) {
            int scrollbarFullHeight = elementHeight * maxDisplayed;
            float maxScroll = Math.max(0, visibleElements.size() - maxDisplayed);

            float dy = (float) (mouseY - dragStartY);
            float ratio = dy / scrollbarFullHeight;
            targetScrollOffset = dragStartScroll + maxScroll * ratio;
            clampScrollValues();
        }
    }

    @Override
    public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (open && isWithinOpenArea(mouseX, mouseY)) {
            targetScrollOffset -= (float) verticalAmount;
            clampScrollValues();
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!searching) return;
        long handle = mc.getWindow().getHandle();

        boolean ctrlPressed = InputUtil.isKeyPressed(handle, MinecraftClient.IS_SYSTEM_MAC ? GLFW.GLFW_KEY_LEFT_SUPER : GLFW.GLFW_KEY_LEFT_CONTROL);

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            searching = false;
            selecting = false;
            searchQuery = "";
            cursorPos = 0;
        } else if (keyCode == GLFW.GLFW_KEY_ENTER) {
            searching = false;
            selecting = false;
            searchQuery = "";
            cursorPos = 0;
        } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (selecting) {
                searchQuery = "";
                cursorPos = 0;
                selecting = false;
            } else {
                if (cursorPos > 0) {
                    searchQuery = searchQuery.substring(0, cursorPos - 1) + searchQuery.substring(cursorPos);
                    cursorPos--;
                }
            }
        } else if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (cursorPos < searchQuery.length()) {
                searchQuery = searchQuery.substring(0, cursorPos) + searchQuery.substring(cursorPos + 1);
            }
        } else if (keyCode == GLFW.GLFW_KEY_LEFT) {
            if (cursorPos > 0) cursorPos--;
        } else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            if (cursorPos < searchQuery.length()) cursorPos++;
        } else if (ctrlPressed) {
            if (keyCode == GLFW.GLFW_KEY_V) {
                try {
                    String clip = mc.keyboard.getClipboard();
                    if (clip != null) {
                        searchQuery = searchQuery.substring(0, cursorPos) + clip + searchQuery.substring(cursorPos);
                        cursorPos += clip.length();
                    }
                } catch (Exception e) {
                    EUClient.LOGGER.error("{}: Failed to process clipboard paste", e.getClass().getName(), e);
                }
            } else if (keyCode == GLFW.GLFW_KEY_C && selecting) {
                try {
                    mc.keyboard.setClipboard(searchQuery);
                } catch (Exception e) {
                    EUClient.LOGGER.error("{}: Failed to process clipboard change", e.getClass().getName(), e);
                }
            } else if (keyCode == GLFW.GLFW_KEY_A) {
                selecting = true;
                cursorPos = searchQuery.length();
            }
        }
    }

    @Override
    public void charTyped(char chr, int modifiers) {
        if (searching && !Character.isISOControl(chr)) {
            if (selecting) {
                searchQuery = String.valueOf(chr);
                selecting = false;
                cursorPos = 1;
            } else {
                searchQuery = searchQuery.substring(0, cursorPos) + chr + searchQuery.substring(cursorPos);
                cursorPos++;
            }
        }
    }

    @Override
    public int getHeight() {
        int baseHeight = getParent().getHeight();
        if (!open) return baseHeight;
        return baseHeight + searchBarHeight + elementHeight * maxDisplayed;
    }

    @Override
    public boolean isHovering(double mouseX, double mouseY) {
        return getX() + getPadding() <= mouseX && getY() <= mouseY && getX() + getWidth() - getPadding() > mouseX && getY() + getParent().getHeight() > mouseY;
    }

    public boolean isHandlingScroll(double mouseX, double mouseY) {
        return open && (isWithinOpenArea(mouseX, mouseY) || draggingScrollbar);
    }

    private boolean isWithinOpenArea(double mouseX, double mouseY) {
        int contentY = getY() + getParent().getHeight();
        int maxAreaHeight = searchBarHeight + elementHeight * maxDisplayed;
        return mouseX >= getX() + getPadding() + 1 + offsetX && mouseX <= getX() + getWidth() - getPadding() - 1 && mouseY >= contentY && mouseY <= contentY + maxAreaHeight;
    }

    private boolean isHoveringScrollbar(double mouseX, double mouseY) {
        if (!open || visibleElements.size() <= maxDisplayed) return false;
        int contentY = getY() + getParent().getHeight();
        int listStartY = contentY + searchBarHeight;
        int scrollbarX = getX() + getWidth() - getPadding() - scrollbarWidth - 1;
        int scrollbarHeight = elementHeight * maxDisplayed;

        return mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth && mouseY >= listStartY && mouseY <= listStartY + scrollbarHeight;
    }

    private void updateVisibleElements() {
        List<String> baseList;
        if (searchQuery.isEmpty()) {
            if (searching) {
                baseList = new ArrayList<>(allElements);
            } else {
                List<String> whitelistIds = setting.getWhitelistIds();
                baseList = allElements.stream()
                        .filter(whitelistIds::contains)
                        .collect(Collectors.toList());
            }
            baseList.sort((id1, id2) -> {
                int cmp = id1.compareToIgnoreCase(id2);
                if (cmp == 0) {
                    return Integer.compare(id1.length(), id2.length());
                }
                return cmp;
            });
        } else {
            String[] words = searchQuery.toLowerCase().split(" ");
            baseList = allElements.stream().filter(id -> {
                String lower = id.toLowerCase();
                for (String w : words) {
                    if (!w.isEmpty() && !lower.contains(w)) return false;
                }
                return true;
            }).collect(Collectors.toList());

            baseList.sort((id1, id2) -> {
                int compare = compareBySearchPriority(id1, id2, words);
                if (compare != 0) return compare;

                int cmp = id1.compareToIgnoreCase(id2);
                if (cmp == 0) {
                    return Integer.compare(id1.length(), id2.length());
                }
                return cmp;
            });
        }

        this.visibleElements = baseList;
    }

    private int compareBySearchPriority(String id1, String id2, String[] words) {
        int startCount1 = countStarts(id1, words);
        int startCount2 = countStarts(id2, words);
        return Integer.compare(startCount1, startCount2);
    }

    private int countStarts(String id, String[] words) {
        int count = id.length();
        String lower = id.toLowerCase();
        for (String w : words) {
            if (!w.isEmpty() && lower.contains(w.toLowerCase())) {
                int c = lower.split(w.toLowerCase())[0].length();
                if (c < count) count = c;
            }
        }
        return count;
    }

    private float getScrollRatio() {
        int maxSize = visibleElements.size();
        int maxScroll = Math.max(0, maxSize - maxDisplayed);
        if (maxScroll == 0) return 0;
        return scrollOffset / maxScroll;
    }

    private void applyScrollRatio(float ratio) {
        int maxSize = visibleElements.size();
        int maxScroll = Math.max(0, maxSize - maxDisplayed);
        float newOffset = ratio * maxScroll;
        this.scrollOffset = newOffset;
        this.targetScrollOffset = newOffset;
    }

    private void clampScrollValues() {
        int maxSize = visibleElements.size();
        int maxScroll = Math.max(0, maxSize - maxDisplayed);
        if (targetScrollOffset < 0) targetScrollOffset = 0;
        if (targetScrollOffset > maxScroll) targetScrollOffset = maxScroll;

        if (scrollOffset < 0) scrollOffset = 0;
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
    }

    private List<String> getAllElements() {
        List<String> temp = new ArrayList<>();
        if (setting.getType() == WhitelistSetting.Type.BLOCKS) {
            Registries.BLOCK.getEntrySet().forEach(e -> {
                String id = Registries.BLOCK.getId(e.getValue()).toString();
                temp.add(id);
            });
        } else {
            Registries.ITEM.getEntrySet().forEach(e -> {
                String id = Registries.ITEM.getId(e.getValue()).toString();
                temp.add(id);
            });
        }

        temp.sort((a, b) -> {
            int cmp = a.compareToIgnoreCase(b);
            if (cmp == 0) {
                return Integer.compare(a.length(), b.length());
            }
            return cmp;
        });

        return temp;
    }

    private List<ColoredSegment> truncateSegments(List<ColoredSegment> segments, int maxWidth) {
        List<ColoredSegment> truncated = new ArrayList<>();
        int currentWidth = 0;

        for (ColoredSegment seg : segments) {
            int segWidth = EUClient.FONT_MANAGER.getWidth(seg.text);
            if (currentWidth + segWidth <= maxWidth) {
                truncated.add(seg);
                currentWidth += segWidth;
            } else {
                int spaceLeft = maxWidth - currentWidth;
                if (spaceLeft > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < seg.text.length(); i++) {
                        char c = seg.text.charAt(i);
                        int charWidth = EUClient.FONT_MANAGER.getWidth(String.valueOf(c));
                        if (charWidth <= spaceLeft - EUClient.FONT_MANAGER.getWidth("…")) {
                            sb.append(c);
                            spaceLeft -= charWidth;
                        } else {
                            break;
                        }
                    }
                    sb.append("…");
                    truncated.add(new ColoredSegment(sb.toString(), seg.color));
                } else {
                    truncated.add(new ColoredSegment("…", Color.GRAY));
                }
                break;
            }
        }

        return truncated;
    }

    private List<ColoredSegment> highlightMatches(String text, String[] words, Color normalColor, Color highlightColor) {
        if (words.length == 0) {
            List<ColoredSegment> segments = new ArrayList<>();
            segments.add(new ColoredSegment(text, normalColor));
            return segments;
        }

        String lowerText = text.toLowerCase();
        List<Range> ranges = new ArrayList<>();
        for (String w : words) {
            if (w.isEmpty()) continue;
            int start = 0;
            while (true) {
                int idx = lowerText.indexOf(w, start);
                if (idx == -1) break;
                ranges.add(new Range(idx, idx + w.length()));
                start = idx + w.length();
            }
        }

        ranges = mergeRanges(ranges);

        List<ColoredSegment> segments = new ArrayList<>();
        int currentIndex = 0;
        for (Range r : ranges) {
            if (r.start > currentIndex) {
                segments.add(new ColoredSegment(text.substring(currentIndex, r.start), normalColor));
            }
            segments.add(new ColoredSegment(text.substring(r.start, r.end), highlightColor));
            currentIndex = r.end;
        }
        if (currentIndex < text.length()) {
            segments.add(new ColoredSegment(text.substring(currentIndex), normalColor));
        }

        return segments;
    }

    private List<Range> mergeRanges(List<Range> ranges) {
        if (ranges.isEmpty()) return ranges;
        ranges.sort(Comparator.comparingInt(r -> r.start));
        List<Range> merged = new ArrayList<>();
        Range current = ranges.getFirst();
        for (int i = 1; i < ranges.size(); i++) {
            Range next = ranges.get(i);
            if (next.start <= current.end) {
                current.end = Math.max(current.end, next.end);
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        return merged;
    }

    @AllArgsConstructor
    private static class Range {
        int start, end;
    }

    @AllArgsConstructor
    private static class ColoredSegment {
        String text;
        Color color;
    }
}