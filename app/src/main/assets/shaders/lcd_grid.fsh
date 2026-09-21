precision mediump float;
uniform sampler2D sampler0;
uniform vec2 u_pixelDelta;
uniform vec2 u_texelDelta;
uniform vec4 u_setting;
varying vec2 v_texcoord0;

void main() {
    float gridDarkness = u_setting.x > 0.001 ? u_setting.x : 0.40;
    float gridSharpness = u_setting.y > 0.001 ? u_setting.y : 3.0;
    float subpixelInt = u_setting.z >= 0.0 ? u_setting.z : 0.30;
    float contrast = u_setting.w > 0.001 ? u_setting.w : 1.10;

    vec2 uv = v_texcoord0;
    vec4 color = texture2D(sampler0, uv);

    // Coordinates inside native texture pixel
    vec2 delta = max(u_texelDelta, vec2(0.0001));
    vec2 cellUV = fract(uv / delta);

    // Grid border calculation
    vec2 gridDist = 0.5 - abs(cellUV - 0.5);
    float factorX = clamp(gridDist.x * gridSharpness * 4.0, 0.0, 1.0);
    float factorY = clamp(gridDist.y * gridSharpness * 4.0, 0.0, 1.0);
    float gridFactor = mix(1.0 - gridDarkness, 1.0, factorX * factorY);

    // LCD RGB subpixel stripe
    float subCol = cellUV.x * 3.0;
    vec3 subMask = vec3(1.0);
    if (subCol < 1.0) {
        subMask = vec3(1.0 + subpixelInt * 0.4, 1.0 - subpixelInt * 0.4, 1.0 - subpixelInt * 0.4);
    } else if (subCol < 2.0) {
        subMask = vec3(1.0 - subpixelInt * 0.4, 1.0 + subpixelInt * 0.4, 1.0 - subpixelInt * 0.4);
    } else {
        subMask = vec3(1.0 - subpixelInt * 0.4, 1.0 - subpixelInt * 0.4, 1.0 + subpixelInt * 0.4);
    }

    vec3 rgb = pow(color.rgb, vec3(1.0 / contrast)) * gridFactor * subMask;
    gl_FragColor = vec4(rgb, color.a);
}
