import java.io.*;
import java.util.HashMap;

public class Ini {
    public static HashMap<String, String> read(String fileName) {
        HashMap<String, String> map = new HashMap<>();
        try {
            File file = new File(fileName);
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            StringBuffer stringBuffer = new StringBuffer();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains("=")) {
                    String[] info = line.split("=", 2);
                    map.put(info[0], info[1]);
                }
            }
            fileReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }

    public static void write(String fileName, HashMap<String, String> map) {
        try {
            File file = new File(fileName);
            FileOutputStream outputStream = new FileOutputStream(file);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(outputStream));

            for (String key : map.keySet()) {
                bw.write(key + "=" + map.get(key));
                bw.newLine();
            }

            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
