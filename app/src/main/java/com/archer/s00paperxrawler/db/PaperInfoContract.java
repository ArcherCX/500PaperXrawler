package com.archer.s00paperxrawler.db;

import android.net.Uri;

import com.archer.s00paperxrawler.BuildConfig;
import com.archer.s00paperxrawler.utils.Prefs;

/**
 * Created by Chen Xin on 2018/8/16.
 * PaperInfo表的Contract
 */
public interface PaperInfoContract {
    String TABLE_NAME = "paper_info";
    String VIEW_UNUSED_PHOTOS = "view_unused_photos";
    String VIEW_UNDOWNLOAD_PHOTOS = "view_undownload_photos";
    String VIEW_HISTORY = "view_history";
    Uri AUTHORITY_URI = Uri.parse("content://" + BuildConfig.CONTENT_PROVIDER_AUTHORITY);
    /*插入照片详情页信息*/
    Uri PHOTO_DETAIL_URI = Uri.withAppendedPath(AUTHORITY_URI, PathSegment.PHOTO_DETAIL);
    /*未使用照片详情*/
    Uri UNUSED_PHOTOS_URI = Uri.withAppendedPath(AUTHORITY_URI, PathSegment.UNUSED_PHOTOS);
    /*未下载照片*/
    Uri UNDOWNLOAD_PHOTOS_URI = Uri.withAppendedPath(AUTHORITY_URI, PathSegment.UNDOWNLOAD_PHOTOS);
    /**
     * 插入照片路径，近照片名称，具体文件夹见{@link Prefs#getDefaultCachePath()}
     */
    Uri PHOTO_PATH_URI = Uri.withAppendedPath(AUTHORITY_URI, PathSegment.PHOTO_PATH);

    interface Columns {
        String PHOTO_DETAIL_URL = "photo_detail_url";//照片详情页url
        String ASPECT_RATIO = "aspect_ratio";//图片宽高比
        String PHOTO_URL = "photo_url";//图片url
        String USED = "used";//是否使用
        String SETTLED_DATE = "settled_date";//设置为壁纸的日期
        String PH = "ph";//摄影
        String PHOTO_NAME = "photo_name";//照片名字
        String DOWNLOAD = "download";//是否下载
        String FILE_NAME = "file_name";//文件名
    }

    interface PathSegment {
        String PHOTO_DETAIL = "photo_detail";
        String UNUSED_PHOTOS = "unused_photos";
        String UNDOWNLOAD_PHOTOS = "undownload_photos";
        String PHOTO_PATH = "photo_path";
    }
}
