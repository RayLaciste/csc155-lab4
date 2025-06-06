#version 430

layout (location = 0) in vec3 vertPos;
layout (location = 1) in vec2 texCoord;
layout (location = 2) in vec3 vertNormal;

out vec3 varyingNormal;
out vec3 varyingLightDir;
out vec3 varyingVertPos;
out vec2 tc;

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

uniform vec4 globalAmbient;
uniform PositionalLight light;
uniform Material material;
uniform mat4 m_matrix;
uniform mat4 v_matrix;
uniform mat4 p_matrix;
uniform mat4 norm_matrix;

uniform float alpha;
uniform float flipNormal;

void main(void)
{	
    varyingVertPos = (v_matrix * m_matrix * vec4(vertPos,1.0)).xyz;
	
    varyingLightDir = (v_matrix * vec4(light.position, 1.0)).xyz - varyingVertPos;
	
    varyingNormal = (norm_matrix * vec4(vertNormal,1.0)).xyz;

    tc = texCoord;

    gl_Position = p_matrix * v_matrix * m_matrix * vec4(vertPos,1.0);
}
