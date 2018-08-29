package com.archer.s00paperxrawler.db;

import android.net.Uri;

import com.archer.s00paperxrawler.BuildConfig;
import com.archer.s00paperxrawler.utils.Prefs;

/**
 * Created by Chen Xin on 2018/8/16.
 * PaperInfo表的Contract
 */
public interface PaperInfoContract {
    /**
     * 照片详情表
     */
    String TABLE_NAME = "paper_info";
    /**
     * 已下载到本地但未使用照片的详情View
     */
    String VIEW_UNUSED_PHOTOS = "view_unused_photos";
    /**
     * 未下载到本地照片的详情View
     */
    String VIEW_UNDOWNLOAD_PHOTOS = "view_undownload_photos";
    /**
     * 曾经被设置为壁纸的照片的详情View，可能已被删除
     */
    String VIEW_HISTORY = "view_history";
    Uri AUTHORITY_URI = Uri.parse("content://" + BuildConfig.CONTENT_PROVIDER_AUTHORITY);
    /**
     * 照片信息相关
     */
    Uri PAPER_INFO_URI = Uri.withAppendedPath(AUTHORITY_URI, PathSegment.PAPER_INFO);
    /*插入照片详情页信息*/
    //    Uri PHOTO_DETAIL_URI = Uri.withAppendedPath(AUTHORITY_URI, PathSegment.PHOTO_DETAIL);
    /**
     * 已下载到本地但未使用照片的详情
     */
    Uri UNUSED_PHOTOS_URI = Uri.withAppendedPath(AUTHORITY_URI, PathSegment.UNUSED_PHOTOS);
    /**
     * 未下载照片
     */
    Uri UNDOWNLOAD_PHOTOS_URI = Uri.withAppendedPath(AUTHORITY_URI, PathSegment.UNDOWNLOAD_PHOTOS);
    /**
     * 壁纸历史
     */
    Uri PAPER_HISTORY_URI = Uri.withAppendedPath(AUTHORITY_URI, PathSegment.PAPER_HISTORY);

    /**
     * 照片是否下载，具体文件夹见{@link Prefs#getPhotosCachePath()}
     */
//    Uri PHOTO_DOWNLOAD_URI = Uri.withAppendedPath(AUTHORITY_URI, PathSegment.PHOTO_DOWNLOAD);

    interface Columns {
        String PHOTO_DETAIL_URL = "photo_detail_url";//照片详情页url
        String ASPECT_RATIO = "aspect_ratio";//图片宽高比
        String PHOTO_URL = "photo_url";//图片url
        String USED = "used";//是否使用
        String SETTLED_DATE = "settled_date";//设置为壁纸的日期
        String PH = "ph";//摄影
        String PHOTO_NAME = "photo_name";//照片名字
        String DOWNLOAD = "download";//是否下载
        String PHOTO_ID = "photo_id";//图片id
    }

    interface PathSegment {
        String PAPER_INFO = "photo_info";
        String UNUSED_PHOTOS = "unused_photos";
        String UNDOWNLOAD_PHOTOS = "undownload_photos";
        String PAPER_HISTORY = "paper_history";
    }
}
