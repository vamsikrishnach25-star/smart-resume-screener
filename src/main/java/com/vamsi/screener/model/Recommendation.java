package com.vamsi.screener.model;

/** Hiring recommendation derived from the 1-10 match score. */
public enum Recommendation {

    STRONG_MATCH,
    GOOD_MATCH,
    POSSIBLE_MATCH,
    NOT_A_MATCH;

    /**
     * Maps a 1-10 match score onto a recommendation band.
     *
     * @param score match score between 1 and 10
     * @return the band the score falls into
     */
    public static Recommendation fromScore(double score) {
        if (score >= 8.5D) {
            return STRONG_MATCH;
        }
        if (score >= 7.0D) {
            return GOOD_MATCH;
        }
        if (score >= 5.0D) {
            return POSSIBLE_MATCH;
        }
        return NOT_A_MATCH;
    }

    /** @return true when the candidate should appear on the shortlist. */
    public boolean isShortlisted() {
        return this == STRONG_MATCH || this == GOOD_MATCH;
    }
}
