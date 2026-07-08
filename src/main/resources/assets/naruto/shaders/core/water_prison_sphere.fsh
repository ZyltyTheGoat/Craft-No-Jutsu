#version 150

in vec4 vertexColor;
in vec3 spherePos;

uniform vec4 ColorModulator;
uniform float WaterPrisonTime;

out vec4 fragColor;

float wave(vec3 p, float scale, float speed) {
    return sin((p.x * 2.3 + p.y * 3.7 + p.z * 1.9) * scale + WaterPrisonTime * speed);
}

void main() {
    vec4 base = vertexColor * ColorModulator;
    vec3 n = normalize(spherePos);
    float lon = atan(n.z, n.x);
    float equatorMask = 1.0 - smoothstep(0.12, 0.58, abs(n.y));

    float bandA = wave(spherePos, 1.45, 3.2);
    float bandB = wave(spherePos.zyx, 2.15, -2.35);
    float bandC = sin(length(spherePos.xz) * 9.5 - spherePos.y * 3.6 + WaterPrisonTime * 4.1);
    float ripple = bandA * 0.42 + bandB * 0.28 + bandC * 0.46;

    float spiralA = 1.0 - smoothstep(0.045, 0.15, abs(sin(lon + n.y * 7.8 - WaterPrisonTime * 1.9)));
    float spiralB = 1.0 - smoothstep(0.045, 0.15, abs(sin(lon - n.y * 7.8 + WaterPrisonTime * 1.55 + 3.14159)));
    float spiral = max(spiralA, spiralB) * equatorMask;

    float shimmer = smoothstep(0.36, 0.86, ripple);
    float deep = 0.5 + 0.5 * sin(WaterPrisonTime * 2.15 + spherePos.y * 6.0);
    vec3 water = mix(vec3(0.0, 0.07, 0.23), vec3(0.0, 0.33, 0.86), 0.42 + ripple * 0.24);
    vec3 highlight = vec3(0.55, 0.9, 1.0) * shimmer;
    vec3 foam = vec3(0.86, 0.97, 1.0) * spiral;
    vec3 color = water * (0.74 + deep * 0.2) + highlight * 0.55 + foam * 0.82;

    float alpha = base.a * (0.28 + shimmer * 0.24 + abs(ripple) * 0.12 + spiral * 0.22);
    fragColor = vec4(clamp(color, 0.0, 1.0), clamp(alpha, 0.16, 0.58));
}
