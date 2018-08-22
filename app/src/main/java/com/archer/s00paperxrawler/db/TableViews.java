package com.archer.s00paperxrawler.db;

/**
 * Created by Chen Xin on 2018/8/22.
 * Table、View整合，虽然目前只有一张表，保不齐以后不新增
 */
public interface TableViews {
    interface TABLES {
        String PAPER_INFO = PaperInfoContract.TABLE_NAME;
    }

    interface VIEWS {
        String VIEW_UNUSED_PHOTOS = PaperInfoContract.VIEW_UNUSED_PHOTOS;
        String VIEW_UNDOWNLOAD_PHOTOS = PaperInfoContract.VIEW_UNDOWNLOAD_PHOTOS;
        String VIEW_HISTORY = PaperInfoContract.VIEW_HISTORY;
    }
}
