package com.mli.lookgo.module.tripPlan.exceptions;

/**
 * 使用者已有同名的有效（未軟刪除）旅程規劃時拋出的例外。
 *
 * @author D5042101
 * @since 2026.07.12
 */
public class TripPlanNameDuplicationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TripPlanNameDuplicationException(String message) {
        super(message);
    }
}
