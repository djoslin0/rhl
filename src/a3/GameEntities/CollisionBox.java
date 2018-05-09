package a3.GameEntities;

import ray.rml.Vector2;
import ray.rml.Vector3;

public class CollisionBox {
    private float x1Pos,x2Pos;
    private float y1Pos,y2Pos;
    private float z1Pos,z2Pos;
    private Vector3 location;
    public CollisionBox(float xLeangth, float yLeangth, float zLeangth,Vector3 location){
        // this box is specified by the leangth's of the sides with the center at the specified location
        this.location = location;
        x1Pos = location.x() - xLeangth / 2;
        x2Pos = location.x() + xLeangth / 2;
        y1Pos = location.y() - yLeangth / 2;
        y2Pos = location.y() + yLeangth / 2;
        z1Pos = location.z() - zLeangth / 2;
        z2Pos = location.z() + zLeangth / 2;

    }
    // used to set dunk to true : for david
    public boolean contains(Vector3 contains){
        if(contains.x() < x2Pos && contains.x() > x1Pos && contains.y() > y1Pos && contains.y() < y2Pos && contains.z() > z1Pos && contains.z() < z2Pos){
            return true;
        }
        return false;
    }

    // used for keeping the value of dunk true as long and the puck is bouncing around on top of the goal: for david
    public boolean Contains2d(Vector2 contains){
        if(contains.x() > x1Pos && contains.x() < x2Pos && contains.y() > z1Pos && contains.y() <z2Pos ){
            return true;
        }
        return false;
    }

    public boolean below(Vector3 position) {
        return position.y() < y1Pos && position.y() < y2Pos;
    }
}
