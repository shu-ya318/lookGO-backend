package com.mli.lookgo.module.metro.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.mli.lookgo.module.metro.dao.MetroDao;
import com.mli.lookgo.module.metro.model.entity.Line;
import com.mli.lookgo.module.metro.model.entity.Station;

/**
 * 處理前端查詢捷運資料相關的業務邏輯。
 *
 * @author D5042101
 * @since 2026.06.24
 */
@Service
public class MetroService {

    private final MetroDao metroDao;
    private static final Logger logger = LoggerFactory.getLogger(MetroService.class);

    /**
     * 讓 Spring 容器能在應用程式啟動時，自動注入所需的依賴。
     *
     * @param metroDao
     */
    public MetroService(MetroDao metroDao) {
        this.metroDao = metroDao;
    }

    /**
     * 取得所有路線資料。
     *
     * @return List<Line>
     */
    public List<Line> getAllLine() {
        logger.debug("開始查詢所有路線資料");
        return metroDao.getAllLine();
    }

    /**
     * 取得所有車站資料。
     *
     * @return List<Station>
     */
    public List<Station> getAllStation() {
        logger.debug("開始查詢所有車站資料");
        return metroDao.getAllStation();
    }
}
