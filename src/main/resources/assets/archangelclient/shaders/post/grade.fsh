#version 330

// Color grading (saturation, contrast, tint), vignette and chromatic aberration in one pass.
uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform GradeConfig {
    float Saturation;
    float Contrast;
    float Vignette;
    float Chromatic;
    vec4 Tint; // rgb color, a = strength
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 fromCenter = texCoord - 0.5;
    vec3 color;
    if (Chromatic > 0.0) {
        // split channels outward from the center, stronger toward the edges
        vec2 offset = fromCenter * dot(fromCenter, fromCenter) * Chromatic * 0.06;
        color = vec3(texture(InSampler, texCoord + offset).r,
                     texture(InSampler, texCoord).g,
                     texture(InSampler, texCoord - offset).b);
    } else {
        color = texture(InSampler, texCoord).rgb;
    }

    float luma = dot(color, vec3(0.2126, 0.7152, 0.0722));
    color = mix(vec3(luma), color, Saturation);
    color = (color - 0.5) * Contrast + 0.5;
    color = mix(color, color * Tint.rgb * 1.6, Tint.a);

    // aspect-correct, smooth vignette
    vec2 v = fromCenter * vec2(OutSize.x / OutSize.y, 1.0);
    float vignette = smoothstep(0.35, 1.05, length(v) * 1.15);
    color *= 1.0 - vignette * Vignette;

    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
