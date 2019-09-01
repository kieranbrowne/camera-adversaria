package com.kieranbrowne.cameraadversaria

import jp.co.cyberagent.android.gpuimage.filter.GPUImageAddBlendFilter.ADD_BLEND_FRAGMENT_SHADER
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter


private const val ADVERSARIAL_SHADER = "" +
        "varying highp vec2 textureCoordinate;\n" +
        "\n" +
        "uniform sampler2D inputImageTexture;\n" +
        "\n" +
        "void main()\n" +
        "{\n" +
        "   highp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
        "   textureColor.g = 0.;\n" +
        "   \n" +
        "   gl_FragColor = textureColor;\n" +
        "}";

class AdversarialFilter : GPUImageFilter(NO_FILTER_VERTEX_SHADER, ADVERSARIAL_SHADER)