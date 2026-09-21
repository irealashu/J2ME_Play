precision mediump float;
uniform sampler2D sampler0;
uniform vec2 u_pixelDelta;
uniform vec2 u_texelDelta;
uniform vec4 u_setting;
varying vec2 v_texcoord0;

// Converts RGB color to perceived luminance
float getLuma(vec3 rgb) {
    return dot(rgb, vec3(0.299, 0.587, 0.114));
}

void main() {
    float smoothStrength = u_setting.x > 0.001 ? u_setting.x : 0.65;
    float edgeSensitivity = u_setting.y > 0.001 ? u_setting.y : 0.20;
    float blendSoftness = u_setting.z > 0.001 ? u_setting.z : 0.35;
    float clarity = u_setting.w > 0.001 ? u_setting.w : 1.05;

    vec2 texel = u_texelDelta;
    if (texel.x <= 0.0) texel = vec2(1.0 / 240.0, 1.0 / 320.0);

    // Multi-tap directional sampling for anti-aliasing edge reconstruction
    vec4 center = texture2D(sampler0, v_texcoord0);
    vec4 up = texture2D(sampler0, v_texcoord0 + vec2(0.0, -texel.y * 0.5));
    vec4 down = texture2D(sampler0, v_texcoord0 + vec2(0.0, texel.y * 0.5));
    vec4 left = texture2D(sampler0, v_texcoord0 + vec2(-texel.x * 0.5, 0.0));
    vec4 right = texture2D(sampler0, v_texcoord0 + vec2(texel.x * 0.5, 0.0));

    // Corner samples
    vec4 upLeft = texture2D(sampler0, v_texcoord0 + vec2(-texel.x * 0.5, -texel.y * 0.5));
    vec4 upRight = texture2D(sampler0, v_texcoord0 + vec2(texel.x * 0.5, -texel.y * 0.5));
    vec4 downLeft = texture2D(sampler0, v_texcoord0 + vec2(-texel.x * 0.5, texel.y * 0.5));
    vec4 downRight = texture2D(sampler0, v_texcoord0 + vec2(texel.x * 0.5, texel.y * 0.5));

    float lumaCenter = getLuma(center.rgb);
    float lumaUp = getLuma(up.rgb);
    float lumaDown = getLuma(down.rgb);
    float lumaLeft = getLuma(left.rgb);
    float lumaRight = getLuma(right.rgb);

    float lumaMin = min(lumaCenter, min(min(lumaUp, lumaDown), min(lumaLeft, lumaRight)));
    float lumaMax = max(lumaCenter, max(max(lumaUp, lumaDown), max(lumaLeft, lumaRight)));
    float lumaRange = lumaMax - lumaMin;

    // Edge-adaptive smoothing: smoothly interpolate moving edges without blurring flat surfaces
    if (lumaRange < edgeSensitivity) {
        gl_FragColor = center;
        return;
    }

    vec4 crossAvg = (up + down + left + right) * 0.25;
    vec4 diagAvg = (upLeft + upRight + downLeft + downRight) * 0.25;
    vec4 smoothColor = mix(crossAvg, diagAvg, blendSoftness);

    // Apply motion smoothing strength
    vec4 result = mix(center, smoothColor, smoothStrength);

    // Subtle adaptive sharpening filter to preserve sprite clarity
    result = mix(result, result * clarity, 0.5);

    gl_FragColor = vec4(clamp(result.rgb, 0.0, 1.0), center.a);
}
