#version 150

in vec4 vertexColor;
in vec2 screenPos;

uniform vec4 ColorModulator;
uniform float WheelTime;
uniform vec2 WheelScreenSize;

out vec4 fragColor;

float hash(vec2 point) {
    return fract(sin(dot(point, vec2(127.1, 311.7))) * 43758.5453);
}

float valueNoise(vec2 point) {
    vec2 cell = floor(point);
    vec2 local = fract(point);
    vec2 smoothLocal = local * local * (3.0 - 2.0 * local);

    float a = hash(cell);
    float b = hash(cell + vec2(1.0, 0.0));
    float c = hash(cell + vec2(0.0, 1.0));
    float d = hash(cell + vec2(1.0, 1.0));

    return mix(mix(a, b, smoothLocal.x), mix(c, d, smoothLocal.x), smoothLocal.y);
}

void main() {
    vec4 color = vertexColor * ColorModulator;
    if (color.a == 0.0) {
        discard;
    }

    vec2 uv = screenPos / max(WheelScreenSize, vec2(1.0));
    vec2 drift = vec2(WheelTime * 0.018, -WheelTime * 0.012);

    float shimmer = valueNoise(uv * 42.0 + drift);
    float veins = sin((uv.x * 18.0 + uv.y * 31.0) + valueNoise(uv * 18.0 - drift) * 5.0 + WheelTime * 0.55);
    float veinMask = smoothstep(0.82, 0.98, veins);
    float grain = valueNoise(uv * 95.0 + drift * 1.7);

    float highlight = veinMask * 0.055 + shimmer * 0.025;
    float shadow = (1.0 - grain) * 0.018;
    vec3 textured = color.rgb + highlight - shadow;

    fragColor = vec4(clamp(textured, 0.0, 1.0), color.a);
}
