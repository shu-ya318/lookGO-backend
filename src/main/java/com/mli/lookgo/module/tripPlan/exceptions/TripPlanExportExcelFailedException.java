package com.mli.lookgo.module.tripPlan.exceptions;

/**
 * 匯出旅程規劃 excel 檔失敗時拋出的例外。
 *
 * @author D5042101
 * @since 2026.07.06
 */
public class TripPlanExportExcelFailedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TripPlanExportExcelFailedException(String message) {
        super(message);
    }
}
