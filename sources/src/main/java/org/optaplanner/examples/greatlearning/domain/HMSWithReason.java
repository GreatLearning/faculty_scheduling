package org.optaplanner.examples.greatlearning.domain;

import org.optaplanner.core.api.domain.solution.Solution;
import org.optaplanner.core.api.score.AbstractScore;
import org.optaplanner.core.api.score.FeasibilityScore;
import org.optaplanner.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;

import java.util.List;

public class HMSWithReason extends AbstractScore<HMSWithReason>
        implements FeasibilityScore<HMSWithReason> {
    private static final String HARD_LABEL = "hard";
    private static final String MEDIUM_LABEL = "medium";
    private static final String SOFT_LABEL = "soft";

    public static HMSWithReason parseScore(String scoreString) {
        String[] levelStrings = parseLevelStrings(HardMediumSoftScore.class, scoreString,
                HARD_LABEL, MEDIUM_LABEL, SOFT_LABEL);
        int hardScore = parseLevelAsInt(HardMediumSoftScore.class, scoreString, levelStrings[0]);
        int mediumScore = parseLevelAsInt(HardMediumSoftScore.class, scoreString, levelStrings[1]);
        int softScore = parseLevelAsInt(HardMediumSoftScore.class, scoreString, levelStrings[2]);
        return valueOf(hardScore, mediumScore, softScore, null);
    }

    public static HMSWithReason valueOf(int hardScore, int mediumScore, int softScore, List<String> reasons) {
        return new HMSWithReason(hardScore, mediumScore, softScore, reasons);
    }

    // ************************************************************************
    // Fields
    // ************************************************************************

    private final int hardScore;
    private final int mediumScore;
    private final int softScore;

    public List<String> getReasons() {
        return reasons;
    }

    private List<String> reasons = null;


    /**
     * Private default constructor for default marshalling/unmarshalling of unknown frameworks that use reflection.
     * Such integration is always inferior to the specialized integration modules, such as
     * optaplanner-persistence-jpa, optaplanner-persistence-xstream, optaplanner-persistence-jaxb, ...
     */
    @SuppressWarnings("unused")
    private HMSWithReason() {
        hardScore = Integer.MIN_VALUE;
        mediumScore = Integer.MIN_VALUE;
        softScore = Integer.MIN_VALUE;
    }

    private HMSWithReason(int hardScore, int mediumScore, int softScore, List<String> reasons) {
        this.hardScore = hardScore;
        this.mediumScore = mediumScore;
        this.softScore = softScore;
        this.reasons = reasons;
    }

    /**
     * The total of the broken negative hard constraints and fulfilled positive hard constraints.
     * Their weight is included in the total.
     * The hard score is usually a negative number because most use cases only have negative constraints.
     *
     * @return higher is better, usually negative, 0 if no hard constraints are broken/fulfilled
     */
    public int getHardScore() {
        return hardScore;
    }

    /**
     * The total of the broken negative medium constraints and fulfilled positive medium constraints.
     * Their weight is included in the total.
     * The medium score is usually a negative number because most use cases only have negative constraints.
     * <p>
     * In a normal score comparison, the medium score is irrelevant if the 2 scores don't have the same hard score.
     *
     * @return higher is better, usually negative, 0 if no hard constraints are broken/fulfilled
     */
    public int getMediumScore() {
        return mediumScore;
    }

    /**
     * The total of the broken negative soft constraints and fulfilled positive soft constraints.
     * Their weight is included in the total.
     * The soft score is usually a negative number because most use cases only have negative constraints.
     * <p>
     * In a normal score comparison, the soft score is irrelevant if the 2 scores don't have the same hard and medium score.
     *
     * @return higher is better, usually negative, 0 if no soft constraints are broken/fulfilled
     */
    public int getSoftScore() {
        return softScore;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    /**
     * A {@link Solution} is feasible if it has no broken hard constraints.
     *
     * @return true if the {@link #getHardScore()} is 0 or higher
     */
    public boolean isFeasible() {
        return getHardScore() >= 0;
    }

    public HMSWithReason add(HMSWithReason augment) {
        return new HMSWithReason(
                hardScore + augment.getHardScore(),
                mediumScore + augment.getMediumScore(),
                softScore + augment.getSoftScore(), augment.getReasons());
    }

    public HMSWithReason subtract(HMSWithReason subtrahend) {
        return new HMSWithReason(
                hardScore - subtrahend.getHardScore(),
                mediumScore - subtrahend.getMediumScore(),
                softScore - subtrahend.getSoftScore(), subtrahend.getReasons());
    }

    public HMSWithReason multiply(double multiplicand) {
        return new HMSWithReason(
                (int) Math.floor(hardScore * multiplicand),
                (int) Math.floor(mediumScore * multiplicand),
                (int) Math.floor(softScore * multiplicand), reasons);
    }

    public HMSWithReason divide(double divisor) {
        return new HMSWithReason(
                (int) Math.floor(hardScore / divisor),
                (int) Math.floor(mediumScore / divisor),
                (int) Math.floor(softScore / divisor), reasons);
    }

    public HMSWithReason power(double exponent) {
        return new HMSWithReason(
                (int) Math.floor(Math.pow(hardScore, exponent)),
                (int) Math.floor(Math.pow(mediumScore, exponent)),
                (int) Math.floor(Math.pow(softScore, exponent)), reasons);
    }

    public HMSWithReason negate() {
        return new HMSWithReason(-hardScore, -mediumScore, -softScore, reasons);
    }

    public Number[] toLevelNumbers() {
        return new Number[]{hardScore, mediumScore, softScore};
    }

    public boolean equals(Object o) {
        // A direct implementation (instead of EqualsBuilder) to avoid dependencies
        if (this == o) {
            return true;
        } else if (o instanceof HardMediumSoftScore) {
            HardMediumSoftScore other = (HardMediumSoftScore) o;
            return hardScore == other.getHardScore()
                    && mediumScore == other.getMediumScore()
                    && softScore == other.getSoftScore();
        } else {
            return false;
        }
    }

    public int hashCode() {
        // A direct implementation (instead of HashCodeBuilder) to avoid dependencies
        return ((((17 * 37)
                + hardScore)) * 37
                + mediumScore) * 37
                + softScore;
    }

    public int compareTo(HMSWithReason other) {
        // A direct implementation (instead of CompareToBuilder) to avoid dependencies
        if (hardScore != other.getHardScore()) {
            if (hardScore < other.getHardScore()) {
                return -1;
            } else {
                return 1;
            }
        } else {
            if (mediumScore != other.getMediumScore()) {
                if (mediumScore < other.getMediumScore()) {
                    return -1;
                } else {
                    return 1;
                }
            } else {
                if (softScore < other.getSoftScore()) {
                    return -1;
                } else if (softScore > other.getSoftScore()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        }
    }

    public String toString() {
        return hardScore + HARD_LABEL + "/" + mediumScore + MEDIUM_LABEL + "/" + softScore + SOFT_LABEL;
    }

}
