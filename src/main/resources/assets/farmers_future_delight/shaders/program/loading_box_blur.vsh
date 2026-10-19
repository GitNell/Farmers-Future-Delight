#version 120

attribute vec4 Position;
uniform mat4 ProjMat;
uniform vec2 InSize;
uniform vec2 OutSize;
uniform vec2 BlurDir;
varying vec2 texCoord;
varying vec2 sampleStep;

void main() {
    vec4 outPos = ProjMat * vec4(Position.xy, 0.0, 1.0);
    gl_Position = vec4(outPos.xy, 0.2, 1.0);
    sampleStep = BlurDir / InSize;
    texCoord = Position.xy / OutSize;
}
