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
    vec2 centered = uv - vec2(0.5);
    centered.x *= WheelScreenSize.x / max(WheelScreenSize.y, 1.0);

    float distanceFromCenter = length(centered);
    float angle = atan(centered.y, centered.x);
    float slowTime = WheelTime * 0.55;

    float ripple = sin(distanceFromCenter * 92.0 - slowTime * 1.4);
    float rippleMask = smoothstep(0.78, 0.97, ripple) * 0.055;

    float spiral = sin(angle * 5.0 + distanceFromCenter * 44.0 - slowTime);
    float veinNoise = valueNoise(vec2(angle * 2.2, distanceFromCenter * 28.0) + vec2(slowTime * 0.08, -slowTime * 0.05));
    float veinMask = smoothstep(0.72, 0.96, spiral + veinNoise * 0.55) * 0.06;

    float irisShadow = smoothstep(0.06, 0.42, distanceFromCenter) * (1.0 - smoothstep(0.46, 0.72, distanceFromCenter));
    float pulse = (sin(slowTime + distanceFromCenter * 18.0) + 1.0) * 0.5;
    float darkLine = smoothstep(0.88, 0.99, sin(distanceFromCenter * 64.0 + angle * 3.0 + slowTime * 0.65)) * 0.035;

    vec3 eyeTint = mix(vec3(0.55, 0.08, 0.16), vec3(0.42, 0.12, 0.72), pulse * 0.35);
    vec3 textured = color.rgb;
    textured = mix(textured, eyeTint, (rippleMask + veinMask) * 0.55);
    textured += rippleMask + veinMask;
    textured -= darkLine * irisShadow;

    fragColor = vec4(clamp(textured, 0.0, 1.0), color.a);
}
