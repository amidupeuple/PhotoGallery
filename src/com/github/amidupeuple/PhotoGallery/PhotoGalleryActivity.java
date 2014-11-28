package com.github.amidupeuple.PhotoGallery;

import android.support.v4.app.Fragment;

/**
 * Created by dpivovar on 28.11.2014.
 */
public class PhotoGalleryActivity extends SingleFragmentActivity {
    @Override
    protected Fragment createFragment() {
        return new PhotoGalleryFragment();
    }
}
