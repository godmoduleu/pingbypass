package eu.client.utils.system;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class FileUtils {
    public static void resetFile(String path) throws IOException {
        if (Files.exists(Paths.get(path))) new File(path).delete();
        Files.createFile(Paths.get(path));
    }

    public static void createDirectory(String path) throws IOException {
        if (!Files.exists(Paths.get(path))) Files.createDirectories(Paths.get(path));
    }

    public static boolean fileExists(String path) {
        return Files.exists(Paths.get(path));
    }

    public static void playSound(File file, float volume) { // Volume must be between 0.0 and 1.0
        if (file.exists()) {
            try {
                Clip clip = AudioSystem.getClip();
                clip.open(AudioSystem.getAudioInputStream(file));
                ((FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN)).setValue(20f * (float) Math.log10(volume));
                clip.start();
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) clip.close();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static List<String> readLines(File file) {
        List<String> messages = new ArrayList<>();
        if(file.exists()) {
            try(Stream<String> lines = Files.lines(Paths.get(file.getPath()))) {
                messages = lines.filter(l -> !l.isEmpty()).toList();
            } catch (Exception ignored) {}
        }
        return messages;
    }
}
