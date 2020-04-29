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
        "uniform lowp float seed;\n" +
        "\n" +
        "//Classic Perlin 2D Noise\n" +
        "//by Stefan Gustavson\n" +
        "\n"+
        "vec4 permute(vec4 x){return mod(((x*34.0)+1.0)*x, 289.0);}\n"+
        "vec4 taylorInvSqrt(vec4 r){return 1.79284291400159 - 0.85373472095314 * r;}\n"+
        "vec3 fade(vec3 t) {return t*t*t*(t*(t*6.0-15.0)+10.0);}\n"+
        "\n" +
        "float cnoise(vec3 P){\n" +
        "vec3 Pi0 = floor(P); // Integer part for indexing\n" +
        "vec3 Pi1 = Pi0 + vec3(1.0); // Integer part + 1\n" +
        "Pi0 = mod(Pi0, 289.0);\n" +
        "Pi1 = mod(Pi1, 289.0);\n" +
        "vec3 Pf0 = fract(P); // Fractional part for interpolation\n" +
        "vec3 Pf1 = Pf0 - vec3(1.0); // Fractional part - 1.0\n" +
        "vec4 ix = vec4(Pi0.x, Pi1.x, Pi0.x, Pi1.x);\n" +
        "vec4 iy = vec4(Pi0.yy, Pi1.yy);\n" +
        "vec4 iz0 = Pi0.zzzz;\n" +
        "vec4 iz1 = Pi1.zzzz;\n" +
        "vec4 ixy = permute(permute(ix) + iy);\n" +
        "vec4 ixy0 = permute(ixy + iz0);\n" +
        "vec4 ixy1 = permute(ixy + iz1);\n" +
        "vec4 gx0 = ixy0 / 7.0;\n" +
        "vec4 gy0 = fract(floor(gx0) / 7.0) - 0.5;\n" +
        "gx0 = fract(gx0);\n" +
        "vec4 gz0 = vec4(0.5) - abs(gx0) - abs(gy0);\n" +
        "vec4 sz0 = step(gz0, vec4(0.0));\n" +
        "gx0 -= sz0 * (step(0.0, gx0) - 0.5);\n" +
        "gy0 -= sz0 * (step(0.0, gy0) - 0.5);\n" +
        "vec4 gx1 = ixy1 / 7.0;\n" +
        "vec4 gy1 = fract(floor(gx1) / 7.0) - 0.5;\n" +
        "gx1 = fract(gx1);\n" +
        "vec4 gz1 = vec4(0.5) - abs(gx1) - abs(gy1);\n" +
        "vec4 sz1 = step(gz1, vec4(0.0));\n" +
        "gx1 -= sz1 * (step(0.0, gx1) - 0.5);\n" +
        "gy1 -= sz1 * (step(0.0, gy1) - 0.5);\n" +
        "vec3 g000 = vec3(gx0.x,gy0.x,gz0.x);\n" +
        "vec3 g100 = vec3(gx0.y,gy0.y,gz0.y);\n" +
        "vec3 g010 = vec3(gx0.z,gy0.z,gz0.z);\n" +
        "vec3 g110 = vec3(gx0.w,gy0.w,gz0.w);\n" +
        "vec3 g001 = vec3(gx1.x,gy1.x,gz1.x);\n" +
        "vec3 g101 = vec3(gx1.y,gy1.y,gz1.y);\n" +
        "vec3 g011 = vec3(gx1.z,gy1.z,gz1.z);\n" +
        "vec3 g111 = vec3(gx1.w,gy1.w,gz1.w);\n" +
        "vec4 norm0 = taylorInvSqrt(vec4(dot(g000, g000), dot(g010, g010), dot(g100, g100), dot(g110, g110)));\n" +
        "g000 *= norm0.x;\n" +
        "g010 *= norm0.y;\n" +
        "g100 *= norm0.z;\n" +
        "g110 *= norm0.w;\n" +
        "vec4 norm1 = taylorInvSqrt(vec4(dot(g001, g001), dot(g011, g011), dot(g101, g101), dot(g111, g111)));\n" +
        "g001 *= norm1.x;\n" +
        "g011 *= norm1.y;\n" +
        "g101 *= norm1.z;\n" +
        "g111 *= norm1.w;\n" +
        "float n000 = dot(g000, Pf0);\n" +
        "float n100 = dot(g100, vec3(Pf1.x, Pf0.yz));\n" +
        "float n010 = dot(g010, vec3(Pf0.x, Pf1.y, Pf0.z));\n" +
        "float n110 = dot(g110, vec3(Pf1.xy, Pf0.z));\n" +
        "float n001 = dot(g001, vec3(Pf0.xy, Pf1.z));\n" +
        "float n101 = dot(g101, vec3(Pf1.x, Pf0.y, Pf1.z));\n" +
        "float n011 = dot(g011, vec3(Pf0.x, Pf1.yz));\n" +
        "float n111 = dot(g111, Pf1);\n" +
        "vec3 fade_xyz = fade(Pf0);\n" +
        "vec4 n_z = mix(vec4(n000, n100, n010, n110), vec4(n001, n101, n011, n111), fade_xyz.z);\n" +
        "vec2 n_yz = mix(n_z.xy, n_z.zw, fade_xyz.y);\n" +
        "float n_xyz = mix(n_yz.x, n_yz.y, fade_xyz.x);\n" +
        "return 2.2 * n_xyz;\n" +
        "}\n" +
        "vec3 filter(vec3 coord, float scale, float s2, float x2) {\n"+
        "   vec3 thing = vec3(0.);\n"+
        "   thing.r += sin(cnoise((cnoise(coord*s2)*x2-vec3(.5,.5,0.))*scale)*5.)*amp;\n" +
        "   thing.g += sin(cnoise((cnoise(coord*s2)*x2-vec3(.5,.5,0.))*scale)*5.)*amp;\n" +
        "   thing.b += sin(cnoise((cnoise(coord*s2)*x2-vec3(.5,.5,0.))*scale)*5.)*amp;\n" +
        //"   img.b += sin(cnoise(coord*scale)*50. - 00.)*amp;\n" +
        "   return thing;\n"+
        "}\n"+
        "void main()\n" +
        "{\n" +
        "   highp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
        "   textureColor.rgb += filter(vec3(textureCoordinate.x,textureCoordinate.y,seed*9.), 10., 2.1, 70.);\n"+
        "   textureColor.rgb += filter(vec3(textureCoordinate.x,textureCoordinate.y,seed*9.), 20., 1.1, 100.);\n"+
        "   textureColor.rgb += filter(vec3(textureCoordinate.x,textureCoordinate.y,seed*9.), 50., 1.1, 30.);\n"+
        "   \n" +
        "   gl_FragColor = textureColor;\n" +
        "}";

class AdversarialFilter(amp: Double, seed: Double) : GPUImageFilter(NO_FILTER_VERTEX_SHADER, ADVERSARIAL_SHADER)  {

    private var amp = amp
    private var seed = seed
    private var ampLocation: Int? = null
    private var seedLocation: Int? = null



    override fun onInit() {
        super.onInit()
        ampLocation = GLES20.glGetUniformLocation(program, "amp")
        seedLocation = GLES20.glGetUniformLocation(program, "seed")
        Log.d("LOC",ampLocation.toString())
    }


    override fun onInitialized() {
        super.onInitialized()
        setAmp(amp)
        setSeed(seed)
    }


    fun setAmp(amp: Double) {
        this.amp = amp
        ampLocation?.let {
            setFloat(it, this.amp.toFloat())
        }
    }

    fun setSeed(seed: Double) {
        this.seed = seed
        seedLocation?.let {
            setFloat(it, this.seed.toFloat())
        }
    }
}
