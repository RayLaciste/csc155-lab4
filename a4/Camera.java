package a4;
import org.joml.*;
import org.joml.Math;

public class Camera {
    private Vector3f position;
    private float pitch;
    private float yaw;
    private float moveSpeed;
    private float rotateSpeed;
    private Vector3f up;
    private Vector3f right;
    private Vector3f forward;
    private Matrix4f viewMatrix;
    
    public Camera() {
        position = new Vector3f(0.0f, 1.75f, 5.0f);
        pitch = 0.0f;
        yaw = -(float) Math.PI/2.0f;
        moveSpeed = 5.0f;
        rotateSpeed = 0.025f;
        up = new Vector3f(0.0f, 1.0f, 0.0f);
        right = new Vector3f(1.0f, 0.0f, 0.0f);
        forward = new Vector3f(0.0f, 0.0f, -1.0f);
        viewMatrix = new Matrix4f();
        updateVectors();
    }
    
    private void updateVectors() {
        forward.x = (float)Math.cos(yaw) * (float)Math.cos(pitch);
        forward.y = (float)Math.sin(pitch);
        forward.z = (float)Math.sin(yaw) * (float)Math.cos(pitch);
        forward.normalize();
        right = new Vector3f(forward).cross(new Vector3f(0.0f, 1.0f, 0.0f)).normalize();
        up = new Vector3f(right).cross(forward).normalize();
    }
    
    public void updateViewMatrix() {
        updateVectors();
        viewMatrix.identity();
        viewMatrix.lookAt(
                position,
                new Vector3f(position).add(forward),
                up
        );
    }
    
    // Methods to be compatible with the main Code.java
    public void setLocation(Vector3f location) {
        this.position = new Vector3f(location);
        updateViewMatrix();
    }
    
    public void lookAt(float x, float y, float z) {
        Vector3f target = new Vector3f(x, y, z);
        Vector3f direction = new Vector3f(target).sub(position);
        
        // Calculate yaw and pitch from direction vector
        yaw = (float)Math.atan2(direction.z, direction.x);
        float length = (float)Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        pitch = (float)Math.atan2(direction.y, length);
        
        updateVectors();
        updateViewMatrix();
    }
    
    public void rotate(float yawDelta, float pitchDelta, float deltaTime) {
        if (yawDelta != 0 || pitchDelta != 0) {
            yaw += yawDelta * rotateSpeed * deltaTime;
            pitch += pitchDelta * rotateSpeed * deltaTime;
            
            // Constrain pitch to avoid gimbal lock
            pitch = Math.clamp(-1.5f, 1.5f, pitch);
            
            updateVectors();
            updateViewMatrix();
        }
    }
    
    public void move(float forwardAmount, float rightAmount, float upAmount, float deltaTime) {
        Vector3f movement = new Vector3f();
        
        if (forwardAmount != 0) {
            movement.add(new Vector3f(forward).mul(forwardAmount));
        }
        
        if (rightAmount != 0) {
            movement.add(new Vector3f(right).mul(rightAmount));
        }
        
        if (upAmount != 0) {
            movement.add(new Vector3f(up).mul(upAmount));
        }
        
        if (movement.length() > 0) {
            movement.normalize().mul(moveSpeed * deltaTime);
            position.add(movement);
            updateViewMatrix();
        }
    }
    
    // Individual movement methods for direct key handling
    public void moveForward(float amount) {
        position.add(new Vector3f(forward).mul(amount));
        updateViewMatrix();
    }
    
    public void moveBackward(float amount) {
        position.add(new Vector3f(forward).mul(-amount));
        updateViewMatrix();
    }
    
    public void strafeLeft(float amount) {
        position.add(new Vector3f(right).mul(-amount));
        updateViewMatrix();
    }
    
    public void strafeRight(float amount) {
        position.add(new Vector3f(right).mul(amount));
        updateViewMatrix();
    }
    
    public void moveUp(float amount) {
        position.add(new Vector3f(up).mul(amount));
        updateViewMatrix();
    }
    
    public void moveDown(float amount) {
        position.add(new Vector3f(up).mul(-amount));
        updateViewMatrix();
    }
    
    public void pitch(float amount) {
        pitch += amount * rotateSpeed;
        pitch = Math.clamp(-1.5f, 1.5f, pitch);
        updateViewMatrix();
    }
    
    public void yaw(float amount) {
        yaw += amount * rotateSpeed;
        updateViewMatrix();
    }
    
    public Matrix4f getViewMatrix() {
        return viewMatrix;
    }
    
    public Vector3f getPosition() {
        return new Vector3f(position);
    }
    
    public Vector3f getForward() {
        return new Vector3f(forward);
    }
    
    public void setMoveSpeed(float moveSpeed) {
        this.moveSpeed = moveSpeed;
    }
    
    public void setRotateSpeed(float rotateSpeed) {
        this.rotateSpeed = rotateSpeed;
    }
}