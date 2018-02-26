#version 330

layout (location=0) in vec3 inPos;
layout (location=1) in vec2 inUV;
layout (location=2) in vec3 inNormal;

out vec3 worldPos;
out vec2 vertUV;
out vec3 normal;

uniform mat4 worldMatrix;
uniform mat4 viewMatrix;
uniform mat4 projectionMatrix;

void main()
{
    worldPos = (worldMatrix * vec4(inPos, 1.0)).xyz;
    vertUV = inUV;
    normal = (worldMatrix * vec4(inNormal, 0.0)).xyz;

    gl_Position = projectionMatrix * viewMatrix * vec4(worldPos, 1.0);
}
