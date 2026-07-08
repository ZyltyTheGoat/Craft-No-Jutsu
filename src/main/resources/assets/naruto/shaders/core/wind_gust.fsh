#version 150

in vec4 vertexColor;
in vec3 localPos;

uniform vec4 ColorModulator;
uniform float WindTime;

out vec4 fragColor;

float hash(float value) {
    return fract(sin(value * 21.173) * 43758.5453);
}

void main() {
    vec4 base = vertexColor * ColorModulator;
    float stream = sin(localPos.z * 5.7 + localPos.x * 2.6 - WindTime * 7.4);
    float cross = sin(localPos.y * 8.3 - localPos.z * 2.1 + WindTime * 4.2);
    float streak = smoothstep(0.28, 1.0, abs(stream * 0.72 + cross * 0.28));
    float mist = 0.55 + 0.45 * sin(length(localPos.xy) * 8.0 - localPos.z * 3.0 + WindTime * 5.0);
    vec3 pale = vec3(0.78, 1.0, 0.92);
    vec3 white = vec3(0.96, 1.0, 0.98);
    vec3 color = mix(pale, white, streak) * (0.74 + mist * 0.26);
    float alpha = base.a * (0.35 + streak * 0.45 + mist * 0.12);
    fragColor = vec4(clamp(color, 0.0, 1.0), clamp(alpha, 0.0, 0.62));
}
