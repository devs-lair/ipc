package devs.lair.ipc.balancer.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static devs.lair.ipc.balancer.utils.Constants.PLAYER_FILE_SUFFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UtilsTest {

    @Test
    @DisplayName("Try delete positive")
    void tryDeletePositive() throws IOException {
        Path tmpFile = Paths.get("temp.file");
        Files.createFile(tmpFile);

        boolean deleted = Utils.tryDelete(tmpFile);
        assertThat(deleted).isTrue();
    }

    @Test
    @DisplayName("Try delete negative")
    void tryDeleteNegative() {
        boolean deleted = Utils.tryDelete(Paths.get("not_exist_file"));
        assertThat(deleted).isFalse();
    }

    @Test
    @DisplayName("Create not exists dir")
    void createDirectoryNotExistTest() throws IOException {
        Path tmpDir = Paths.get("tmpDir");
        Utils.createDirectoryIfNotExist(tmpDir);

        assertThat(Files.exists(tmpDir)).isTrue();
        Utils.tryDelete(tmpDir);
    }

    @Test
    @DisplayName("Create not exists dir")
    void createDirectoryExistTest() throws IOException {
        Path tmpDir = Paths.get("tmpDir");
        Utils.createDirectoryIfNotExist(tmpDir);

        assertDoesNotThrow(() -> Utils.createDirectoryIfNotExist(tmpDir));
        Utils.tryDelete(tmpDir);
    }


    @Test
    @DisplayName("Get name from path")
    void getNameFromPathTest() {
        String name = Utils.generateUniqueName(new String[0], "player");
        Path path = Utils.getPathFromName(name);
        String nameFromPath = Utils.getNameFromPath(path);

        assertThat(nameFromPath).contains("player").doesNotContain(PLAYER_FILE_SUFFIX);
    }

    @Test
    @DisplayName("Get path from name")
    void getPathFromName() {
        String name = Utils.generateUniqueName(new String[0], "player");
        Path pathFromName = Utils.getPathFromName(name);
        assertThat(pathFromName.toString()).contains(PLAYER_FILE_SUFFIX);
    }

    @Test
    @DisplayName("Take name from args")
    void generateUniqueName() {
        final String MIKE = "mike";
        String[] args = {MIKE};

        String generatedName = Utils.generateUniqueName(args, "ignored");
        assertThat(generatedName).isEqualTo(MIKE);
    }

    @Test
    @DisplayName("Generate name with prefix")
    void generateUniqueNameWithPrefix() {
        final String PREFIX = "ACTOR";
        String generatedName = Utils.generateUniqueName(new String[0], PREFIX);
        assertThat(generatedName).contains(PREFIX);
    }

    @Test
    @DisplayName("Args exist but empty")
    void argsExistByEmpty() {
        final String PREFIX = "ACTOR";
        String generatedName = Utils.generateUniqueName(new String[]{""}, PREFIX);
        assertThat(generatedName).contains(PREFIX);
    }

    @Test
    @DisplayName("Args == null")
    void argsCanBeNull() {
        final String PREFIX = "ACTOR";
        String generatedName = Utils.generateUniqueName(null, PREFIX);
        assertThat(generatedName).contains(PREFIX);
    }

    @Test
    @DisplayName("On null throw Exception")
    void onNullTest() {
        assertThrows(NullPointerException.class, () -> Utils.tryDelete(null));
        assertThrows(NullPointerException.class, () -> Utils.createDirectoryIfNotExist(null));
        assertThrows(NullPointerException.class, () -> Utils.getNameFromPath(null));
        assertThrows(NullPointerException.class, () -> Utils.getPathFromName(null));
        assertThrows(NullPointerException.class, () -> Utils.generateUniqueName(new String[0], null));
        assertThrows(NullPointerException.class, () -> Utils.generateUniqueName(null, null));
        assertThrows(IllegalArgumentException.class, () -> Utils.generateUniqueName(new String[0], ""));

    }

    @Test
    @DisplayName("Check int")
    void checkIntTest() {
        assertDoesNotThrow(() -> Utils.checkInt(5, v -> v > 0));

        int zero = Utils.checkInt(0, v -> v >= 0);
        assertThat(zero).isEqualTo(0);
        assertThrows(IllegalArgumentException.class, ()->Utils.checkInt(-1, v -> v > 0));
    }

    @Test
    @DisplayName("Is null or empty")
    void isNullOrEmptyTest() {
        assertThat(Utils.isNullOrEmpty(null)).isTrue();
        assertThat(Utils.isNullOrEmpty("")).isTrue();
        assertThat(Utils.isNullOrEmpty("STRING")).isFalse();
    }
}