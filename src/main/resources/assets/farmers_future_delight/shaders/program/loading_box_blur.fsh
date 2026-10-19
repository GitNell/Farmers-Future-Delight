#version 120

uniform sampler2D DiffuseSampler;
uniform float Radius;
varying vec2 texCoord;
varying vec2 sampleStep;

void main() {
    vec4 blurred = vec4(0.0);
    float actualRadius = floor(Radius + 0.5);
    for (float offset = -actualRadius + 0.5; offset <= actualRadius; offset += 2.0) {
        blurred += texture2D(DiffuseSampler, texCoord + sampleStep * offset);
    }
    blurred += texture2D(DiffuseSampler, texCoord + sampleStep * actualRadius) / 2.0;
    gl_FragColor = blurred / (actualRadius + 0.5);
}
