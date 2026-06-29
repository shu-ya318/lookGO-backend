package com.mli.lookgo.module.metro.exceptions;

/**
 * 找不到指定車站時拋出的例外。
 *
 * @author D5042101
 * @since 2026.06.29
 */
public class StationNotFoundException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;

    public StationNotFoundException(String message) {
        super(message);
    }
}
