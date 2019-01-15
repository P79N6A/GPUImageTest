package jp.co.cyberagent.android.gpuimage.sample.videoPlayer.render;


public interface IMovieRenderer {
    void surfaceCreated();
    void surfaceChanged(int width, int height);
    void doFrame();
    void surfaceDestroy();
}
