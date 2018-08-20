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
    Uri AUTHORITY_URI = Uri.parse("content://" + BuildConfig.CONTENT_PROVIDER_AUTHORITY);
    Uri PHOTO_DETAIL_CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, PathSegment.PHOTO_DETAIL);

    interface Columns {
        String PHOTO_DETAIL_URL = "photo_detail_url";
        String ASPECT_RATIO = "aspect_ratio";
        String PHOTO_URL = "photo_url";
    }

    interface PathSegment {
        String PHOTO_DETAIL = "photo_detail";
    }
}
