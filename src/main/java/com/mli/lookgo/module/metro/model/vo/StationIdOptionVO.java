package com.mli.lookgo.module.metro.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用於回傳車站管理頁面下拉選單所需車站選項的物件（車站 id＋中文名稱）。
 *
 * @author D5042101
 * @since 2026.07.05
 */
@Schema(description = "回傳車站管理頁面下拉選單所需車站選項的物件")
public class StationIdOptionVO {

    @Schema(description = "車站 id", example = "1")
    private Integer id;

    @Schema(description = "車站中文名稱", example = "淡水")
    private String nameZhTw;

    public StationIdOptionVO() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNameZhTw() {
        return nameZhTw;
    }

    public void setNameZhTw(String nameZhTw) {
        this.nameZhTw = nameZhTw;
    }

    @Override
    public String toString() {
        return "StationIdOptionVO{id=" + id + ", nameZhTw='" + nameZhTw + '\'' + '}';
    }
}
