package com.mli.lookgo.module.tripPlan.exceptions;

/**
 * 使用者建立的旅程規劃數量已達會員等級上限時拋出的例外。
 *
 * @author D5042101
 * @since 2026.07.06
 */
public class TripPlanLimitExceededException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TripPlanLimitExceededException(String message) {
        super(message);
    }
}
