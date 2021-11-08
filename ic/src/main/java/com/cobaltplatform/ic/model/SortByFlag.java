package com.cobaltplatform.ic.model;

import com.cobaltplatform.ic.backend.model.response.DispositionResponse;

import java.util.Comparator;

public class SortByFlag implements Comparator<DispositionResponse> {
    public int compare(DispositionResponse a, DispositionResponse b) {
        if (a.getFlag().getId() == b.getFlag().getId()) {
            return 0;
        }

        if (a.getFlag().getId() == DispositionFlag.NEEDS_INITIAL_SAFETY_PLANNING.getId()) {
            return -1;
        }

        if (b.getFlag().getId() == DispositionFlag.NEEDS_INITIAL_SAFETY_PLANNING.getId()) {
            return 1;
        }

        if (a.getFlag().getId() == DispositionFlag.NEEDS_SAFETY_PLANNING_FOLLOW.getId()) {
            return -1;
        }

        if (b.getFlag().getId() == DispositionFlag.NEEDS_SAFETY_PLANNING_FOLLOW.getId()) {
            return 1;
        }

        if (a.getFlag().getId() == DispositionFlag.NEEDS_FURTHER_ASSESSMENT_WITH_MHIC.getId()) {
            return -1;
        }

        if (b.getFlag().getId() == DispositionFlag.NEEDS_FURTHER_ASSESSMENT_WITH_MHIC.getId()) {
            return 1;
        }

        return a.getCreatedAt().compareTo(b.getCreatedAt());
    }
}

