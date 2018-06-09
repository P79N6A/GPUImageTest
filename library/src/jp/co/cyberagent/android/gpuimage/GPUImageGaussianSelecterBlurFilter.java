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

/**
 * A more generalized 9x9 Gaussian blur filter
 * blurSize value ranging from 0.0 on up, with a default of 1.0
 */
public class GPUImageGaussianSelecterBlurFilter extends GPUImageTwoPassTextureSamplingFilter {


    public static final String VERTEX_SHADER1 =
            "varying vec2 textureCoordinate;\n" +
            "varying vec2 textureCoordinate2;" +
                    "\n" +
            "uniform sampler2D inputImageTexture;\n" +
            "uniform sampler2D inputImageTexture2;\n" +
                    "\n" +
            "uniform float excludeCircleRadius;\n" +
            "uniform vec2 excludeCirclePoint;\n" +
            "uniform float aspectRatio;\n" +
                    "\n" +
                    "void main()\n" +
                    "{\n" +
                    "	vec4 sharpImageColor = texture2D(inputImageTexture, textureCoordinate);\n" +
                    "	vec4 blurredImageColor = texture2D(inputImageTexture2, textureCoordinate2);\n" +
                    "	\n" +
                    "	// Calculate the positions for the blur\n" +
                    "	vec2 textureCoordinateToUse = vec2(textureCoordinate2.x, (textureCoordinate2.y * aspectRatio + 0.5 - 0.5 * aspectRatio));\n" +
                    "	float distanceFromCenter = distance(excludeCirclePoint, textureCoordinateToUse);\n" +
                    "   gl_FragColor = mix(sharpImageColor, blurredImageColor, smoothstep(excludeCircleRadius - excludeBlurSize, excludeCircleRadius, distanceFromCenter));\n" +
                    "}\n";

            ;
    public static final String VERTEX_SHADER =
            "attribute vec4 position;\n" +
                    "attribute vec4 inputTextureCoordinate;\n" +
                    "const int GAUSSIAN_SAMPLES = 9;\n" +
                    "uniform lowp float aspectRatio;\n" +
                    "\n" +
                    "uniform float texelWidthOffset;\n" +
                    "uniform float texelHeightOffset;\n" +
                    "uniform highp float radius;\n" +
                    "\n" +
                    "varying vec2 textureCoordinate;\n" +
                    "varying vec2 blurCoordinates[GAUSSIAN_SAMPLES];\n" +
                    "\n" +
                    "void main()\n" +
                    "{\n" +
                    "	gl_Position = position;\n" +
                    "	textureCoordinate = inputTextureCoordinate.xy;\n" +
                    "	\n" +
                    "	// Calculate the positions for the blur\n" +
                    "	int multiplier = 0;\n" +
                    "	vec2 blurStep;\n" +

                    "   vec2 singleStepOffset = vec2(texelHeightOffset, texelWidthOffset);\n" +
                    "    \n" +
                    "	for (int i = 0; i < GAUSSIAN_SAMPLES; i++)\n" +
                    "   {\n" +
                    "		multiplier = (i - ((GAUSSIAN_SAMPLES - 1) / 2));\n" +
                    "       // Blur in x (horizontal)\n" +
                    "       blurStep = float(multiplier) * singleStepOffset;\n" +
                    "		blurCoordinates[i] = inputTextureCoordinate.xy + blurStep;\n" +
                    "	}\n" +
                    "}\n";

    public static final String FRAGMENT_SHADER =
            "uniform sampler2D inputImageTexture;\n" +
                    "\n" +
                    "const lowp int GAUSSIAN_SAMPLES = 9;\n" +
                    "\n" +
                    "uniform lowp float aspectRatio;\n" +
                    "uniform highp vec2 blurCenter;\n" +
                    "uniform highp float radius;\n" +
                    "varying highp vec2 textureCoordinate;\n" +
                    "varying highp vec2 blurCoordinates[GAUSSIAN_SAMPLES];\n" +
                    "\n" +
                    "void main()\n" +
                    "{\n" +
                    "	lowp vec3 sum = vec3(0.0);\n" +

                    "   highp vec2 textureCoordinateToUse = vec2((textureCoordinate.x ), (textureCoordinate.y * aspectRatio + 0.5 - 0.5 * aspectRatio));" +
                    "   lowp vec4 fragColor=texture2D(inputImageTexture,textureCoordinate);\n" +
                    "   highp float dist = distance(blurCenter, textureCoordinateToUse);\n" +
                    "	\n" +
                    "	if (dist > radius)" +
                    "   {\n" +
                    "       sum += texture2D(inputImageTexture, blurCoordinates[0]).rgb * 0.05;\n" +
                    "       sum += texture2D(inputImageTexture, blurCoordinates[1]).rgb * 0.09;\n" +
                    "       sum += texture2D(inputImageTexture, blurCoordinates[2]).rgb * 0.12;\n" +
                    "       sum += texture2D(inputImageTexture, blurCoordinates[3]).rgb * 0.15;\n" +
                    "       sum += texture2D(inputImageTexture, blurCoordinates[4]).rgb * 0.18;\n" +
                    "       sum += texture2D(inputImageTexture, blurCoordinates[5]).rgb * 0.15;\n" +
                    "       sum += texture2D(inputImageTexture, blurCoordinates[6]).rgb * 0.12;\n" +
                    "       sum += texture2D(inputImageTexture, blurCoordinates[7]).rgb * 0.09;\n" +
                    "       sum += texture2D(inputImageTexture, blurCoordinates[8]).rgb * 0.05;\n" +
                            "\n" +
                    "	    //gl_FragColor = vec4(sum,fragColor.a);\n" +
                    "     vec4 blurredImageColor = vec4(sum,fragColor.a);\n" +
                    "     \n" +
                    "     gl_FragColor = mix(fragColor, blurredImageColor, smoothstep(radius , 0.5, dist));" +
                    "    }" +
                    "   else " +
                    "   {\n" +
                    "	    gl_FragColor = fragColor;\n" +
                    "    }" +
                    "}";

    protected float mBlurSize = 8.0f;
    protected float mRadius;
    private float mAspectRatio;
    private PointF mCenter;

    public GPUImageGaussianSelecterBlurFilter() {
        this(new PointF(0.5f, 0.5f),8f, 0.34f);
    }

    public GPUImageGaussianSelecterBlurFilter(PointF center, float blurSize, float radius) {
        super(VERTEX_SHADER, FRAGMENT_SHADER, VERTEX_SHADER, FRAGMENT_SHADER);
        mCenter = center;
        mBlurSize = blurSize;
        mRadius = radius;
    }

    @Override
    public void onInit() {
        super.onInit();
        setRadius(0.5f);
        setCenter(new PointF(0.5f, 0.5f));
    }

    @Override
    public void onInitialized() {
        super.onInitialized();
        setRadius(mRadius);
        setCenter(new PointF(0.5f, 0.5f));
    }

    @Override
    public void onOutputSizeChanged(int width, int height) {
        mAspectRatio = (float) height / width;
        updateUinformValue("aspectRatio", mAspectRatio);
        super.onOutputSizeChanged(width, height);
    }

    @Override
    public float getVerticalTexelOffsetRatio() {
        return mBlurSize;
    }

    @Override
    public float getHorizontalTexelOffsetRatio() {
        return mBlurSize;
    }

    /**
     * A multiplier for the blur size, ranging from 0.0 on up, with a default of 1.0
     *
     * @param blurSize from 0.0 on up, default 1.0
     */
    public void setBlurSize(float blurSize) {
        mBlurSize = blurSize;
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                initTexelOffsets();
            }
        });
    }

    public void setRadius(final float radius) {
        mRadius = radius;
        android.util.Log.e("jerrypxiao", "jerrypxiao  mRadius =" + mRadius);
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                updateUinformValue("radius", mRadius);
            }
        });
    }

    public void setCenter(PointF center) {
        mCenter = center;
        updateUinformValue("blurCenter", mCenter);
    }
}
