precision mediump float;
uniform sampler2D sampler0;
uniform vec2 u_pixelDelta;
uniform vec2 u_texelDelta;
uniform vec4 u_setting;
varying vec2 v_texcoord0;

void main() {
    float satBoost = u_setting.x > 0.001 ? u_setting.x : 1.35;
    float contrast = u_setting.y > 0.001 ? u_setting.y : 1.15;
    float blackDepth = u_setting.z >= 0.0 ? u_setting.z : 0.04;
    float warmth = u_setting.w > 0.001 ? u_setting.w : 1.05;

    vec4 color = texture2D(sampler0, v_texcoord0);

    // Deep black level offset for AMOLED
    vec3 c = max(vec3(0.0), color.rgb - vec3(blackDepth));
    c /= (1.0 - blackDepth);

    // Contrast curve
    c = (c - 0.5) * contrast + 0.5;

    // Vibrancy / Saturation boost
    float luma = dot(c, vec3(0.299, 0.587, 0.114));
    c = mix(vec3(luma), c, satBoost);

    // Subtle warm yellow-gold tint for retro nostalgia
    c.r *= warmth;
    c.g *= (1.0 + (warmth - 1.0) * 0.5);

    gl_FragColor = vec4(clamp(c, 0.0, 1.0), color.a);
}
