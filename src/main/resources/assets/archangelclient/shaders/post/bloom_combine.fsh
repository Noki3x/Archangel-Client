#version 330

// Adds the blurred highlights back onto the scene.
uniform sampler2D InSampler;
uniform sampler2D BloomSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform BloomCombineConfig {
    float Intensity;
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec3 scene = texture(InSampler, texCoord).rgb;
    vec3 bloom = texture(BloomSampler, texCoord).rgb;
    // screen-style blend keeps bright areas from clipping straight to white
    vec3 added = bloom * Intensity;
    fragColor = vec4(1.0 - (1.0 - scene) * (1.0 - clamp(added, 0.0, 1.0)), 1.0);
}
