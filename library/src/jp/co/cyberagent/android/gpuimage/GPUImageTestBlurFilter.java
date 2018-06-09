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

import android.util.Log;

/**
 * A hardware-accelerated 9-hit box blur of an image
 *
 * scaling: for the size of the applied blur, default of 1.0
 */
public class GPUImageTestBlurFilter extends GPUImageTwoPassTextureSamplingFilter {
    private static final String VERTEX_SHADER =
            "attribute vec4 position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
             "varying vec2 textureCoordinate;\n" +
            " \n" +
            "const int MAX_VERTICAL_SAMPLES = 15;\n" +
            "uniform float texelWidthOffset;\n" +
            "uniform float texelHeightOffset;\n" +
            "uniform float stepOffset[MAX_VERTICAL_SAMPLES]; \n" +
            "uniform int sampleSize;\n" +
            "varying vec2 blurCoordinates[MAX_VERTICAL_SAMPLES];\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = position;\n" +
            "    textureCoordinate = inputTextureCoordinate.xy;\n" +
            "    vec2 singleStepOffset = vec2(texelWidthOffset, texelHeightOffset);\n" +
            "    blurCoordinates[0] = inputTextureCoordinate.xy;" +
            "    for (int i = 0; i < sampleSize; i++) {\n" +
            "        int first = i * 2 + 1;\n" +
            "        int second = i * 2 + 2;\n" +
            "        blurCoordinates[first] = inputTextureCoordinate.xy + singleStepOffset * stepOffset[i];\n" +
            "        blurCoordinates[second] = inputTextureCoordinate.xy - singleStepOffset * stepOffset[i];\n" +
            "    }\n" +
            "}";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "uniform sampler2D inputImageTexture;\n" +
            "varying highp vec2 textureCoordinate;\n" +
            "const int GAUSSIAN_WEIGHT_NUMBERS = 36;\n" +
            "const int MAX_VERTICAL_SAMPLES = 15;\n" +
            "uniform float texelWidthOffset;\n" +
            "uniform float texelHeightOffset;\n" +
            "uniform int sampleSize;\n" +
            "uniform int optimizeSampleSize;\n" +
            "\n" +
            "uniform float standardGaussianWeights[GAUSSIAN_WEIGHT_NUMBERS]; \n" +
            "varying vec2 blurCoordinates[MAX_VERTICAL_SAMPLES];\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "    highp vec4 sum = vec4(0.0);\n" +
            "    lowp vec4 fragColor=texture2D(inputImageTexture,textureCoordinate);\n" +
            "    sum += texture2D(inputImageTexture, blurCoordinates[0]) * standardGaussianWeights[0]; " +
            "    for (int i = 0; i < sampleSize; i++) {\n" +
            "        float firstWeight = standardGaussianWeights[i * 2 + 1];\n" +
            "        float secondWeight = standardGaussianWeights[i * 2 + 2];\n" +" " +
            "        float optimizedWeight = firstWeight + secondWeight;\n" +
            "        sum += texture2D(inputImageTexture, blurCoordinates[i * 2 + 1]) * optimizedWeight;\n" +
            "        sum += texture2D(inputImageTexture, blurCoordinates[i * 2 + 2]) * optimizedWeight;\n" +
            "    }\n" +
            "    vec2 singleStepOffset = vec2(texelWidthOffset, texelHeightOffset);\n" +
            "    if (optimizeSampleSize > sampleSize)\n" +
            "    {\n" +
            "       for (int j = sampleSize; j < optimizeSampleSize; j++) {\n" +
            "           float firstWeight = standardGaussianWeights[j * 2 + 1];\n" +
            "           float secondWeight = standardGaussianWeights[j * 2 + 2];\n" +
            "            \n" +
            "           float optimizedWeight = firstWeight + secondWeight;\n" +
            "           float optimizedOffset = (firstWeight * float(j * 2 + 1) + secondWeight * float(j * 2 + 2)) / optimizedWeight;\n" +
            "           sum += texture2D(inputImageTexture, blurCoordinates[0] + singleStepOffset * optimizedOffset) * optimizedWeight;\n" +
            "           sum += texture2D(inputImageTexture, blurCoordinates[0] - singleStepOffset * optimizedOffset) * optimizedWeight;\n" +
            "       }\n" +
            "    }\n" +
            "    gl_FragColor = sum;" +
//            "    gl_FragColor = texture2D(inputImageTexture,textureCoordinate);\n" +
            "}";


    private float blurSize = 1f;
    private float sigma = 2.0f;
    private float[] mStandardGaussianWeights;
    private float[] mStepOffset;
    private int mSampleSize;
    private int mOptSampleSize;

    /**
     * Construct new BoxBlurFilter with default blur size of 1.0.
     */
    public GPUImageTestBlurFilter() {
        this(1f);
    }


    public GPUImageTestBlurFilter(float blurSize) {
        super(VERTEX_SHADER, FRAGMENT_SHADER, VERTEX_SHADER, FRAGMENT_SHADER);
        this.blurSize = blurSize;
        Log.i("jerrypxiao", "VERTEX_SHADER = " + VERTEX_SHADER);
        Log.i("jerrypxiao", "FRAGMENT_SHADER = " + FRAGMENT_SHADER);
    }

    @Override
    public void onInit() {
        super.onInit();
        setOffsetAndWeights(9.8f);

        initStepAndWeight(mStepOffset, mStandardGaussianWeights);
        initSampleSize(mSampleSize, mOptSampleSize);
    }

    /**
     * A scaling for the size of the applied blur, default of 1.0
     *
     * @param blurValue
     */
    public void setBlurSize(final float blurValue) {

        runOnDraw(new Runnable() {
            @Override
            public void run() {
                setOffsetAndWeights(blurValue);
                Log.e("jerrypxiao", " setBlurSize mSampleSize = " + mSampleSize + ", mOptSampleSize=" + mOptSampleSize);
                initStepAndWeight(mStepOffset, mStandardGaussianWeights);
                initSampleSize(mSampleSize, mOptSampleSize);
            }
        });
    }

    private void caculateWeight(int blurRadius, float sigma){
        if (blurRadius < 1) {
            return;
        }
        float[] standardGaussianWeights = new float[blurRadius +1];

    }

    /* 算法只适用于采样数为奇数 */
    static private final int GAUSSIAN_WEIGHT_SAMPLES = 11; // 固定采样数，与shader中统一
    private float[] getWeight(float radius) {
        int blurRadius = 0;
        if (radius >= 1) {
            float minimumWeightToFindEdgeOfSamplingArea = 0.00390625F;
            blurRadius = (int) Math.floor(Math.sqrt(-2.0D * Math.pow((double) radius, 2.0D) *
                    Math.log((double) minimumWeightToFindEdgeOfSamplingArea * Math.sqrt(6.283185307179586D * Math.pow((double) radius, 2.0D)))));
            blurRadius += blurRadius % 2;
        }

        float sigma = radius;
        float[] standardGaussianWeights = new float[blurRadius + 1];
        float sumOfWeights = 0.0F;

        int numberOfOptimizedOffsets;
        for (numberOfOptimizedOffsets = 0; numberOfOptimizedOffsets < blurRadius + 1; ++numberOfOptimizedOffsets) {
            standardGaussianWeights[numberOfOptimizedOffsets] = (float) (1.0D / Math.sqrt(6.283185307179586D * Math.pow((double) sigma, 2.0D)) * Math.exp(-Math.pow((double) numberOfOptimizedOffsets, 2.0D) / (2.0D * Math.pow((double) sigma, 2.0D))));
            if (numberOfOptimizedOffsets == 0) {
                sumOfWeights += standardGaussianWeights[numberOfOptimizedOffsets];
            } else {
                sumOfWeights = (float) ((double) sumOfWeights + 2.0D * (double) standardGaussianWeights[numberOfOptimizedOffsets]);
            }
        }

        for (numberOfOptimizedOffsets = 0; numberOfOptimizedOffsets < blurRadius + 1; ++numberOfOptimizedOffsets) {
            standardGaussianWeights[numberOfOptimizedOffsets] /= sumOfWeights;
        }

        numberOfOptimizedOffsets = Math.min(blurRadius / 2 + blurRadius % 2, 7);
        float[] optimizedGaussianOffsets = new float[numberOfOptimizedOffsets];

        int trueNumberOfOptimizedOffsets;
        float firstWeight;
        for (trueNumberOfOptimizedOffsets = 0; trueNumberOfOptimizedOffsets < numberOfOptimizedOffsets; ++trueNumberOfOptimizedOffsets) {
            float shaderString = standardGaussianWeights[trueNumberOfOptimizedOffsets * 2 + 1];
            float currentOverlowTextureRead = standardGaussianWeights[trueNumberOfOptimizedOffsets * 2 + 2];
            firstWeight = shaderString + currentOverlowTextureRead;
            optimizedGaussianOffsets[trueNumberOfOptimizedOffsets] = (shaderString * (float) (trueNumberOfOptimizedOffsets * 2 + 1) + currentOverlowTextureRead * (float) (trueNumberOfOptimizedOffsets * 2 + 2)) / firstWeight;
        }

        trueNumberOfOptimizedOffsets = blurRadius / 2 + blurRadius % 2;

        float secondWeight;
        float optimizedWeight;
        int offsetIndex;
        float[] weight = new float[GAUSSIAN_WEIGHT_SAMPLES];
        for (int i = 0; i < GAUSSIAN_WEIGHT_SAMPLES; i++) {
            weight[i] = 0.0f;
        }
        for (offsetIndex = 0; offsetIndex < numberOfOptimizedOffsets; ++offsetIndex) {
            firstWeight = standardGaussianWeights[offsetIndex * 2 + 1];
            secondWeight = standardGaussianWeights[offsetIndex * 2 + 2];
            optimizedWeight = firstWeight + secondWeight;
            weight[offsetIndex] = optimizedWeight;
        }

        if (trueNumberOfOptimizedOffsets > numberOfOptimizedOffsets) {
            for (offsetIndex = numberOfOptimizedOffsets; offsetIndex < trueNumberOfOptimizedOffsets; ++offsetIndex) {
                firstWeight = standardGaussianWeights[offsetIndex * 2 + 1];
                secondWeight = standardGaussianWeights[offsetIndex * 2 + 2];
                optimizedWeight = firstWeight + secondWeight;
                weight[offsetIndex] = optimizedWeight;
            }
        }

        float sum = -weight[0];
        for (int i = 0; i < weight.length; i++) {
            sum += weight[i] * 2;
        }

        for (int i = 0; i < weight.length; i++) {
            weight[i] /= sum;
        }
        return weight;
    }

    private void setOffsetAndWeights(float radius){
        int blurRadius = 0;
        if (radius >= 1) {
            float minimumWeightToFindEdgeOfSamplingArea = 0.00390625F;
            blurRadius = (int) Math.floor(Math.sqrt(-2.0D * Math.pow((double) radius, 2.0D) *
                    Math.log((double) minimumWeightToFindEdgeOfSamplingArea * Math.sqrt(6.283185307179586D * Math.pow((double) radius, 2.0D)))));
            blurRadius += blurRadius % 2;
        }

        float sigma = radius;
        float[] standardGaussianWeights = new float[blurRadius + 1];
        float sumOfWeights = 0.0F;

        int numberOfOptimizedOffsets;
        for (numberOfOptimizedOffsets = 0; numberOfOptimizedOffsets < blurRadius + 1; ++numberOfOptimizedOffsets) {
            standardGaussianWeights[numberOfOptimizedOffsets] = (float) (1.0D / Math.sqrt(6.283185307179586D * Math.pow((double) sigma, 2.0D)) * Math.exp(-Math.pow((double) numberOfOptimizedOffsets, 2.0D) / (2.0D * Math.pow((double) sigma, 2.0D))));
            if (numberOfOptimizedOffsets == 0) {
                sumOfWeights += standardGaussianWeights[numberOfOptimizedOffsets];
            } else {
                sumOfWeights = (float) ((double) sumOfWeights + 2.0D * (double) standardGaussianWeights[numberOfOptimizedOffsets]);
            }
        }

        for (numberOfOptimizedOffsets = 0; numberOfOptimizedOffsets < blurRadius + 1; ++numberOfOptimizedOffsets) {
            standardGaussianWeights[numberOfOptimizedOffsets] /= sumOfWeights;
        }

        numberOfOptimizedOffsets = Math.min(blurRadius / 2 + blurRadius % 2, 7);
        float[] optimizedGaussianOffsets = new float[numberOfOptimizedOffsets];

        int trueNumberOfOptimizedOffsets;
        float firstWeight;
        for (trueNumberOfOptimizedOffsets = 0; trueNumberOfOptimizedOffsets < numberOfOptimizedOffsets; ++trueNumberOfOptimizedOffsets) {
            float shaderString = standardGaussianWeights[trueNumberOfOptimizedOffsets * 2 + 1];
            float currentOverlowTextureRead = standardGaussianWeights[trueNumberOfOptimizedOffsets * 2 + 2];
            firstWeight = shaderString + currentOverlowTextureRead;
            optimizedGaussianOffsets[trueNumberOfOptimizedOffsets] = (shaderString * (float) (trueNumberOfOptimizedOffsets * 2 + 1) + currentOverlowTextureRead * (float) (trueNumberOfOptimizedOffsets * 2 + 2)) / firstWeight;
        }

        trueNumberOfOptimizedOffsets = blurRadius / 2 + blurRadius % 2;

        mStepOffset = optimizedGaussianOffsets;
        mStandardGaussianWeights = standardGaussianWeights;
        mSampleSize = numberOfOptimizedOffsets;
        mOptSampleSize = trueNumberOfOptimizedOffsets;
    }



    @Override
    public float getVerticalTexelOffsetRatio() {
        return blurSize;
    }

    @Override
    public float getHorizontalTexelOffsetRatio() {
        return blurSize;
    }
}
