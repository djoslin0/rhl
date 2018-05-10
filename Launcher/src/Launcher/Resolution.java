import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

public class Resolution implements Comparable<Resolution> {
    public static HashMap<String, Resolution> resolutions = null;
    public int width;
    public int height;
    public HashMap<Integer, HashSet<Integer>> depthRefresh = new HashMap<>();

    public Resolution(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public int compareTo(Resolution o) {
        return width > o.width ? -1 : 1;
    }

    public static void populateResolutions() {
        resolutions = new HashMap<>();
        GraphicsEnvironment g = GraphicsEnvironment.getLocalGraphicsEnvironment();
        DisplayMode[] modes = g.getDefaultScreenDevice().getDisplayModes();
        for (DisplayMode mode : modes) {
            Resolution resolution = null;
            String key = mode.getWidth() + " x " + mode.getHeight();
            if (resolutions.containsKey(key)) {
                resolution = resolutions.get(key);
            } else {
                resolution = new Resolution(mode.getWidth(), mode.getHeight());
                resolutions.put(key, resolution);
            }
            if (!resolution.depthRefresh.containsKey(mode.getBitDepth())) {
                resolution.depthRefresh.put(mode.getBitDepth(), new HashSet<>());
            }
            if (!resolution.depthRefresh.get(mode.getBitDepth()).contains(mode.getRefreshRate())) {
                resolution.depthRefresh.get(mode.getBitDepth()).add(mode.getRefreshRate());
            }
        }
    }

    public static Resolution get(String resolution) {
        if (resolutions == null) { populateResolutions(); }
        return resolutions.get(resolution);
    }


    public static String[] getList() {
        if (resolutions == null) { populateResolutions(); }
        ArrayList<Resolution> sortedResolutions = new ArrayList<>();
        for (Resolution resolution : resolutions.values()) {
            sortedResolutions.add(resolution);
        }
        Collections.sort(sortedResolutions);

        String[] res = new String[sortedResolutions.size()];
        for (int i = 0 ; i < sortedResolutions.size(); i++) {
            res[i] = sortedResolutions.get(i).width + " x " + sortedResolutions.get(i).height;
        }

        return res;
    }

}