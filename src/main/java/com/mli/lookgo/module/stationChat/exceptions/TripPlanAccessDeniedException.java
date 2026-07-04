package com.mli.lookgo.module.stationChat.exceptions;

/**
 * 使用者嘗試分享非本人擁有的旅程規劃時拋出的例外。
 *
 * @author D5042101
 * @since 2026.07.04
 */
public class TripPlanAccessDeniedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TripPlanAccessDeniedException(String message) {
        super(message);
    }
}
