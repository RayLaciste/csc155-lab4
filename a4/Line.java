package a4;

import org.joml.*;

// Primitive shape with start and end points defined when created, used as world axes

public class Line {
	float[] vertices = new float[6];

	public Line(Vector3f start, Vector3f end) {	
		vertices[0] = start.x();
		vertices[1] = start.y();
		vertices[2] = start.z();
		vertices[3] = end.x();
		vertices[4] = end.y();
		vertices[5] = end.z();
	}

    public float[] getVertices() { return vertices; }
}