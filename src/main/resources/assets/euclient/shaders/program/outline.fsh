#version 330

#define Width 3

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

uniform vec2 InSize;
uniform int RenderMode;
uniform float FillOpacity;

out vec4 fragColor;

float quad(float x) {
    return x * x;
}

void main() {
    float divider = 5;
    float maxSample = 3;
    vec4 current = texture(DiffuseSampler, texCoord);

    if (current.a != 0) {
        if (RenderMode == 1) discard;
        fragColor = vec4(current.rgb, current.a * FillOpacity);
    } else {
        if (RenderMode == 0) discard;
        float alpha = 0;

        for (float x = -Width; x < Width; x++) {
            for (float y = -Width; y < Width; y++) {
                vec4 texture = texture(DiffuseSampler, texCoord + vec2(x, y) * oneTexel);

                if (texture.a != 0) {
                    current = texture;
                    alpha += max(0, (maxSample - distance(vec2(x, y), vec2(0))) / divider);
                }
            }
        }

        fragColor = vec4(current.rgb, quad(alpha));
    }
}