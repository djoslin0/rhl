package Networking;

import a3.GameEntities.Player;
import a3.GameEntities.Puck;
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
        Puck puck = EntityManager.getPuck();
        if (attackedId == (byte)-1) {
            attacker.getGlove().attack(null, relative);
        } else if (attackedId == puck.getId()) {
            attacker.getGlove().attack(puck, puck.getNode().getWorldPosition().add(relative));
            puck.attacked(aim, relative);
        } else {
            Player attacked = UDPServer.getPlayer(attackedId);
            attacker.getGlove().attack(attacked, attacked.getNode().getWorldPosition().add(relative));
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
        if (attacker == null || attacker.getGlove() == null) { return; }
        Puck puck = EntityManager.getPuck();
        if (attackedId == (byte)-1) {
            attacker.getGlove().attack(null, relative);
        } else if (attackedId == puck.getId()) {
            attacker.getGlove().attack(puck, puck.getNode().getWorldPosition().add(relative));
        } else {
            Player attacked = UDPClient.getPlayer(attackedId);
            attacker.getGlove().attack(attacked, attacked.getNode().getWorldPosition().add(relative));
            attacked.attacked(aim, relative);
        }
    }
}
