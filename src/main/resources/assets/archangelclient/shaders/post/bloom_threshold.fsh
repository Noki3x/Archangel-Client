#version 330

// Keeps only the bright parts of the image, with a soft knee so the cut-off doesn't band.
uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform BloomThresholdConfig {
    float Threshold;
    float Knee;
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec3 color = texture(InSampler, texCoord).rgb;
    float brightness = max(color.r, max(color.g, color.b));
    float soft = clamp(brightness - Threshold + Knee, 0.0, 2.0 * Knee);
    soft = soft * soft / (4.0 * Knee + 1e-4);
    float contribution = max(soft, brightness - Threshold) / max(brightness, 1e-4);
    fragColor = vec4(color * contribution, 1.0);
}
