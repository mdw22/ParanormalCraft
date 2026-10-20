package io.github.mdw22.paranormalcraft.containment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContainmentTablesTest {

    private static final String LINGERING = "paranormalcraft:lingering_spirit";

    @Test
    void lingeringSpiritYieldsThreeToFiveEctoplasm() {

        ContainmentTables.Yield yield = ContainmentTables.yieldFor(LINGERING);
        assertNotNull(yield);
        assertEquals("paranormalcraft:lingering_ectoplasm", yield.item());
        assertEquals(3, ContainmentTables.yieldCount(yield, 0.0));
        assertEquals(3, ContainmentTables.yieldCount(yield, 0.33));
        assertEquals(4, ContainmentTables.yieldCount(yield, 0.34));
        assertEquals(5, ContainmentTables.yieldCount(yield, 0.999));
        for (double roll = 0; roll < 1.0; roll += 0.01) {
            int count = ContainmentTables.yieldCount(yield, roll);
            assertTrue(count >= 3 && count <= 5, "roll " + roll + " gave " + count);
        }
    }

    @Test
    void residueOnTenPercent() {

        assertTrue(ContainmentTables.residue(0.0));
        assertTrue(ContainmentTables.residue(0.0999));
        assertFalse(ContainmentTables.residue(0.1));
        assertFalse(ContainmentTables.residue(0.9));
    }

    @Test
    void lingeringSpiritBlessingIsRestfulForThreeDays() {

        ContainmentTables.Blessing blessing = ContainmentTables.blessingFor(LINGERING);
        assertNotNull(blessing);
        assertEquals("paranormalcraft:restful", blessing.effect());
        assertEquals(72_000, blessing.ticks());
    }

    @Test
    void unknownTypesHaveNoRow() {

        assertNull(ContainmentTables.yieldFor("minecraft:zombie"));
        assertNull(ContainmentTables.blessingFor("minecraft:zombie"));
    }
}
