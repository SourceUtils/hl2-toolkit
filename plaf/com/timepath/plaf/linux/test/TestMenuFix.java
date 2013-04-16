package com.timepath.plaf.linux.test;

import com.timepath.plaf.linux.GtkFixer;
import java.awt.Dimension;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

@SuppressWarnings("serial")
public class TestMenuFix extends JFrame {

    public static void main(String[] args) {
        GtkFixer.installGtkPopupBugWorkaround();

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                TestMenuFix test = new TestMenuFix();
                test.setDefaultCloseOperation(EXIT_ON_CLOSE);
                test.setPreferredSize(new Dimension(400, 300));
                test.pack();
                test.setLocationRelativeTo(null);

                JMenuBar menuBar = new JMenuBar();
                JMenu menu1 = new JMenu("Menu 1");
                menu1.add(new JMenuItem("Item 1.1"));
                JMenuItem t = new JMenuItem("Item 1.2");
                t.setEnabled(false);
                menu1.add(t);
                menu1.add(new JMenuItem("Item 1.3"));
                menuBar.add(menu1);
                JMenu menu2 = new JMenu("Menu 2");
                menu2.add(new JMenuItem("Item 2.1"));
                menu2.add(new JMenuItem("Item 2.2"));
                menu2.add(new JMenuItem("Item 2.3"));
                menuBar.add(menu2);
                test.setJMenuBar(menuBar);

                test.setVisible(true);
            }
        });
    }

    private static final Logger LOG = Logger.getLogger(TestMenuFix.class.getName());
}