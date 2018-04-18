package myGameEngine.NetworkHelpers;

import java.net.InetAddress;

public class ClientInfo {
    private InetAddress ip;
    private int port;

    public ClientInfo(InetAddress ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public InetAddress getIp() { return ip; }
    public int getPort() { return port; }

    public String info(){
        return ip + ":" + port;
    }

    @Override
    public String toString() { return this.info(); }
}
