#version 150

in vec4 vertexColor;
in vec2 screenPos;

uniform vec4 ColorModulator;
uniform float MistTime;
uniform float MistStrength;
uniform vec2 MistScreenSize;

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

float layeredNoise(vec2 uv) {
    float n = valueNoise(uv * 3.0 + vec2(MistTime * 0.015, -MistTime * 0.012));
    n += valueNoise(uv * 7.0 + vec2(-MistTime * 0.028, MistTime * 0.018)) * 0.55;
    n += valueNoise(uv * 15.0 + vec2(MistTime * 0.04, MistTime * 0.021)) * 0.25;
    return n / 1.8;
}

void main() {
    vec4 base = vertexColor * ColorModulator;
    float strength = clamp(MistStrength, 0.0, 1.0);
    vec2 screenSize = max(MistScreenSize, vec2(1.0));
    vec2 uv = screenPos / screenSize;
    vec2 centered = uv - vec2(0.5);

    float fog = layeredNoise(uv + vec2(sin(MistTime * 0.035) * 0.04, cos(MistTime * 0.025) * 0.03));
    float rollingBand = 0.5 + 0.5 * sin(uv.y * 12.0 + uv.x * 3.0 - MistTime * 0.32 + fog * 2.4);
    float crossBand = 0.5 + 0.5 * sin(uv.x * 8.0 - MistTime * 0.18 + fog * 1.8);
    float edgeCloud = smoothstep(0.1, 0.82, length(centered));
    float veil = clamp(fog * 0.62 + rollingBand * 0.25 + crossBand * 0.13 + edgeCloud * 0.16, 0.0, 1.0);

    vec3 paleBlue = vec3(0.58, 0.82, 1.0);
    vec3 pearl = vec3(0.93, 0.98, 1.0);
    vec3 greyMist = vec3(0.67, 0.78, 0.86);
    vec3 color = mix(greyMist, mix(paleBlue, pearl, veil), 0.68 + rollingBand * 0.18);
    color *= 0.78 + veil * 0.26;

    float alpha = base.a * strength * (0.16 + veil * 0.34 + edgeCloud * 0.12);
    fragColor = vec4(clamp(color, 0.0, 1.0), clamp(alpha, 0.0, 0.62));
}
