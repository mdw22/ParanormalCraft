package io.github.mdw22.paranormalcraft.binding;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundJobsTest {

    @Test
    void aLingeringSpiritBecomesACarriedWispFamiliar() {

        BoundJobs.Job job = BoundJobs.jobFor("paranormalcraft:lingering_spirit");
        assertEquals(BoundJobs.WISP_FAMILIAR, job.id());
        assertEquals(BoundJobs.Use.CARRIED, job.use());
        assertEquals("job.paranormalcraft.wisp_familiar", job.langKey());
        assertTrue(BoundJobs.canBind("paranormalcraft:lingering_spirit"));
    }

    @Test
    void typesWithoutARowCantBeBound() {

        assertNull(BoundJobs.jobFor("paranormalcraft:poltergeist"));
        assertFalse(BoundJobs.canBind("minecraft:zombie"));
    }
}
