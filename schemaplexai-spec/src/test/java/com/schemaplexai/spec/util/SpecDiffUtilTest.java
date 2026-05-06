package com.schemaplexai.spec.util;

import com.schemaplexai.spec.dto.DiffHunk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpecDiffUtilTest {

    @Test
    void identicalTextsProduceNoChanges() {
        String text = "line1\nline2\nline3";

        List<DiffHunk> hunks = SpecDiffUtil.diff(text, text);

        assertThat(hunks).isEmpty();
    }

    @Test
    void nullTextsProduceNoChanges() {
        List<DiffHunk> hunks = SpecDiffUtil.diff(null, null);

        assertThat(hunks).isEmpty();
    }

    @Test
    void nullTextA_and_nonNullTextB_producesAddedLines() {
        String textB = "new line 1\nnew line 2";

        List<DiffHunk> hunks = SpecDiffUtil.diff(null, textB);

        assertThat(hunks).hasSize(1);
        DiffHunk hunk = hunks.get(0);
        assertThat(hunk.getNewStart()).isEqualTo(1);
        assertThat(hunk.getNewLines()).isEqualTo(2);
        assertThat(hunk.getLines()).extracting("type").containsOnly("ADDED");
        assertThat(hunk.getLines()).extracting("content").containsExactly("new line 1", "new line 2");
    }

    @Test
    void nonNullTextA_and_nullTextB_producesRemovedLines() {
        String textA = "old line 1\nold line 2";

        List<DiffHunk> hunks = SpecDiffUtil.diff(textA, null);

        assertThat(hunks).hasSize(1);
        DiffHunk hunk = hunks.get(0);
        assertThat(hunk.getOldStart()).isEqualTo(1);
        assertThat(hunk.getOldLines()).isEqualTo(2);
        assertThat(hunk.getLines()).extracting("type").containsOnly("REMOVED");
        assertThat(hunk.getLines()).extracting("content").containsExactly("old line 1", "old line 2");
    }

    @Test
    void singleLineChanged() {
        String textA = "line1\nline2\nline3";
        String textB = "line1\nline2-modified\nline3";

        List<DiffHunk> hunks = SpecDiffUtil.diff(textA, textB);

        assertThat(hunks).hasSize(1);
        DiffHunk hunk = hunks.get(0);
        assertThat(hunk.getOldStart()).isEqualTo(2);
        assertThat(hunk.getNewStart()).isEqualTo(2);
        assertThat(hunk.getOldLines()).isEqualTo(1);
        assertThat(hunk.getNewLines()).isEqualTo(1);
        assertThat(hunk.getLines()).extracting("type").containsExactly("REMOVED", "ADDED");
    }

    @Test
    void lineAddedInMiddle() {
        String textA = "line1\nline3";
        String textB = "line1\nline2\nline3";

        List<DiffHunk> hunks = SpecDiffUtil.diff(textA, textB);

        assertThat(hunks).hasSize(1);
        DiffHunk hunk = hunks.get(0);
        assertThat(hunk.getNewLines()).isEqualTo(1);
        assertThat(hunk.getLines()).extracting("type").containsExactly("ADDED");
        assertThat(hunk.getLines()).extracting("content").containsExactly("line2");
    }

    @Test
    void lineRemovedFromMiddle() {
        String textA = "line1\nline2\nline3";
        String textB = "line1\nline3";

        List<DiffHunk> hunks = SpecDiffUtil.diff(textA, textB);

        assertThat(hunks).hasSize(1);
        DiffHunk hunk = hunks.get(0);
        assertThat(hunk.getOldLines()).isEqualTo(1);
        assertThat(hunk.getLines()).extracting("type").containsExactly("REMOVED");
        assertThat(hunk.getLines()).extracting("content").containsExactly("line2");
    }

    @Test
    void multipleSeparateChanges() {
        String textA = "A\nB\nC\nD\nE";
        String textB = "A\nB-modified\nC\nD-modified\nE";

        List<DiffHunk> hunks = SpecDiffUtil.diff(textA, textB);

        assertThat(hunks).hasSize(2);
        assertThat(hunks.get(0).getOldStart()).isEqualTo(2);
        assertThat(hunks.get(0).getNewStart()).isEqualTo(2);
        assertThat(hunks.get(1).getOldStart()).isEqualTo(4);
        assertThat(hunks.get(1).getNewStart()).isEqualTo(4);
    }

    @Test
    void emptyTexts() {
        List<DiffHunk> hunks = SpecDiffUtil.diff("", "");

        assertThat(hunks).isEmpty();
    }

    @Test
    void textAEmpty_textBNonEmpty() {
        List<DiffHunk> hunks = SpecDiffUtil.diff("", "new content");

        assertThat(hunks).hasSize(1);
        // Empty string vs "new content": LCS produces both REMOVED (of empty line) and ADDED
        assertThat(hunks.get(0).getLines()).extracting("type").containsExactly("REMOVED", "ADDED");
    }

    @Test
    void textNonEmpty_textBEmpty() {
        List<DiffHunk> hunks = SpecDiffUtil.diff("old content", "");

        assertThat(hunks).hasSize(1);
        // "old content" vs empty string: LCS produces both REMOVED and ADDED (of empty line)
        assertThat(hunks.get(0).getLines()).extracting("type").containsExactly("REMOVED", "ADDED");
    }

    @Test
    void prependLines() {
        String textA = "B\nC";
        String textB = "A\nB\nC";

        List<DiffHunk> hunks = SpecDiffUtil.diff(textA, textB);

        assertThat(hunks).hasSize(1);
        assertThat(hunks.get(0).getNewStart()).isEqualTo(1);
        assertThat(hunks.get(0).getLines()).extracting("type").containsExactly("ADDED");
    }

    @Test
    void appendLines() {
        String textA = "A\nB";
        String textB = "A\nB\nC";

        List<DiffHunk> hunks = SpecDiffUtil.diff(textA, textB);

        assertThat(hunks).hasSize(1);
        DiffHunk hunk = hunks.get(0);
        assertThat(hunk.getNewStart()).isEqualTo(3);
        assertThat(hunk.getLines()).extracting("type").containsExactly("ADDED");
    }

    @Test
    void completelyDifferentTexts() {
        String textA = "old1\nold2\nold3";
        String textB = "new1\nnew2\nnew3";

        List<DiffHunk> hunks = SpecDiffUtil.diff(textA, textB);

        assertThat(hunks).hasSize(1);
        DiffHunk hunk = hunks.get(0);
        assertThat(hunk.getOldLines()).isEqualTo(3);
        assertThat(hunk.getNewLines()).isEqualTo(3);
        assertThat(hunk.getLines()).extracting("type").containsExactly(
                "REMOVED", "REMOVED", "REMOVED", "ADDED", "ADDED", "ADDED");
    }

    @Test
    void unicodeContent() {
        String textA = "你好\n世界";
        String textB = "你好\n世界-modified";

        List<DiffHunk> hunks = SpecDiffUtil.diff(textA, textB);

        assertThat(hunks).hasSize(1);
        DiffHunk hunk = hunks.get(0);
        assertThat(hunk.getOldStart()).isEqualTo(2);
        assertThat(hunk.getOldLines()).isEqualTo(1);
        assertThat(hunk.getNewLines()).isEqualTo(1);
    }

    @Test
    void longTextProducesNoChangesWhenIdentical() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("line-").append(i).append("\n");
        }
        String text = sb.toString();

        List<DiffHunk> hunks = SpecDiffUtil.diff(text, text);

        assertThat(hunks).isEmpty();
    }
}
