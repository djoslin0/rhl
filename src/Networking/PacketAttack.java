package Networking;

import a2.GameEntities.Player;
import a2.GameEntities.Puck;
import myGameEngine.NetworkHelpers.ClientInfo;
import myGameEngine.NetworkHelpers.NetworkFloat;
import myGameEngine.Singletons.EntityManager;
import ray.rml.Vector3;
import ray.rml.Vector3f;

import java.nio.ByteBuffer;

public class PacketAttack extends Packet {
    // read/write variables
    private byte attackerId;
    private byte attackedId;
    private Vector3 aim;
    private Vector3 relative;

    public PacketAttack() {}

    public PacketAttack(byte attackerId, byte attackedId, Vector3 aim, Vector3 relative) {
        this.attackerId = attackerId;
        this.attackedId = attackedId;
        this.aim = aim;
        this.relative = relative;
    }

    @Override
    public boolean isReliable() { return true; }

    @Override
    public byte getId() { return (byte)'t'; }

    @Override
    public ByteBuffer writeInfo() {
        ByteBuffer buffer = ByteBuffer.allocate(14);
        buffer.put(attackerId);
        buffer.put(attackedId);
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
        attackerId = buffer.get();
        attackedId = buffer.get();

        aim = Vector3f.createFrom(
                NetworkFloat.decode(buffer.getShort()),
                NetworkFloat.decode(buffer.getShort()),
                NetworkFloat.decode(buffer.getShort()));

        relative = Vector3f.createFrom(
                NetworkFloat.decode(buffer.getShort()),
                NetworkFloat.decode(buffer.getShort()),
                NetworkFloat.decode(buffer.getShort()));
    }

    @Override
    public void receivedOnServer(ClientInfo cli) {
        Player attacker = UDPServer.getPlayer(cli);
        if (attackedId == -1) {
            Puck puck = EntityManager.getPuck();
            attacker.getGlove().attack(puck.getNode().getWorldPosition().add(relative));
            puck.attacked(aim, relative);
        } else {
            Player attacked = UDPServer.getPlayer(attackedId);
            attacker.getGlove().attack(attacked.getNode().getWorldPosition().add(relative));
            attacked.attacked(aim, relative);
        }

        // broadcast
        PacketAttack attack = new PacketAttack(attacker.getId(), attackedId, aim, relative);
        UDPServer.sendToAll(attack);
    }

    @Override
    public void receivedOnClient() {
        if (attackerId == UDPClient.getPlayerId()) { return; }
        Player attacker = UDPClient.getPlayer(attackerId);
        if (attackedId == -1) {
            Puck puck = EntityManager.getPuck();
            attacker.getGlove().attack(puck.getNode().getWorldPosition().add(relative));
        } else {
            Player attacked = UDPClient.getPlayer(attackedId);
            attacker.getGlove().attack(attacked.getNode().getWorldPosition().add(relative));
            attacked.attacked(aim, relative);
        }
    }
}
