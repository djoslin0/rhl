package Networking;

import a2.GameEntities.Player;
import a2.GameEntities.Puck;
import com.bulletphysics.linearmath.Transform;
import myGameEngine.NetworkHelpers.ClientInfo;
import myGameEngine.NetworkHelpers.NetworkFloat;
import myGameEngine.Singletons.EntityManager;
import ray.rml.Vector3;
import ray.rml.Vector3f;

import java.nio.ByteBuffer;

public class PacketWorldState extends Packet {
    // write variables

    // read variables
    Vector3 puckPosition;
    javax.vecmath.Quat4f puckOrientation = new javax.vecmath.Quat4f();
    javax.vecmath.Vector3f puckLinearVelocity = new javax.vecmath.Vector3f();
    javax.vecmath.Vector3f puckAngularVelocity = new javax.vecmath.Vector3f();

    @Override
    public boolean isReliable() { return false; }

    @Override
    public byte getId() { return (byte)'w'; }

    @Override
    public ByteBuffer writeInfo() {
        ByteBuffer buffer = ByteBuffer.allocate(26);
        Puck puck = (Puck) EntityManager.get("puck").get(0);
        puckPosition = puck.getNode().getWorldPosition();
        puck.getBody().getOrientation(puckOrientation);
        puck.getBody().getLinearVelocity(puckLinearVelocity);
        puck.getBody().getAngularVelocity(puckAngularVelocity);

        // position
        buffer.putShort(NetworkFloat.encode(puckPosition.x()));
        buffer.putShort(NetworkFloat.encode(puckPosition.y()));
        buffer.putShort(NetworkFloat.encode(puckPosition.z()));

        // orientation
        buffer.putShort(NetworkFloat.encode(puckOrientation.w));
        buffer.putShort(NetworkFloat.encode(puckOrientation.x));
        buffer.putShort(NetworkFloat.encode(puckOrientation.y));
        buffer.putShort(NetworkFloat.encode(puckOrientation.z));

        // linear velocity
        buffer.putShort(NetworkFloat.encode(puckLinearVelocity.x));
        buffer.putShort(NetworkFloat.encode(puckLinearVelocity.y));
        buffer.putShort(NetworkFloat.encode(puckLinearVelocity.z));

        // angular velocity
        buffer.putShort(NetworkFloat.encode(puckAngularVelocity.x));
        buffer.putShort(NetworkFloat.encode(puckAngularVelocity.y));
        buffer.putShort(NetworkFloat.encode(puckAngularVelocity.z));
        return buffer;
    }

    @Override
    public void receivedOnServer(ClientInfo cli) {
        // should not happen
    }

    @Override
    public void readInfo(ByteBuffer info) {
        puckPosition = Vector3f.createFrom(
                NetworkFloat.decode(info.getShort()),
                NetworkFloat.decode(info.getShort()),
                NetworkFloat.decode(info.getShort())
        );

        puckOrientation.w = NetworkFloat.decode(info.getShort());
        puckOrientation.x = NetworkFloat.decode(info.getShort());
        puckOrientation.y = NetworkFloat.decode(info.getShort());
        puckOrientation.z = NetworkFloat.decode(info.getShort());

        puckLinearVelocity.x = NetworkFloat.decode(info.getShort());
        puckLinearVelocity.y = NetworkFloat.decode(info.getShort());
        puckLinearVelocity.z = NetworkFloat.decode(info.getShort());

        puckAngularVelocity.x = NetworkFloat.decode(info.getShort());
        puckAngularVelocity.y = NetworkFloat.decode(info.getShort());
        puckAngularVelocity.z = NetworkFloat.decode(info.getShort());
    }

    @Override
    public void receivedOnClient() {
        Puck puck = (Puck) EntityManager.get("puck").get(0);

        // set position
        Transform puckTransform = new Transform();
        puck.getBody().getWorldTransform(puckTransform);
        puckTransform.origin.set(puckPosition.x(), puckPosition.y(), puckPosition.z());

        // orientation
        puckTransform.setRotation(puckOrientation);
        puck.getBody().setWorldTransform(puckTransform);

        // velocities
        puck.getBody().setLinearVelocity(puckLinearVelocity);
        puck.getBody().setAngularVelocity(puckAngularVelocity);

        System.out.println("world state.");
    }
}
