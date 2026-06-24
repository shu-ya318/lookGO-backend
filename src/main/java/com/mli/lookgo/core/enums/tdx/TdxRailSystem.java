package com.mli.lookgo.core.enums.tdx;

/**
 * TDX 鐵路系統相關的列舉。
 * Enum 的 operatorCode（如 TRTC）必須與設定檔配置的值相同，才能觸發 Spring Boot 自動轉換。
 *
 * @author D5042101
 * @since 2026.06.24
 */
public enum TdxRailSystem {
	
	TRTC("TRTC", "臺北捷運"),
    KRTC("KRTC", "高雄捷運"),
    TYMC("TYMC", "桃園捷運"),
    TMRT("TMRT", "臺中捷運"),
    TRTCMG("TRTCMG", "貓空纜車"),
    NTMC("NTMC", "新北捷運"),
    KLRT("KLRT", "高雄輕軌");

    private final String operatorCode;
    private final String chineseName;

    TdxRailSystem(String operatorCode, String chineseName) {
    	 this.operatorCode = operatorCode;
         this.chineseName = chineseName;
    }

	public String getOperatorCode() {
		return operatorCode;
	}

	public String getChineseName() {
		return chineseName;
	}
}
