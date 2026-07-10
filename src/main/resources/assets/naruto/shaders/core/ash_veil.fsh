#version 150

in vec4 vertexColor;
in vec2 screenPos;

uniform vec4 ColorModulator;
uniform float AshTime;
uniform float AshStrength;
uniform vec2 AshScreenSize;

out vec4 fragColor;

float hash(vec2 point) {
    return fract(sin(dot(point, vec2(91.7, 213.3))) * 43758.5453);
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
    float n = valueNoise(uv * 3.5 + vec2(AshTime * 0.028, -AshTime * 0.018));
    n += valueNoise(uv * 9.0 + vec2(-AshTime * 0.046, AshTime * 0.025)) * 0.55;
    n += valueNoise(uv * 24.0 + vec2(AshTime * 0.07, AshTime * 0.05)) * 0.2;
    return n / 1.75;
}

void main() {
    vec4 base = vertexColor * ColorModulator;
    float strength = clamp(AshStrength, 0.0, 1.0);
    vec2 screenSize = max(AshScreenSize, vec2(1.0));
    vec2 uv = screenPos / screenSize;
    vec2 centered = uv - vec2(0.5);

    float ash = layeredNoise(uv + vec2(sin(AshTime * 0.03) * 0.035, cos(AshTime * 0.022) * 0.025));
    float rolling = 0.5 + 0.5 * sin(uv.y * 14.0 + uv.x * 4.0 - AshTime * 0.42 + ash * 2.6);
    float lowSmoke = smoothstep(0.24, 1.0, uv.y);
    float edgeDark = smoothstep(0.16, 0.84, length(centered));
    float nearAsh = smoothstep(1.0, 0.08, distance(uv, vec2(0.5, 0.78)));
    float ember = smoothstep(0.965, 1.0, hash(floor((uv + vec2(AshTime * 0.006, -AshTime * 0.018)) * vec2(90.0, 52.0))));

    float veil = clamp(ash * 0.55 + rolling * 0.16 + lowSmoke * 0.28 + edgeDark * 0.18 + nearAsh * 0.5, 0.0, 1.0);

    vec3 charcoal = vec3(0.13, 0.12, 0.11);
    vec3 warmGrey = vec3(0.42, 0.36, 0.30);
    vec3 paleAsh = vec3(0.72, 0.66, 0.56);
    vec3 emberColor = vec3(1.0, 0.28, 0.08);
    vec3 color = mix(charcoal, warmGrey, veil);
    color = mix(color, paleAsh, ash * 0.28);
    color = mix(color, emberColor, ember * (0.18 + nearAsh * 0.25));

    float density = pow(veil, 0.72);
    float alpha = base.a * strength * (0.24 + density * 0.54 + nearAsh * 0.36 + lowSmoke * 0.16);
    fragColor = vec4(clamp(color, 0.0, 1.0), clamp(alpha, 0.0, 0.9));
}
