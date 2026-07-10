#version 150

uniform float FalseDarknessTime;

in vec4 vertexColor;
in vec3 localPos;

out vec4 fragColor;

void main() {
    float forward = localPos.z;
    float side = length(localPos.xy);

    float pulseA = sin(forward * 7.5 - FalseDarknessTime * 15.0);
    float pulseB = sin(forward * 13.0 + localPos.x * 9.0 - FalseDarknessTime * 22.0);
    float pulseC = sin(forward * 5.5 + localPos.y * 11.0 + FalseDarknessTime * 18.0);

    float electric = smoothstep(0.25, 1.0, pulseA * 0.45 + pulseB * 0.35 + pulseC * 0.2);
    float whiteHot = smoothstep(0.72, 1.0, electric);

    vec3 deepBlue = vec3(0.25, 0.48, 1.0);
    vec3 paleLightning = vec3(0.68, 0.9, 1.0);
    vec3 white = vec3(1.0, 1.0, 1.0);

    vec3 color = mix(deepBlue, paleLightning, electric);
    color = mix(color, white, whiteHot * 0.82);

    float flicker = 0.82 + sin(FalseDarknessTime * 31.0 + forward * 2.0) * 0.12;
    float alpha = vertexColor.a * flicker;

    fragColor = vec4(color * vertexColor.rgb, alpha);
}