precision mediump float;
uniform sampler2D sampler0;
uniform vec2 u_pixelDelta;
uniform vec2 u_texelDelta;
uniform vec4 u_setting;
varying vec2 v_texcoord0;

void main() {
    float mode = u_setting.x;
    float gridInt = u_setting.y >= 0.0 ? u_setting.y : 0.40;
    float contrast = u_setting.z > 0.001 ? u_setting.z : 1.2;
    float brightness = u_setting.w > 0.001 ? u_setting.w : 1.05;

    vec2 uv = v_texcoord0;
    vec4 color = texture2D(sampler0, uv);

    // Convert to grayscale luminance
    float lum = dot(color.rgb, vec3(0.299, 0.587, 0.114));
    lum = clamp((lum - 0.5) * contrast + 0.5, 0.0, 1.0);

    // Pixel matrix grid
    vec2 delta = max(u_texelDelta, vec2(0.0001));
    vec2 cellUV = fract(uv / delta);
    vec2 gridDist = 0.5 - abs(cellUV - 0.5);
    float gridFactor = clamp(min(gridDist.x, gridDist.y) * 8.0, 1.0 - gridInt, 1.0);

    vec3 tintColor;
    if (mode < 0.5) {
        // Nokia 3310 Classic Green LCD
        vec3 bg = vec3(0.55, 0.68, 0.32);
        vec3 fg = vec3(0.12, 0.18, 0.09);
        tintColor = mix(fg, bg, lum);
    } else if (mode < 1.5) {
        // Amber Phosphor CRT
        vec3 bg = vec3(0.10, 0.05, 0.01);
        vec3 fg = vec3(1.0, 0.65, 0.05);
        tintColor = mix(bg, fg, lum);
    } else {
        // GameBoy Classic Olive LCD
        vec3 bg = vec3(0.61, 0.73, 0.06);
        vec3 fg = vec3(0.06, 0.22, 0.06);
        tintColor = mix(fg, bg, lum);
    }

    gl_FragColor = vec4(tintColor * gridFactor * brightness, color.a);
}
