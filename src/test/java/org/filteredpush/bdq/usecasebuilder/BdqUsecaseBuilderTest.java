package org.filteredpush.bdq.usecasebuilder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Smoke tests for {@link BdqUsecaseBuilder}.
 */
public class BdqUsecaseBuilderTest {

    @Test
    public void testClassIsLoadable() {
        assertDoesNotThrow(() ->
                Class.forName("org.filteredpush.bdq.usecasebuilder.BdqUsecaseBuilder"));
    }

    @Test
    public void testBuildOptionsReturnsNonNull() {
        assertNotNull(BdqUsecaseBuilder.buildOptions());
    }

    @Test
    public void testHelpFlagDoesNotThrow() {
        // --help should print help and return without calling System.exit()
        assertDoesNotThrow(() -> BdqUsecaseBuilder.main(new String[]{"--help"}));
    }

    @Test
    public void testShortHelpFlagDoesNotThrow() {
        assertDoesNotThrow(() -> BdqUsecaseBuilder.main(new String[]{"-h"}));
    }
}
