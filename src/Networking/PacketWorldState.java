package Networking;

import a2.GameEntities.Player;
import a2.GameEntities.Puck;
import com.bulletphysics.linearmath.Transform;
import myGameEngine.NetworkHelpers.ClientInfo;
import myGameEngine.NetworkHelpers.NetworkFloat;
import myGameEngine.Singletons.EntityManager;
import myGameEngine.Singletons.TimeManager;
import ray.rml.Vector3;
import ray.rml.Vector3f;

import java.nio.ByteBuffer;

public class PacketWorldState extends Packet {
    // write variables

    // read variables
    private short tick;
    private Vector3 puckPosition;
    private javax.vecmath.Quat4f puckOrientation = new javax.vecmath.Quat4f();
    private javax.vecmath.Vector3f puckLinearVelocity = new javax.vecmath.Vector3f();
    private javax.vecmath.Vector3f puckAngularVelocity = new javax.vecmath.Vector3f();

    @Override
    public boolean isReliable() { return false; }

    @Override
    public byte getId() { return (byte)'w'; }

    @Override
    public ByteBuffer writeInfo() {
        ByteBuffer buffer = ByteBuffer.allocate(28);
        Puck puck = (Puck) EntityManager.get("puck").get(0);
        puckPosition = puck.getNode().getWorldPosition();
        puck.getBody().getOrientation(puckOrientation);
        puck.getBody().getLinearVelocity(puckLinearVelocity);
        puck.getBody().getAngularVelocity(puckAngularVelocity);

        // tick
        buffer.putShort(TimeManager.getTick());

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
    public void readInfo(ByteBuffer buffer) {
        tick = buffer.getShort();

        puckPosition = Vector3f.createFrom(
                NetworkFloat.decode(buffer.getShort()),
                NetworkFloat.decode(buffer.getShort()),
                NetworkFloat.decode(buffer.getShort())
        );

        puckOrientation.w = NetworkFloat.decode(buffer.getShort());
        puckOrientation.x = NetworkFloat.decode(buffer.getShort());
        puckOrientation.y = NetworkFloat.decode(buffer.getShort());
        puckOrientation.z = NetworkFloat.decode(buffer.getShort());

        puckLinearVelocity.x = NetworkFloat.decode(buffer.getShort());
        puckLinearVelocity.y = NetworkFloat.decode(buffer.getShort());
        puckLinearVelocity.z = NetworkFloat.decode(buffer.getShort());

        puckAngularVelocity.x = NetworkFloat.decode(buffer.getShort());
        puckAngularVelocity.y = NetworkFloat.decode(buffer.getShort());
        puckAngularVelocity.z = NetworkFloat.decode(buffer.getShort());
    }

    @Override
    public void receivedOnServer(ClientInfo cli) {
        // should not happen
    }

    @Override
    public void receivedOnClient() {
        // TODO: rewind and stuff
        if (tick - TimeManager.getTick() > 10 || tick - TimeManager.getTick() < 0) {
            //TimeManager.setTick(tick);
        }

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

        System.out.println("world state: " + tick);

        /*
            TODO PLAN:
            servertime is a short that wraps
            custom time difference function
            custom time within function
            custom time max/min funtions

            joinSuccess NEEDS server time
                set client time to (server time + 1)

            client @t0 -(10)-> @t10	server
                rewind & apply input from client & fastforward
                    apply list of client inputs
                    fastforward to server time

            server @t10 -(10)-> @t10 client
                rewind & apply input from client & 'fastforward' if required
                    keep and apply list of client inputs
                    if client time > server time + server tickrate
                        client time = client time - 1

                        rewind to server time
                        apply state
                            ignore client's server state if the positions are within a defined acceptable error distance
                            never override client inputs
                            save history @ server time
                        fast forward to client time
                            apply client inputs

                        apply real client inputs
                        jumping/attacking/etc is true
                            if previous (client time + 1) input is true
                            if real input is true
                    else if client time > server time
                        rewind to server time
                        apply state
                            ignore client's server state if the positions are within a defined acceptable error distance
                            never override client inputs
                            save history @ server time
                        fast forward to client time
                            apply client inputs

                        apply real client inputs
                        jumping/attacking/etc is true
                            if real input is true

                    else (client time <= server time)
                        fast forward to server time
                        apply state
                            ignore client's server state if the positions are within a defined acceptable error distance
                            never override client inputs
                            save history @ server time
                        apply real client inputs
                        jumping/attacking/etc is true
                            if real input is true
                        fast forward one tick

         */
    }
}
