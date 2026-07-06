package com.mli.lookgo.module.tripPlan.exceptions;

/**
 * 找不到指定旅程規劃，或該旅程規劃已被軟刪除時拋出的例外。
 *
 * @author D5042101
 * @since 2026.07.06
 */
public class TripPlanNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TripPlanNotFoundException(String message) {
        super(message);
    }
}
