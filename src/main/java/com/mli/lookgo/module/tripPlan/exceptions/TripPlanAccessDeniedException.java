package com.mli.lookgo.module.tripPlan.exceptions;

/**
 * 使用者嘗試操作或分享非本人擁有的旅程規劃時拋出的例外。
 *
 * @author D5042101
 * @since 2026.07.06
 */
public class TripPlanAccessDeniedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TripPlanAccessDeniedException(String message) {
        super(message);
    }
}
