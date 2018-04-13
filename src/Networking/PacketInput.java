package Networking;

import a2.Contollers.CharacterController;
import a2.GameEntities.Player;
import myGameEngine.NetworkHelpers.ClientInfo;
import myGameEngine.Singletons.HistoryManager;
import myGameEngine.Singletons.TimeManager;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class PacketInput extends Packet {
    private static int listTickCount = (144 / UDPServer.updateRate + 1) * 1; // allow for 6 dropped updates

    // write variables
    private Player player;

    // read variables
    private short onTick;
    private ArrayList<PacketInputInfo> infos;

    private class PacketInputInfo {
        public short tick;
        public byte controls;
        public short aimX;
        public short aimY;
    }

    public PacketInput() { }

    public PacketInput(Player player) {
        this.player = player;
    }

    @Override
    public boolean isReliable() { return false; }

    @Override
    public byte getId() { return (byte)'i'; }

    @Override
    public ByteBuffer writeInfo() {
        ByteBuffer buffer = ByteBuffer.allocate(2 + 5 * listTickCount);
        short onTick = TimeManager.getTick();
        buffer.putShort(onTick);

        for (int i = 0; i < listTickCount; i++) {
            HistoryManager.HistoryState state = HistoryManager.getState((short)(onTick - i));
            if (state == null) { System.out.println("COULDNT FIND STATE"); break; }

            CharacterController.HistoryCharacterController controller = state.controllers.getHistory(player);
            if (controller == null) { System.out.println("COULDNT FIND CONTROLLER"); break; }

            buffer.put(controller.getControls());
            buffer.putShort((short)0); // TODO: controller.getAimX()
            buffer.putShort((short)0); // TODO: controller.getAimY()

            //System.out.println("OVERWRITE: " + (short)(onTick - i) + ", " + controller.getControls());
        }

        return buffer;
    }

    @Override
    public void readInfo(ByteBuffer buffer) {
        onTick = buffer.getShort();
        short cTick = onTick;
        infos = new ArrayList<>();
        while (buffer.position() < buffer.limit()) {
            PacketInputInfo info = new PacketInputInfo();
            info.tick = cTick;
            info.controls = buffer.get();
            info.aimX = buffer.getShort();
            info.aimY = buffer.getShort();
            infos.add(info);
            cTick--;
        }
    }

    @Override
    public void receivedOnServer(ClientInfo cli) {
        Player player = UDPServer.getPlayer(cli);
        short currentTick = TimeManager.getTick();
        short rewindTo = currentTick;

        PacketInputInfo firstInfo = infos.get(0);
        if (TimeManager.getTick() - firstInfo.tick > 0) {
            for (short i = firstInfo.tick; i != (short)(TimeManager.getTick() + 1); i++) {
                HistoryManager.HistoryState state = HistoryManager.getState(i);
                if (state == null) { System.out.println("SKIPPING: STATE"); continue; }

                CharacterController.HistoryCharacterController controller = state.controllers.getHistory(player);
                if (controller == null) { System.out.println("SKIPPING: CONTROLLER"); continue; }

                controller.overwriteInput(firstInfo.controls, firstInfo.aimX, firstInfo.aimY);
            }
        }


        for (PacketInputInfo info : infos) {
            HistoryManager.HistoryState state = HistoryManager.getState(info.tick);
            if (state == null) { System.out.println("SKIPPING: STATE"); continue; }
            CharacterController.HistoryCharacterController controller = state.controllers.getHistory(player);

            if (controller == null) { System.out.println("SKIPPING: CONTROLLER"); continue; }
            controller.overwriteInput(info.controls, info.aimX, info.aimY);

            //System.out.println("OVERWRITE: " + info.tick + ", " + info.controls);

            if (info.tick - currentTick < rewindTo - currentTick) {
                rewindTo = info.tick;
            }
        }
        /*player.getController().setAimX(infos.get(0).aimX); TODO: AIMX AIMY
        player.getController().setAimY(infos.get(0).aimY);*/
        System.out.println("Received input, rewinding " + (currentTick - rewindTo));
        HistoryManager.rewind(rewindTo);
        //player.getController().setControls(infos.get(0).controls);
    }

    @Override
    public void receivedOnClient() {
        // should not happen
    }
}
