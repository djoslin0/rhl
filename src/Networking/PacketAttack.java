package Networking;

import myGameEngine.NetworkHelpers.ClientInfo;
import myGameEngine.NetworkHelpers.NetworkFloat;
import myGameEngine.Singletons.EntityManager;
import ray.rml.Vector3;
import ray.rml.Vector3f;

import java.nio.ByteBuffer;

public class PacketAttack extends Packet {
    // read/write variables
    private Vector3 aim;
    private Vector3 relative;

    public PacketAttack() {}

    public PacketAttack(Vector3 aim, Vector3 relative) {
        this.aim = aim;
        this.relative = relative;
    }

    @Override
    public boolean isReliable() { return true; }

    @Override
    public byte getId() { return (byte)'t'; }

    @Override
    public ByteBuffer writeInfo() {
        ByteBuffer buffer = ByteBuffer.allocate(12);
        buffer.putShort(NetworkFloat.encode(aim.x()));
        buffer.putShort(NetworkFloat.encode(aim.y()));
        buffer.putShort(NetworkFloat.encode(aim.z()));
        buffer.putShort(NetworkFloat.encode(relative.x()));
        buffer.putShort(NetworkFloat.encode(relative.y()));
        buffer.putShort(NetworkFloat.encode(relative.z()));
        return buffer;
    }

    @Override
    public void readInfo(ByteBuffer buffer) {
        this.aim = Vector3f.createFrom(
                NetworkFloat.decode(buffer.getShort()),
                NetworkFloat.decode(buffer.getShort()),
                NetworkFloat.decode(buffer.getShort()));
        this.relative = Vector3f.createFrom(
                NetworkFloat.decode(buffer.getShort()),
                NetworkFloat.decode(buffer.getShort()),
                NetworkFloat.decode(buffer.getShort()));
    }

    @Override
    public void receivedOnServer(ClientInfo cli) {
        EntityManager.getPuck().attack(aim, relative);
    }

    @Override
    public void receivedOnClient() {
        // should not happen
    }
}
