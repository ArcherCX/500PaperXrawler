package com.archer.s00paperxrawler.db;

import android.net.Uri;
import android.util.SparseArray;

import com.archer.s00paperxrawler.BuildConfig;

/**
 * Created by Chen Xin on 2018/8/16.
 * PaperInfo表的Contract
 */
public interface PaperInfoContract {
    String TABLE_NAME = "paper_info";
    String VIEW_UNUSED_PAPER = "view_unused_paper";
    String VIEW_HISTORY = "view_history";
    Uri AUTHORITY_URI = Uri.parse("content://" + BuildConfig.CONTENT_PROVIDER_AUTHORITY);
    Uri PHOTO_DETAIL_CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, PathSegment.PHOTO_DETAIL);

    interface Columns {
        String PHOTO_DETAIL_URL = "photo_detail_url";//照片详情页url
        String ASPECT_RATIO = "aspect_ratio";//图片宽高比
        String PHOTO_URL = "photo_url";//图片url
        String USED = "used";//是否使用
        String SETTLED_DATE = "settled_date";//设置为壁纸的日期
        String PH = "ph";//摄影
        String PHOTO_NAME = "photo_name";//照片名字
    }

    interface PathSegment {
        String PHOTO_DETAIL = "photo_detail";
    }
}
