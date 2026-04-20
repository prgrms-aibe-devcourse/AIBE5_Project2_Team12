package com.generic4.itda.service.recommend.scoring;

import org.springframework.stereotype.Component;

@Component
public class CareerAdjustmentCalculator {

    private static final double IN_RANGE_BONUS = 0.08;
    private static final double MIN_SHORTAGE_UNIT_PENALTY = 0.04;
    private static final double MAX_EXCESS_UNIT_PENALTY = 0.02;
    private static final double MIN_SHORTAGE_MAX_PENALTY = -0.12;
    private static final double MAX_EXCESS_MAX_PENALTY = -0.06;

    public double calculate(int candidateCareerYears, Integer careerMinYears, Integer careerMaxYears) {
        if (careerMinYears == null && careerMaxYears == null) {
            return 0.0;
        }

        if (careerMinYears != null && candidateCareerYears < careerMinYears) {
            int shortage = careerMinYears - candidateCareerYears;
            double penalty = -(shortage * MIN_SHORTAGE_UNIT_PENALTY);
            return Math.max(MIN_SHORTAGE_MAX_PENALTY, penalty);
        }

        if (careerMaxYears != null && candidateCareerYears > careerMaxYears) {
            int excess = candidateCareerYears - careerMaxYears;
            double penalty = -(excess * MAX_EXCESS_UNIT_PENALTY);
            return Math.max(MAX_EXCESS_MAX_PENALTY, penalty);
        }

        return IN_RANGE_BONUS;
    }
}
