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
public class GPUImageStandardBlurFilter extends GPUImageTwoPassTextureSamplingFilter {
    private static final String VERTEX_SHADER =
            "precision highp float;\n" +
            "attribute vec4 position;\n" +
                    "attribute vec4 inputTextureCoordinate;\n" +
                    "varying vec2 textureCoordinate;\n" +
                    "varying vec2 singleStepOffset;\n" +
                    " \n" +
                    "uniform float texelWidthOffset;\n" +
                    "uniform float texelHeightOffset;\n" +
                    "\n" +
                    "uniform int sampleSize;\n" +
                    "const int MAX_VERTICAL_SAMPLES = 21;\n" +
                    "varying vec2 blurCoordinates[21];\n" +
                    "varying vec2 test;\n" +
                    "\n" +
                    "void main()\n" +
                    "{\n" +
                    "    gl_Position = position;\n" +
                    "    textureCoordinate = inputTextureCoordinate.xy;\n" +
                    "    int multiplier = 0;\n" +
                    "    int minSample = 0;\n" +
                    "    if(sampleSize > MAX_VERTICAL_SAMPLES){\n" +
                    "        minSample = MAX_VERTICAL_SAMPLES;\n" +
                    "    }else{\n" +
                    "         minSample = sampleSize;\n" +
                    "    }\n" +
                    "    vec2 blurStep;\n" +
                    "    test.x = 0.0;\n" +
                    "    test.y = float((sampleSize - 1) / 2);\n" +
                    "    singleStepOffset = vec2(texelWidthOffset, texelHeightOffset);\n" +
                    "    for (int i = 0; i < minSample; i++) {\n" +
                    "        multiplier = (i - ((minSample - 1) / 2));\n" +
                    "        blurStep = float(multiplier) * singleStepOffset;\n" +
                    "        blurCoordinates[i] = inputTextureCoordinate.xy + blurStep;\n" +
                    "    }\n" +
                    "}";

    private static final String FRAGMENT_SHADER =
            "precision highp float;\n" +
                    "uniform sampler2D inputImageTexture;\n" +
                    "varying  vec2 textureCoordinate;\n" +
                    "varying  vec2 singleStepOffset;\n" +
                    "const int GAUSSIAN_WEIGHT_NUMBERS = 41;\n" +
                    "\n" +
                    "uniform int sampleSize;\n" +
                    "uniform float weight[GAUSSIAN_WEIGHT_NUMBERS]; \n" +
                    "varying vec2 blurCoordinates[21];\n" +
                    "varying vec2 test;\n" +
                    "\n" +
                    "void main()\n" +
                    "{\n" +
                    "    vec3 sum = vec3(0.0);\n" +
                    "    vec4 fragColor=texture2D(inputImageTexture,textureCoordinate);\n" +
                    "\n" +
                    "    sum += texture2D(inputImageTexture, textureCoordinate.xy).rgb * weight[0];\n" +
                    "\n" +
                    "    int  medium = GAUSSIAN_WEIGHT_NUMBERS - 1;\n" +
                    "    int minSample = int(test.y);\n" +
                    "    if(minSample > medium){\n" +
                    "        minSample = medium;\n" +
                    "    }\n" +
                    "    for (int i = 1; i <= minSample; i++) {\n" +
                    "        vec2 blurCoordinate1 = textureCoordinate.xy + singleStepOffset*float(i);\n" +
                    "        vec2 blurCoordinate2 = textureCoordinate.xy - singleStepOffset*float(i);\n" +
                    "        sum += texture2D(inputImageTexture, blurCoordinate1).rgb * weight[i];\n" +
                    "        sum += texture2D(inputImageTexture, blurCoordinate2).rgb * weight[i];\n" +
                    "    }\n" +
                    "\n" +
                    "    gl_FragColor = vec4(sum,fragColor.a);\n" +
//            "    gl_FragColor = texture2D(inputImageTexture,textureCoordinate);\n" +
                    "}";

    //fragment计算统一用float,特别是除法
    private float blurSize = 1f;
    private float sigma = 2.0f;
    private float[] mStandardGaussianWeights;
    private float[] mStepOffset;
    private int mSampleSize;
    private int mOptSampleSize;

    private float[] mWeights;

    /**
     * Construct new BoxBlurFilter with default blur size of 1.0.
     */
    public GPUImageStandardBlurFilter() {
        this(1f);
    }


    public GPUImageStandardBlurFilter(float blurSize) {
        super(VERTEX_SHADER, FRAGMENT_SHADER, VERTEX_SHADER, FRAGMENT_SHADER);
        this.blurSize = blurSize;
        //Log.i("jerrypxiao", "VERTEX_SHADER = " + VERTEX_SHADER);
        //Log.i("jerrypxiao", "FRAGMENT_SHADER = " + FRAGMENT_SHADER);
    }

    @Override
    public void onInit() {
        super.onInit();
        getWeight(9.8f);
        initWeights(mSampleSize, mWeights);
        //initStepAndWeight(mStepOffset, mStandardGaussianWeights);
        //initSampleSize(mSampleSize, mOptSampleSize);
    }

    /**
     * A scaling for the size of the applied blur, default of 1.0
     *
     * @param blurValue
     */
    public void setBlurSize(final float blurValue) {

        /*runOnDraw(new Runnable() {
            @Override
            public void run() {
                setOffsetAndWeights(blurValue);
                Log.e("jerrypxiao", " setBlurSize mSampleSize = " + mSampleSize + ", mOptSampleSize=" + mOptSampleSize);
                initStepAndWeight(mStepOffset, mStandardGaussianWeights);
                initSampleSize(mSampleSize, mOptSampleSize);
            }
        });*/
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                getWeight(blurValue);
                Log.e("jerrypxiao", " setBlurSize mSampleSize = " + mSampleSize + ", mWeights.length =" + mWeights.length);
                for(int i =0; i< mWeights.length; i++){
                    Log.e("jerrypxiao", "weight = ["+ i +"]" + mWeights[i]);
                }
                initWeights(mSampleSize, mWeights);
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


        Log.e("jerrypxiao", "getWeight blurRadius = " + blurRadius +", sigma =" + sigma);
        int numberOfOptimizedOffsets;
        for (numberOfOptimizedOffsets = 0; numberOfOptimizedOffsets < blurRadius + 1; ++numberOfOptimizedOffsets) {
            standardGaussianWeights[numberOfOptimizedOffsets] = (float) (1.0D / Math.sqrt(6.283185307179586D * Math.pow((double) sigma, 2.0D)) * Math.exp(-Math.pow((double) numberOfOptimizedOffsets, 2.0D) / (2.0D * Math.pow((double) sigma, 2.0D))));
            //Log.e("jerrypxiao", "getWeight Standadweight = ["+ numberOfOptimizedOffsets +"] =" + standardGaussianWeights[numberOfOptimizedOffsets]);
            if (numberOfOptimizedOffsets == 0) {
                sumOfWeights += standardGaussianWeights[numberOfOptimizedOffsets];
            } else {
                sumOfWeights = (float) ((double) sumOfWeights + 2.0D * (double) standardGaussianWeights[numberOfOptimizedOffsets]);
            }
        }

        for (numberOfOptimizedOffsets = 0; numberOfOptimizedOffsets < blurRadius + 1; ++numberOfOptimizedOffsets) {
            standardGaussianWeights[numberOfOptimizedOffsets] /= sumOfWeights;
            Log.e("jerrypxiao", "getWeight Standadweight = ["+ numberOfOptimizedOffsets +"] =" + standardGaussianWeights[numberOfOptimizedOffsets]);
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
        float[] weight = new float[blurRadius + 1];
        for (int i = 0; i < blurRadius + 1; i++) {
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

        mWeights = weight;
        mSampleSize = blurRadius + 1;
        Log.e("jerrypxiao", "getWeight mSampleSize = " + mSampleSize);
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
