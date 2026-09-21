precision mediump float;
uniform sampler2D sampler0;
uniform vec2 u_pixelDelta;
uniform vec2 u_texelDelta;
uniform vec4 u_setting;
varying vec2 v_texcoord0;

vec2 curve(vec2 uv, float bend) {
    if (bend <= 0.001) return uv;
    vec2 st = (uv - 0.5) * 2.0;
    st *= 1.0 + pow(length(st) * bend, 2.0);
    return st * 0.5 + 0.5;
}

void main() {
    float scanlineInt = u_setting.x > 0.001 ? u_setting.x : 0.45;
    float curvature = u_setting.y >= 0.0 ? u_setting.y : 0.04;
    float maskInt = u_setting.z >= 0.0 ? u_setting.z : 0.35;
    float brightness = u_setting.w > 0.001 ? u_setting.w : 1.15;

    vec2 uv = curve(v_texcoord0, curvature);
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) {
        gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    }

    vec4 color = texture2D(sampler0, uv);

    // Sine scanlines based on vertical pixel coordinate
    float pixelY = uv.y / max(u_pixelDelta.y, 0.0001);
    float scanline = sin(pixelY * 3.14159265);
    float scanFactor = 1.0 - scanlineInt * (0.5 + 0.5 * scanline * scanline);

    // Aperture grille RGB phosphor mask
    float pixelX = uv.x / max(u_pixelDelta.x, 0.0001);
    float modX = mod(floor(pixelX), 3.0);
    vec3 mask = vec3(1.0);
    if (modX < 1.0) {
        mask = vec3(1.0, 1.0 - maskInt, 1.0 - maskInt);
    } else if (modX < 2.0) {
        mask = vec3(1.0 - maskInt, 1.0, 1.0 - maskInt);
    } else {
        mask = vec3(1.0 - maskInt, 1.0 - maskInt, 1.0);
    }

    // Corner vignette
    vec2 vigUV = (uv - 0.5) * 2.0;
    float vig = clamp(1.0 - dot(vigUV, vigUV) * 0.15, 0.0, 1.0);

    vec3 finalRgb = color.rgb * scanFactor * mask * brightness * vig;
    gl_FragColor = vec4(finalRgb, color.a);
}
