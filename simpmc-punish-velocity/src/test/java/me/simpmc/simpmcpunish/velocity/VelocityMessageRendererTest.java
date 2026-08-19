package me.simpmc.simpmcpunish.velocity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class VelocityMessageRendererTest {
    @Test
    void auditReasonIsCollapsedToOneLineAndTruncated() {
        VelocityMessageRenderer renderer = new VelocityMessageRenderer(
                new VelocityMessages("<red>ban</red>", "kick", "error"), 13);

        assertEquals("line one | l…", renderer.auditReason("line one\nline two"));
    }
}
