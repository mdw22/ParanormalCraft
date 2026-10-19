package io.github.mdw22.paranormalcraft.debug;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpectationTest {

    private static JsonObject json(String text) {

        return JsonParser.parseString(text).getAsJsonObject();
    }

    @Test
    void parsesTraceWithConditionsAndWithin() {

        Expectation expectation = Expectation.parse("trace tether_drain amount>=0.09 ghost!=none within 40");
        assertEquals(Expectation.Kind.TRACE, expectation.kind());
        assertEquals("tether_drain", expectation.type());
        assertEquals(2, expectation.conditions().size());
        assertEquals(">=", expectation.conditions().get(0).operator());
        assertEquals("!=", expectation.conditions().get(1).operator());
        assertEquals(40, expectation.within());
        assertEquals(Expectation.DEFAULT_TRACE_WITHIN, Expectation.parse("trace manifest_start").within());
    }

    @Test
    void parsesDumpAndRejectsNonsense() {

        Expectation dump = Expectation.parse("dump ghosts.length=1 ghosts[0].weakened=true");
        assertEquals(Expectation.Kind.DUMP, dump.kind());
        assertNull(dump.type());
        assertEquals(0, dump.within());
        assertThrows(IllegalArgumentException.class, () -> Expectation.parse("frobnicate x=1"));
        assertThrows(IllegalArgumentException.class, () -> Expectation.parse("trace"));
        assertThrows(IllegalArgumentException.class, () -> Expectation.parse("dump"));
        assertThrows(IllegalArgumentException.class, () -> Expectation.parse("trace foo within 10 x=1"));
        assertThrows(IllegalArgumentException.class, () -> Expectation.parse("trace foo noOperator"));
    }

    @Test
    void matchesEventsByTypeAndFields() {

        Expectation expectation = Expectation.parse("trace haunt action=knocking done=true");
        assertTrue(expectation.matchesEvent(json("{\"t\":5,\"type\":\"haunt\",\"action\":\"knocking\",\"done\":true}")));
        assertFalse(expectation.matchesEvent(json("{\"t\":5,\"type\":\"haunt\",\"action\":\"knocking\",\"done\":false}")));
        assertFalse(expectation.matchesEvent(json("{\"t\":5,\"type\":\"haunt_none\",\"action\":\"knocking\",\"done\":true}")));
    }

    @Test
    void comparesNumbersNumericallyAndTextExactly() {

        JsonObject event = json("{\"type\":\"x\",\"a\":0.1,\"b\":\"abc\"}");
        assertTrue(Expectation.parse("trace x a=0.10").matchesEvent(event));
        assertTrue(Expectation.parse("trace x a>0.05 a<=0.1").matchesEvent(event));
        assertFalse(Expectation.parse("trace x a>0.1").matchesEvent(event));
        assertTrue(Expectation.parse("trace x b=abc").matchesEvent(event));
        assertFalse(Expectation.parse("trace x b>abc").matchesEvent(event));
    }

    @Test
    void dumpPathsSupportIndicesWildcardsAndLength() {

        JsonObject dump = json("{\"ghosts\":[{\"name\":\"Ada Crane\",\"presence\":6.0,\"weakened\":false},{\"name\":\"Eli Marsh\",\"presence\":4.0,\"weakened\":true}]}");
        assertTrue(Expectation.parse("dump ghosts.length=2").matchesDump(dump));
        assertTrue(Expectation.parse("dump ghosts[1].weakened=true").matchesDump(dump));
        assertTrue(Expectation.parse("dump ghosts[*].weakened=true").matchesDump(dump));
        assertFalse(Expectation.parse("dump ghosts[0].weakened=true").matchesDump(dump));
        assertTrue(Expectation.parse("dump ghosts[*].presence<5").matchesDump(dump));
        assertFalse(Expectation.parse("dump ghosts[5].presence<5").matchesDump(dump));
        assertFalse(Expectation.parse("dump players.length=1").matchesDump(dump));
    }

    @Test
    void closestPrefersSameTypeWithMostConditions() {

        Expectation expectation = Expectation.parse("trace haunt action=knocking done=true");
        JsonObject a = json("{\"t\":1,\"type\":\"haunt\",\"action\":\"footsteps\",\"done\":true}");
        JsonObject b = json("{\"t\":2,\"type\":\"haunt\",\"action\":\"knocking\",\"done\":false}");
        JsonObject c = json("{\"t\":3,\"type\":\"goal_change\"}");
        assertEquals(b, expectation.closest(List.of(a, b, c)));
        assertEquals(c, Expectation.parse("trace backfire").closest(List.of(a, b, c)));
        assertNull(expectation.closest(List.of()));
    }
}
