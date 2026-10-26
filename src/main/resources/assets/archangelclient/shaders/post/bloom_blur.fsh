#version 330

// One direction of a separable Gaussian blur; run once horizontally and once vertically.
uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform BloomBlurConfig {
    vec2 Direction;
    float Radius;
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    // 9-tap Gaussian folded into 5 bilinear taps
    vec2 step = Direction / InSize * Radius;
    vec3 sum = texture(InSampler, texCoord).rgb * 0.2270270270;
    sum += texture(InSampler, texCoord + step * 1.3846153846).rgb * 0.3162162162;
    sum += texture(InSampler, texCoord - step * 1.3846153846).rgb * 0.3162162162;
    sum += texture(InSampler, texCoord + step * 3.2307692308).rgb * 0.0702702703;
    sum += texture(InSampler, texCoord - step * 3.2307692308).rgb * 0.0702702703;
    fragColor = vec4(sum, 1.0);
}
