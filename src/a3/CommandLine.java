package a3;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class CommandLine {
    private static CommandLine instance = new CommandLine();
    private boolean server = false;
    private boolean client = false;
    private InetAddress ip;
    private int port;
    private byte headId = 1;
    private int bots = -1;
    private DisplaySettings display;

    public static void read(String[] args) {
        if (args.length <= 0) { return; }
        if (args[0].equals("s")) {
            instance.server = true;
            instance.port = Integer.parseInt(args[1]);
        } else if (args[0].equals("c")) {
            try {
                instance.client = true;
                instance.ip = InetAddress.getByName(args[1]);
                instance.port = Integer.parseInt(args[2]);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }

        for (String arg : args) {
            if (arg.toLowerCase().startsWith("head=")) {
                instance.headId = Byte.parseByte(arg.split("=", 2)[1]);
            } else if (arg.toLowerCase().startsWith("bots=")) {
                instance.bots = Integer.parseInt(arg.split("=", 2)[1]);
            } else if (arg.toLowerCase().startsWith("display=")) {
                String[] s = arg.split("=", 2)[1].split(",");
                instance.display = new DisplaySettings();
                instance.display.width = Integer.parseInt(s[0]);
                instance.display.height = Integer.parseInt(s[1]);
                instance.display.bitDepth = Integer.parseInt(s[2]);
                instance.display.refreshRate = Integer.parseInt(s[3]);
                instance.display.fullscreen = s[4].equals("1");
                instance.display.vsync = s[5].equals("1");
            }
        }
    }

    public static boolean isServer() { return instance.server; }
    public static boolean isClient() { return instance.client; }
    public static InetAddress getIp() { return instance.ip; }
    public static int getPort() { return instance.port; }
    public static byte getHeadId() { return instance.headId; }
    public static int getBots() { return instance.bots; }
    public static DisplaySettings getDisplaySettings() { return instance.display; }

    public static class DisplaySettings {
        public int width;
        public int height;
        public int bitDepth;
        public int refreshRate;
        public boolean fullscreen;
        public boolean vsync;
    }
}
