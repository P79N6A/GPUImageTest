/*
 * Copyright (C) 2012 CyberAgent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.co.cyberagent.android.gpuimage;

import android.graphics.PointF;
import android.opengl.GLES20;

public class GPUImageTwoPassTextureSamplingFilter extends GPUImageTwoPassFilter {
    public GPUImageTwoPassTextureSamplingFilter(String firstVertexShader, String firstFragmentShader,
                                                String secondVertexShader, String secondFragmentShader) {
        super(firstVertexShader, firstFragmentShader,
                secondVertexShader, secondFragmentShader);
    }

    @Override
    public void onInit() {
        super.onInit();
        initTexelOffsets();
    }

    protected void initTexelOffsets() {
        float ratio = getHorizontalTexelOffsetRatio();
        GPUImageFilter filter = mFilters.get(0);
        int texelWidthOffsetLocation = GLES20.glGetUniformLocation(filter.getProgram(), "texelWidthOffset");
        int texelHeightOffsetLocation = GLES20.glGetUniformLocation(filter.getProgram(), "texelHeightOffset");
        filter.setFloat(texelWidthOffsetLocation, ratio / mOutputHeight);
        filter.setFloat(texelHeightOffsetLocation, 0);

        ratio = getVerticalTexelOffsetRatio();
        filter = mFilters.get(1);
        texelWidthOffsetLocation = GLES20.glGetUniformLocation(filter.getProgram(), "texelWidthOffset");
        texelHeightOffsetLocation = GLES20.glGetUniformLocation(filter.getProgram(), "texelHeightOffset");
        filter.setFloat(texelWidthOffsetLocation, 0);
        filter.setFloat(texelHeightOffsetLocation, ratio / mOutputHeight);
    }

    protected void initStepAndWeight(float[] steps, float[] weights){
        GPUImageFilter filter = mFilters.get(0);
        int stepsLoc = GLES20.glGetUniformLocation(filter.getProgram(), "stepOffset");
        int weightsLoc = GLES20.glGetUniformLocation(filter.getProgram(), "standardGaussianWeights");
        filter.setFloatArray(stepsLoc, steps);
        filter.setFloatArray(weightsLoc, weights);

        filter = mFilters.get(1);
        stepsLoc = GLES20.glGetUniformLocation(filter.getProgram(), "stepOffset");
        weightsLoc = GLES20.glGetUniformLocation(filter.getProgram(), "standardGaussianWeights");
        filter.setFloatArray(stepsLoc, steps);
        filter.setFloatArray(weightsLoc, weights);
    }

    protected void initSampleSize(int size, int optSize){
        GPUImageFilter filter = mFilters.get(0);
        int sampleSizeLoc = GLES20.glGetUniformLocation(filter.getProgram(), "sampleSize");
        int optimizeSampleSizeLoc = GLES20.glGetUniformLocation(filter.getProgram(), "optimizeSampleSize");
        filter.setInteger(sampleSizeLoc, size);
        filter.setInteger(optimizeSampleSizeLoc, optSize);

        filter = mFilters.get(1);
        sampleSizeLoc = GLES20.glGetUniformLocation(filter.getProgram(), "sampleSize");
        optimizeSampleSizeLoc = GLES20.glGetUniformLocation(filter.getProgram(), "optimizeSampleSize");
        filter.setInteger(sampleSizeLoc, size);
        filter.setInteger(optimizeSampleSizeLoc, optSize);
    }

    protected void updateUinformValue(String locationname, float value){
        for(int i =0; i< mFilters.size(); i++){
            GPUImageFilter filter = mFilters.get(i);
            int valueLocation = GLES20.glGetUniformLocation(filter.getProgram(), locationname);
            filter.setFloat(valueLocation, value);
        }
    }

    protected void updateUinformValue(String locationname, PointF center){
        for(int i =0; i< mFilters.size(); i++){
            GPUImageFilter filter = mFilters.get(i);
            int valueLocation = GLES20.glGetUniformLocation(filter.getProgram(), locationname);
            filter.setPoint(valueLocation, center);
        }
    }

    @Override
    public void onOutputSizeChanged(int width, int height) {
        super.onOutputSizeChanged(width, height);
        initTexelOffsets();
    }

    public float getVerticalTexelOffsetRatio() {
        return 1f;
    }

    public float getHorizontalTexelOffsetRatio() {
        return 1f;
    }
}
