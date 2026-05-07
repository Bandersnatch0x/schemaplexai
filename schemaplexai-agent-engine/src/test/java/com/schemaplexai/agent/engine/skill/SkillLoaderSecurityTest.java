package com.schemaplexai.agent.engine.skill;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {SkillLoader.class})
class SkillLoaderSecurityTest {

    @Autowired
    private SkillLoader skillLoader;

    @Test
    void shouldRejectNameLongerThan64Chars() {
        String longName = "a".repeat(65);
        String markdown = "---\nname: " + longName + "\ndescription: test\n---\n# Instructions";
        assertThrows(ValidationException.class, () -> skillLoader.parse(markdown));
    }

    @Test
    void shouldRejectDescriptionLongerThan500Chars() {
        String longDesc = "a".repeat(501);
        String markdown = "---\nname: test\ndescription: " + longDesc + "\n---\n# Instructions";
        assertThrows(ValidationException.class, () -> skillLoader.parse(markdown));
    }

    @Test
    void shouldRejectHtmlTagsInContent() {
        String markdown = "---\nname: test\ndescription: test\n---\n<script>alert('xss')</script>";
        assertThrows(ValidationException.class, () -> skillLoader.parse(markdown));
    }

    @Test
    void shouldParseValidMarkdown() {
        String markdown = "---\nname: my-skill\ndescription: A test skill\n---\n# Instructions\nDo something useful.";
        SkillDefinition def = skillLoader.parse(markdown);
        assertEquals("my-skill", def.name());
        assertEquals("A test skill", def.description());
        assertTrue(def.instructions().contains("Do something useful"));
    }

    @Test
    void shouldRejectNullName() {
        String markdown = "---\ndescription: test\n---\n# Instructions";
        assertThrows(ValidationException.class, () -> skillLoader.parse(markdown));
    }

    @Test
    void shouldRejectBlankContent() {
        assertThrows(ValidationException.class, () -> skillLoader.parse(""));
        assertThrows(ValidationException.class, () -> skillLoader.parse(null));
    }

    @Test
    void shouldAcceptDescriptionWithinLimit() {
        String desc500 = "a".repeat(500);
        String markdown = "---\nname: test\ndescription: " + desc500 + "\n---\n# Instructions";
        SkillDefinition def = skillLoader.parse(markdown);
        assertEquals("test", def.name());
        assertEquals(desc500, def.description());
    }
}
