#version 430

in vec2 tc;

in vec3 varyingNormal;
in vec3 varyingLightDir;
in vec3 varyingVertPos;

out vec4 fragColor;

struct PositionalLight
{	vec4 ambient;  
	vec4 diffuse;  
	vec4 specular;  
	vec3 position;
};

struct Material
{	vec4 ambient;  
	vec4 diffuse;  
	vec4 specular;  
	float shininess;
};

uniform vec3 fogColor;
uniform float fogDensity;
uniform float fogStartDistance;
uniform float fogMaxDistance;

uniform vec4 globalAmbient;
uniform PositionalLight light;
uniform Material material;
uniform mat4 m_matrix;
uniform mat4 v_matrix;	 
uniform mat4 p_matrix;
uniform mat4 norm_matrix;

uniform sampler2D samp;

uniform float alpha;
uniform float flipNormal;

uniform float reflectionFactor = 0.5;
layout (binding = 0) uniform samplerCube skybox;

float calculateFog(float dist) {
    return 1.0 - clamp(exp(-fogDensity * (dist - fogStartDistance)), 0.0, 1.0);
}


void main(void)
{	
	// normalize the light, normal, and view vectors:
	vec3 L = normalize(varyingLightDir);
	vec3 N = normalize(varyingNormal);
	vec3 V = normalize(-v_matrix[3].xyz - varyingVertPos);
	
	// compute light reflection vector, with respect N:
	vec3 R = normalize(reflect(-L, N));
	
	// get the angle between the light and surface normal:
	float cosTheta = dot(L,N);
	
	// angle between the view vector and reflected light:
	float cosPhi = dot(V,R);

    vec3 envMapR = reflect(-V, N);

    vec4 texColor = texture(samp, tc);
    vec4 reflectionColor = texture(skybox, envMapR);

	// compute ADS contributions (per pixel):
	vec3 ambient = ((globalAmbient * material.ambient) + (light.ambient * material.ambient)).xyz;
	vec3 diffuse = light.diffuse.xyz * material.diffuse.xyz * max(cosTheta,0.0);
	vec3 specular = light.specular.xyz * material.specular.xyz * pow(max(cosPhi,0.0), material.shininess);
    
	vec4 mixedColor = mix(texColor, reflectionColor, reflectionFactor);

    vec4 originalColor = vec4(mixedColor.xyz * (ambient + diffuse + specular), alpha);
	
	float dist = length(varyingVertPos);
	float fogFactor = calculateFog(dist);

	fragColor = mix(originalColor, vec4(fogColor, alpha), fogFactor);
}
