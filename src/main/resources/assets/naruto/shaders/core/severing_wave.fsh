#version 150

uniform float SeveringWaveTime;

in vec4 vertexColor;
in vec3 localPos;

out vec4 fragColor;

void main() {
    float flow = localPos.z * 2.4 - SeveringWaveTime * 7.5;
    float rippleA = sin(flow + localPos.x * 8.0) * 0.5 + 0.5;
    float rippleB = sin(flow * 1.7 + localPos.y * 10.0) * 0.5 + 0.5;

    float streak = smoothstep(0.18, 1.0, rippleA) * 0.45 + smoothstep(0.35, 1.0, rippleB) * 0.35;
    float whiteCore = smoothstep(0.72, 1.0, rippleA + rippleB * 0.45);

    vec3 waterBlue = vec3(0.22, 0.78, 1.0);
    vec3 paleBlue = vec3(0.72, 0.95, 1.0);
    vec3 white = vec3(1.0, 1.0, 1.0);

    vec3 color = mix(waterBlue, paleBlue, streak);
    color = mix(color, white, whiteCore * 0.55);

    float alphaPulse = 0.82 + sin(SeveringWaveTime * 5.0 + localPos.z * 1.5) * 0.08;
    float alpha = vertexColor.a * alphaPulse;

    fragColor = vec4(color * vertexColor.rgb, alpha);
}