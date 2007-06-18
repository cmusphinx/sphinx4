package edu.cmu.sphinx.frontend.util;

import javax.swing.*;
import java.awt.*;

/**
 * Copyright 1999-2006 Carnegie Mellon University. All Rights Reserved.  Use is subject to license terms.
 * <p/>
 * See the file "license.terms" for information on usage and redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * <p/>
 * User: Peter Wolf Date: Apr 30, 2006 Time: 9:10:44 AM
 */
public class VUMeterPanel extends JPanel {

    public void setVu(VUMeter vu) {
        this.vu = vu;
    }


    VUMeter vu = null;
    boolean quit = false;
    Thread thread = null;


    public void start() {
        quit = false;
        thread = new VUMeterPanelThread();
        thread.start();
    }


    public void stop() {
        quit = true;
        boolean hasQuit = false;
        while (!hasQuit) {
            try {
                thread.join();
                hasQuit = true;
            } catch (InterruptedException e) {
            }
        }
    }


    class VUMeterPanelThread extends Thread {

        public void run() {
            while (!quit) {
                repaint();  // probably this one should be replaced by a more appropriate method call in order to get rid of the annoying flickering
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {

                }
            }
        }
    }

    private int lastLevel = -1;


    /**
     * Paint the component.  This will be called by AWT/Swing.
     *
     * @param g The <code>Graphics</code> to draw on.
     */
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (vu != null) {

            System.out.println(lastLevel);

            if (vu.getIsClipping()) {
                paintClippingVUMeter(g);
            } else {
                paintVUMeter(g);
            }
        }
    }


    final int numberOfLights = 50;
    final int greenLevel = (int) (numberOfLights * 0.3);
    final int yellowLevel = (int) (numberOfLights * 0.7);
    final int redLevel = (int) (numberOfLights * 0.9);


    public VUMeter getVu() {
        return vu;
    }


    private void paintVUMeter(Graphics g) {

        int level = (int) ((vu.getRmsDB() / vu.getMaxDB()) * numberOfLights);
        int peak = (int) ((vu.getPeakDB() / vu.getMaxDB()) * numberOfLights);

        assert level >= 0;
        assert level < numberOfLights;

        if (level == lastLevel) return;
        lastLevel = level;

        Dimension sz = getSize();
        int w = sz.width;
        int h = (sz.height / numberOfLights);

        assert h > 2;
        assert w > 2;

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, sz.width - 1, sz.height - 1);

        for (int i = 0; i < level; i++) {
            setLevelColor(i, g);
            g.fillRect(1, sz.height - (i * h) + 1, w - 2, h - 2);
        }

        setLevelColor(peak, g);
        g.fillRect(1, sz.height - (peak * h) + 1, w - 2, h - 2);

    }


    private void setLevelColor(int i, Graphics g) {
        if (i < greenLevel)
            g.setColor(Color.BLUE);
        else if (i < yellowLevel)
            g.setColor(Color.GREEN);
        else if (i < redLevel)
            g.setColor(Color.YELLOW);
        else
            g.setColor(Color.RED);
    }


    private void paintClippingVUMeter(Graphics g) {

        lastLevel = -1;

        Dimension sz = getSize();
        int w = sz.width;
        int h = (sz.height / numberOfLights);

        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(0, 0, sz.width - 1, sz.height - 1);

        g.setColor(Color.RED);
        for (int i = 0; i < numberOfLights; i++) {
            g.fillRect(1, (i * h) + 1, w - 1, h - 2);
        }

    }
}

