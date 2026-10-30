package org.example.ishtirak.common.dto;

import java.util.List;

public record ReadingBatchResponse(
        int saved,
        List<ReadingIssue> rejected,
        List<ReadingIssue> warnings
) {
}
