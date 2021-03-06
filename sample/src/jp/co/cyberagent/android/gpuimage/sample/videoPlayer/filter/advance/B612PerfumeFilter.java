package jp.co.cyberagent.android.gpuimage.sample.videoPlayer.filter.advance;


import android.content.Context;

import jp.co.cyberagent.android.gpuimage.sample.videoPlayer.filter.base.OpenGlUtils;

public class B612PerfumeFilter extends B612BaseFilter {


    public B612PerfumeFilter(Context context) {
        super(context);
    }

    @Override
    protected int getInputTexture() {
        return OpenGlUtils.loadTexture(mContext, "filter/perfume_new.png");
    }
}
