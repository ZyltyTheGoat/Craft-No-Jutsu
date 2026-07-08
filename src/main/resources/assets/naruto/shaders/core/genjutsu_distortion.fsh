#version 150

in vec4 vertexColor;
in vec2 screenPos;

uniform vec4 ColorModulator;
uniform float GenjutsuTime;
uniform float GenjutsuIntensity;
uniform vec2 GenjutsuScreenSize;

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
    vec4 base = vertexColor * ColorModulator;
    float intensity = clamp(GenjutsuIntensity, 0.0, 1.65);
    vec2 screenSize = max(GenjutsuScreenSize, vec2(1.0));
    vec2 uv = screenPos / screenSize;
    vec2 centered = uv - vec2(0.5);

    float waveA = sin((uv.x * 9.0 + uv.y * 4.5) + GenjutsuTime * 0.95);
    float waveB = sin((uv.y * 12.0 - uv.x * 5.0) - GenjutsuTime * 0.72);
    float drift = valueNoise(uv * 5.0 + vec2(GenjutsuTime * 0.05, -GenjutsuTime * 0.035));
    float fine = valueNoise(uv * 26.0 + vec2(-GenjutsuTime * 0.11, GenjutsuTime * 0.09));
    float pulse = 0.5 + 0.5 * sin(GenjutsuTime * 1.25 + drift * 4.0);

    float redSplit = smoothstep(0.08, 1.0, abs(waveA + drift - 0.45));
    float cyanSplit = smoothstep(0.18, 1.0, abs(waveB - drift + 0.2));
    float violetField = 0.55 + 0.45 * sin(length(centered) * 13.0 - GenjutsuTime * 0.8 + fine * 1.8);

    vec3 red = vec3(1.0, 0.02, 0.08) * redSplit;
    vec3 cyan = vec3(0.0, 0.95, 1.0) * cyanSplit;
    vec3 violet = vec3(0.58, 0.0, 0.95) * violetField;
    vec3 wash = mix(red, cyan, 0.45 + 0.25 * waveB) + violet * 0.35;
    wash *= 0.32 + pulse * 0.2 + fine * 0.18;

    float screenDarken = intensity * (0.045 + 0.025 * pulse);
    wash = mix(wash, vec3(0.0), clamp(screenDarken, 0.0, 0.12));

    float alpha = base.a * intensity * (0.12 + 0.1 * pulse + 0.06 * drift);
    alpha = clamp(alpha + screenDarken * 0.55, 0.0, 0.4);

    fragColor = vec4(clamp(wash, 0.0, 1.0), alpha);
}
