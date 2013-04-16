package com.timepath.reddit;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author timepath
 */
public class TF2ScriptHelp {

    /**
     * For the /r/TF2ScriptHelp background
     *
     * @param s
     */
    static void screenshot(String s) {
        String[] strings = s.split("\n");

        HashMap<String, Integer> mp = new HashMap<String, Integer>();

        for(int i = 0; i < strings.length; i++) {
            if(!strings[i].contains("_")) {
                continue;
            }
            String prefix = strings[i].split("_")[0];
            if(strings[i].charAt(0) == '_') {
                prefix = "_";
            }
            if(!mp.containsKey(prefix)) {
                mp.put(prefix, 1);
//                System.out.println(prefix);
            } else {
                mp.put(prefix, mp.get(prefix) + 1);
            }
        }
        Iterator<Map.Entry<String, Integer>> it = mp.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry<String, Integer> pairs = it.next();
            LOG.log(Level.INFO, "{0} = {1}", new Object[]{pairs.getKey(), pairs.getValue()});
            it.remove(); // avoids a ConcurrentModificationException
        }

        int parts = 8;
        int len = strings.length;
        int bit = len / parts;
        int padding = 5;
        len = bit; // 3158

        Font font = new Font("Monospaced", Font.BOLD, 12);

        BufferedImage bi = new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);
        FontMetrics fm = bi.getGraphics().getFontMetrics(font);

        int width = 0;
        for(int i = 0; i < strings.length; i++) {
            int j = fm.stringWidth(strings[i]);
            if(j > width) {
                width = j;
            }
        }
        width += 2 * padding;
        int height = fm.getHeight() * len;
        height += padding;
//        int height = 47385;

        LOG.log(Level.INFO, "{0} x {1}", new Object[]{width, height});

        for(int r = 0; r < parts; r++) {


            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = img.createGraphics();
            g2d.setPaint(Color.BLACK);
//            g2d.fillRect(0, 0, width, height);
            g2d.setPaint(Color.GREEN.darker().darker());
            g2d.setFont(font);

            int y = fm.getHeight();
            Color cBack = new Color(39, 36, 34);
            Color cFront = new Color(57, 53, 50);
            Color cButtonText = new Color(236, 227, 203);
            Color cFiltered = new Color(160, 160, 160);
            Color c = cBack.brighter();
            for(int i = r * bit; i < r * bit + bit; i++) {
//                c = randomColor(i);
                g2d.setPaint(c);
                g2d.drawString(strings[i], padding, y + ((i - (r * bit)) * y));
                LOG.log(Level.INFO, "{0}/{1}", new Object[]{i + 1, len});
            }

            g2d.dispose();

            try {
                File scrot = new File("/home/timepath/Desktop/console" + r + ".png");
                ImageIO.write(img, "png", scrot);
            } catch(Exception e) {
                LOG.log(Level.SEVERE, "Exception", e);
            }
        }
        LOG.info("done");
    }

    Color randomColor(long r) {
        Random rnd = new Random(r);
        return new Color(rndMin(0.08f, rnd), rndMin(0.08f, rnd), rndMin(0.08f, rnd));
    }

    float rndMin(float min, Random r) {
        return Math.max(r.nextFloat() - min, min);
    }

    static void generateBackground() {
        File file = new File("/home/timepath/.local/share/Steam/SteamApps/timepath/Team Fortress 2/tf/pic.log");
        try {
            RandomAccessFile rf = new RandomAccessFile(file, "r");
            String str;
            StringBuilder sb = new StringBuilder();
            while((str = rf.readLine()) != null) {
                sb.append(str).append("\n");
            }

            screenshot(sb.toString());

        } catch(IOException ex) {
            Logger.getLogger(TF2ScriptHelp.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    static void generateBlockCodeNumbers() {
        StringBuilder sb = new StringBuilder();
        for(int i = 1; i < 1000; i++) {
            String num = "" + i + "";
            sb.append(num);
            if(num.length() < 3) {
                for(int j = 0; j < 3 - num.length(); j++) {
                    sb.append(" ");
                }
            } else {
                sb.append(" ");
            }
        }
        LOG.info(sb.toString());
    }

    /**
     * y = x % 360
     *
     * y = sin(x * pi / 180) / |sin(x * pi / 180)| * x mod 180
     * y = sgn(sin(x * pi / 180)) * x mod 180
     *
     * wrong, 180 is not quite the same as 0
     */
    static void viewmodelFovGraph() {
        double y;
        for(int x = -360; x < 360; x++) {
            y = Math.signum(Math.sin(x * Math.PI / 180)) * (x % 180);
            if(x < 0) {
                y *= -1;
            }
            LOG.log(Level.INFO, "{0}\t{1}", new Object[]{x, y});
        }
    }

    public static void main(String... args) throws FileNotFoundException {
//        generateBackground();
//        generateBlockCodeNumbers();
        viewmodelFovGraph();
    }

    private static final Logger LOG = Logger.getLogger(TF2ScriptHelp.class.getName());

}
