package jp.co.cyberagent.android.gpuimage.sample.videoPlayer.filter.advance;


import android.content.Context;

import jp.co.cyberagent.android.gpuimage.sample.videoPlayer.filter.base.OpenGlUtils;


public class B612AdoreFilter extends B612BaseFilter {


    public B612AdoreFilter(Context context) {
        super(context);
    }

    @Override
    protected int getInputTexture() {
        return OpenGlUtils.loadTexture(mContext, "filter/adore_new.png");
    }


}
