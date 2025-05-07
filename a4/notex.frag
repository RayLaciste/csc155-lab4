#version 430
uniform mat4 m_matrix;
uniform mat4 v_matrix;
uniform mat4 p_matrix;
uniform vec3 axisColor;

out vec4 fragColor;

void main(void) {
    fragColor = vec4(axisColor, 1.0);
}