package a2;

import ray.rml.Vector3;

public interface Attackable {
    void attacked(Vector3 aim, Vector3 relative);
    byte getId();
}
