package com.mli.lookgo.module.metro.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.mli.lookgo.module.metro.model.entity.Line;
import com.mli.lookgo.module.metro.model.entity.Station;

/**
 * 和資料庫交互，處理捷運資料查詢相關的操作。
 *
 * @author D5042101
 * @since 2026.06.24
 */
@Mapper
public interface MetroDao {

    /**
     * 批次新增或更新路線資料。
     *
     * @param lines
     */
    void upsertAllLine(@Param("lines") List<Line> lines);

    /**
     * 取得所有路線資料。
     *
     * @return List<Line>
     */
    List<Line> getAllLine();

    /**
     * 批次新增或更新車站資料。
     *
     * @param stations
     */
    void upsertAllStation(@Param("stations") List<Station> stations);

    /**
     * 取得所有車站資料。
     *
     * @return List<Station>
     */
    List<Station> getAllStation();
}
