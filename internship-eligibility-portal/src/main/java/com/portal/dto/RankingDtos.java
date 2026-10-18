package com.portal.dto;

import com.portal.entity.InternshipApplication;

public class RankingDtos {

    /** composite = 0.6 * resume match score + 0.4 * ATR score (missing values count as 0). */
    public record RankedApplicant(InternshipApplication application, Double resumeMatchScore,
                                  Double atrScore, double compositeScore) {}
}
