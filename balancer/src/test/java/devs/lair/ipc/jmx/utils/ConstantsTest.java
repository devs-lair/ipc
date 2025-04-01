package devs.lair.ipc.jmx.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConstantsTest {

    @Test
    @DisplayName("Paths exists")
    void checkPath() {
        assertThat(Constants.CONFIG_PATH).isNotNull();
        assertThat(Constants.MEMORY_CONFIG_PATH).isNotNull();

    }
}