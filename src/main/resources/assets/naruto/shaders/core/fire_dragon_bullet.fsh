#version 150

uniform float FireDragonTime;

in vec4 vertexColor;
in vec3 localPos;

out vec4 fragColor;

void main() {
    float forward = localPos.z;
    float radial = length(localPos.xy);

    float flameA = sin(forward * 6.5 - FireDragonTime * 18.0 + radial * 2.8);
    float flameB = sin(forward * 13.0 + localPos.x * 8.0 - FireDragonTime * 26.0);
    float flameC = sin(forward * 4.0 + localPos.y * 11.0 + FireDragonTime * 15.0);
    float heat = smoothstep(-0.25, 1.0, flameA * 0.45 + flameB * 0.35 + flameC * 0.2);
    float core = smoothstep(0.66, 1.0, heat);

    vec3 ember = vec3(1.0, 0.18, 0.02);
    vec3 orange = vec3(1.0, 0.48, 0.05);
    vec3 whiteHot = vec3(1.0, 0.94, 0.62);
    vec3 color = mix(ember, orange, heat);
    color = mix(color, whiteHot, core * 0.74);

    float flicker = 0.82 + sin(FireDragonTime * 33.0 + forward * 2.0) * 0.13;
    float alpha = vertexColor.a * flicker * (0.82 + heat * 0.28);

    fragColor = vec4(color * vertexColor.rgb, alpha);
}
