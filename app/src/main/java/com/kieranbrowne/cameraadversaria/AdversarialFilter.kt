package com.kieranbrowne.cameraadversaria

import jp.co.cyberagent.android.gpuimage.filter.GPUImageAddBlendFilter.ADD_BLEND_FRAGMENT_SHADER
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import android.opengl.GLES20;
import android.util.Log


private const val ADVERSARIAL_SHADER = "#define PI 3.1415\n" +
        "varying highp vec2 textureCoordinate;\n" +
        "\n" +
        "uniform sampler2D inputImageTexture;\n" +
        "uniform lowp float amp;\n" +
        "\n" +
        "//\tClassic Perlin 2D Noise \n" +
        "//\tby Stefan Gustavson\n" +
        "\n"+
        "vec4 permute(vec4 x){return mod(((x*34.0)+1.0)*x, 289.0);}\n"+
        "vec4 taylorInvSqrt(vec4 r){return 1.79284291400159 - 0.85373472095314 * r;}\n"+
        "vec4 fade(vec4 t) {return t*t*t*(t*(t*6.0-15.0)+10.0);}\n"+
        "vec2 fade(vec2 t) {return t*t*t*(t*(t*6.0-15.0)+10.0);}\n"+
        "\n" +
        "float cnoise(vec2 P){\n"+
        "vec4 Pi = floor(P.xyxy) + vec4(0.0, 0.0, 1.0, 1.0);\n"+
        "vec4 Pf = fract(P.xyxy) - vec4(0.0, 0.0, 1.0, 1.0);\n"+
        "Pi = mod(Pi, 289.0);\n"+
        "vec4 ix = Pi.xzxz;\n"+
        "vec4 iy = Pi.yyww;\n"+
        "vec4 fx = Pf.xzxz;\n"+
        "vec4 fy = Pf.yyww;\n"+
        "vec4 i = permute(permute(ix) + iy);\n"+
        "vec4 gx = 2.0 * fract(i * 0.0243902439) - 1.0;\n"+
        "vec4 gy = abs(gx) - 0.5;\n"+
        "vec4 tx = floor(gx + 0.5);\n"+
        "gx = gx - tx;\n"+
        "vec2 g00 = vec2(gx.x,gy.x);\n"+
        "vec2 g10 = vec2(gx.y,gy.y);\n"+
        "vec2 g01 = vec2(gx.z,gy.z);\n"+
        "vec2 g11 = vec2(gx.w,gy.w);\n"+
        "vec4 norm = 1.79284291400159 - 0.85373472095314 *\n"+
        "vec4(dot(g00, g00), dot(g01, g01), dot(g10, g10), dot(g11, g11));\n"+
        "g00 *= norm.x;\n"+
        "g01 *= norm.y;\n"+
        "g10 *= norm.z;\n"+
        "g11 *= norm.w;\n"+
        "float n00 = dot(g00, vec2(fx.x, fy.x));\n"+
        "float n10 = dot(g10, vec2(fx.y, fy.y));\n"+
        "float n01 = dot(g01, vec2(fx.z, fy.z));\n"+
        "float n11 = dot(g11, vec2(fx.w, fy.w));\n"+
        "vec2 fade_xy = fade(Pf.xy);\n"+
        "vec2 n_x = mix(vec2(n00, n01), vec2(n10, n11), fade_xy.x);\n"+
        "float n_xy = mix(n_x.x, n_x.y, fade_xy.y);\n"+
        "return 2.3 * n_xy;\n"+
        "}\n"+
        "void main()\n" +
        "{\n" +
        "   highp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
        "   textureColor.g += sin(cnoise(textureCoordinate*20. + 3050.)*30. + 50.)*amp;\n" +
        "   textureColor.r += sin(cnoise(textureCoordinate*20. + 0000.)*47. - 00.)*amp;\n" +
        "   textureColor.b += sin(cnoise(textureCoordinate*20. - 8250.)*53. - 00.)*amp;\n" +
        "   textureColor.g += sin(cnoise(textureCoordinate*10. + 3050.)*30. + 50.)*amp;\n" +
        "   textureColor.r += sin(cnoise(textureCoordinate*10. + 0000.)*47. - 00.)*amp;\n" +
        "   textureColor.b += sin(cnoise(textureCoordinate*10. - 8250.)*53. - 00.)*amp;\n" +
        "   \n" +
        "   gl_FragColor = textureColor;\n" +
        "}";

class AdversarialFilter(amp: Double) : GPUImageFilter(NO_FILTER_VERTEX_SHADER, ADVERSARIAL_SHADER)  {

    private var amp = amp
    private var ampLocation: Int? = null




    override fun onInit() {
        super.onInit()
        ampLocation = GLES20.glGetUniformLocation(program, "amp")
        Log.d("LOC",ampLocation.toString())
    }


    override fun onInitialized() {
        super.onInitialized()
        setAmp(amp)
    }


    fun setAmp(amp: Double) {
        this.amp = amp
        ampLocation?.let {
            setFloat(it, this.amp.toFloat())
        }
    }
}