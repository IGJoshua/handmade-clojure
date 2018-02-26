#version 330

struct ambient_light
{
    vec3 color;
    float intensity;
};

struct directional_light
{
    vec3 direction;
    vec3 color;
    float intensity;
};

struct point_light
{
    vec3 pos;
    vec3 color;
    float intensity;
    float radius;
};

struct spot_light
{
    vec3 pos;
    vec3 direction;
    vec3 color;
    float intensity;
    float innerRadius;
    float outerRadius;
    float radius;
};

const int MAX_POINT_LIGHTS = 16;
const int MAX_SPOT_LIGHTS = 16;

in vec3 worldPos;
in vec2 vertUV;
in vec3 normal;

out vec4 fragColor;

uniform sampler2D textureSampler;
uniform vec3 cameraPosition;
uniform ambient_light ambientLight;
uniform directional_light directionalLight;
uniform point_light pointLights[MAX_POINT_LIGHTS];
uniform spot_light spotLights[MAX_SPOT_LIGHTS];

vec4 calculateAmbientLight(ambient_light light)
{
    return vec4(light.color * light.intensity, 1);
}

vec4 calculateDirectionalLight(directional_light light, vec3 fragmentNormal)
{
    return vec4(clamp(dot(normalize(-light.direction), fragmentNormal), 0, 1)
                * light.color * light.intensity, 1);
}

vec4 calculatePointLight(point_light light, vec3 fragmentPosition, vec3 fragmentNormal)
{
    // Lambertien
    vec3 lightToFragment = light.pos - fragmentPosition;
    float normalAttenuation = clamp(dot(normalize(lightToFragment), fragmentNormal), 0, 1);
    float distanceAttenuation = pow(1 - clamp(length(lightToFragment) / light.radius, 0, 1), 2);

    return vec4(normalAttenuation * distanceAttenuation * light.color * light.intensity, 1);
}

vec4 calculateSpotLight(spot_light light, vec3 fragmentPosition, vec3 fragmentNormal)
{
    // Lambertien
    vec3 lightToFragment = fragmentPosition - light.pos;
    float normalAttenuation = clamp(dot(fragmentNormal, normalize(-lightToFragment)), 0, 1);
    float coneAttenuation = pow(1.0 - clamp((light.innerRadius - dot(normalize(light.direction),
                                                                     lightToFragment))
                                            / (light.innerRadius - light.outerRadius),
                                            0, 1), 2);
    float distanceAttenuation = 1.0 - clamp(length(lightToFragment) / light.radius, 0, 1);

    return vec4(light.color * light.intensity
                * normalAttenuation * coneAttenuation
                * pow(distanceAttenuation, 2), 1);
}

vec4 calculateSpecularLight(vec3 lightDirection, vec3 lightColor, vec3 fragmentPosition,
                            vec3 fragmentNormal, vec3 cameraPosition)
{
    float power = 256;
    float specularIntensity = 0.7;

    vec3 viewDirection = normalize(cameraPosition - fragmentPosition);
    vec3 halfVector = normalize(normalize(-lightDirection) + viewDirection);
    float intensity = max(pow(clamp(dot(fragmentNormal, halfVector), 0, 1), power), 0);

    return vec4(lightColor * specularIntensity * intensity, 1);
}

void main()
{
    fragColor = vec4(ambientLight.color, 1) + vec4(1, 1, 1, 1);
}
