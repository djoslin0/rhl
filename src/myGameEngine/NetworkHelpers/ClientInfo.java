package myGameEngine.NetworkHelpers;

public class ClientInfo {
    private String ip;
    private String port;
    public ClientInfo(String ip,String port){
        this.ip = ip;
        this.port = port;
    }
    public String info(){
        return ip + " " + port;
    }
}
