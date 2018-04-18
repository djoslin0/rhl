package Networking;

import a2.Contollers.CharacterController;
import a2.GameEntities.Player;
import com.bulletphysics.linearmath.Transform;
import myGameEngine.NetworkHelpers.ClientInfo;
import myGameEngine.NetworkHelpers.NetworkFloat;
import myGameEngine.Singletons.TimeManager;
import ray.rml.Matrix3;
import ray.rml.Matrix3f;

import java.nio.ByteBuffer;

public class PacketInput extends Packet {
    // read variables
    private short tick;
    public byte controls;
    public float pitch;
    public float yaw;
    private float x;
    private float y;
    private float z;

    public PacketInput() { }

    @Override
    public boolean isReliable() { return false; }

    @Override
    public byte getId() { return (byte)'i'; }

    @Override
    public ByteBuffer writeInfo() {
        ByteBuffer buffer = ByteBuffer.allocate(13);

        short onTick = TimeManager.getTick();
        buffer.putShort(onTick);

        Player player = Networking.UDPClient.getPlayer();
        buffer.put(player.getController().getControls());

        float pitch = (float)player.getCameraNode().getLocalRotation().getPitch();
        buffer.putShort(NetworkFloat.encode(pitch * 100f));

        float yaw = (float)player.getNode().getLocalRotation().getYaw();
        buffer.putShort(NetworkFloat.encode(yaw * 100f));

        Transform t = new Transform();
        player.getBody().getWorldTransform(t);
        buffer.putShort(NetworkFloat.encode(t.origin.x));
        buffer.putShort(NetworkFloat.encode(t.origin.y));
        buffer.putShort(NetworkFloat.encode(t.origin.z));

        return buffer;
    }

    @Override
    public void readInfo(ByteBuffer buffer) {
        tick = buffer.getShort();
        controls = buffer.get();
        pitch = NetworkFloat.decode(buffer.getShort()) / 100f;
        yaw = NetworkFloat.decode(buffer.getShort()) / 100f;
        x = NetworkFloat.decode(buffer.getShort());
        y = NetworkFloat.decode(buffer.getShort());
        z = NetworkFloat.decode(buffer.getShort());
    }

    @Override
    public void receivedOnServer(ClientInfo cli) {
        Player player = UDPServer.getPlayer(cli);
        CharacterController controller = player.getController();
        controller.setControls(controls);
        double wrapYawValue = CharacterController.wrapYawFromControl(controls) ? Math.PI : 0;

        Matrix3 cameraNodeRotation = Matrix3f.createFrom(pitch, 0, 0);
        player.getCameraNode().setLocalRotation(cameraNodeRotation);

        Matrix3 nodeRotation = Matrix3f.createFrom(wrapYawValue, yaw, wrapYawValue);
        player.getNode().setLocalRotation(nodeRotation);

        Transform t = new Transform();
        player.getBody().getWorldTransform(t);
        t.origin.x = x;
        t.origin.y = y;
        t.origin.z = z;
        player.getBody().proceedToTransform(t);
        player.getBody().getMotionState().setWorldTransform(t);
    }

    @Override
    public void receivedOnClient() {
        // should not happen
    }
}
