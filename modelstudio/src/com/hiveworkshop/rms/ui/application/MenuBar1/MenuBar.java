package com.hiveworkshop.rms.ui.application.MenuBar1;

import com.hiveworkshop.rms.filesystem.GameDataFileSystem;
import com.hiveworkshop.rms.parsers.blp.BLPHandler;
import com.hiveworkshop.rms.parsers.slk.DataTable;
import com.hiveworkshop.rms.ui.application.MainLayoutCreator;
import com.hiveworkshop.rms.ui.application.MainPanel;
import com.hiveworkshop.rms.ui.application.MenuBarActions;
import com.hiveworkshop.rms.ui.browsers.jworldedit.WEString;
import com.hiveworkshop.rms.ui.browsers.jworldedit.objects.UnitEditorTree;
import com.hiveworkshop.rms.ui.browsers.jworldedit.objects.datamodel.MutableObjectData;
import com.hiveworkshop.rms.ui.browsers.model.ModelOptionPanel;
import com.hiveworkshop.rms.ui.browsers.mpq.MPQBrowser;
import com.hiveworkshop.rms.ui.browsers.unit.UnitOptionPanel;
import com.hiveworkshop.rms.ui.gui.modeledit.ModelPanel;
import com.hiveworkshop.rms.ui.preferences.SaveProfile;
import com.hiveworkshop.rms.ui.preferences.listeners.WarcraftDataSourceChangeListener;
import net.infonode.docking.DockingWindow;
import net.infonode.docking.View;

import javax.swing.*;
import java.awt.*;

public class MenuBar {
    static JMenuBar menuBar;
    static MainPanel mainPanel;

    static FileMenu fileMenu;
    static RecentMenu recentMenu;
    static EditMenu editMenu;
    static ToolsMenu toolsMenu;
    static ViewMenu viewMenu;
    static WindowsMenu windowMenu;
    static TeamColorMenu teamColorMenu;
    static AddMenu addMenu;
    static ScriptsMenu scriptsMenu;
    static AboutMenu aboutMenu;
    static WarcraftDataSourceChangeListener.WarcraftDataSourceChangeNotifier directoryChangeNotifier = new WarcraftDataSourceChangeListener.WarcraftDataSourceChangeNotifier();

    public MenuBar(MainPanel mainPanel) {
        this.mainPanel = mainPanel;
        menuBar = new JMenuBar();

        recentMenu = new RecentMenu(mainPanel);
        fileMenu = new FileMenu(mainPanel, recentMenu);
        editMenu = new EditMenu(mainPanel);
        toolsMenu = new ToolsMenu(mainPanel);
        toolsMenu.setEnabled(false);
        viewMenu = new ViewMenu(mainPanel);
        teamColorMenu = new TeamColorMenu(mainPanel);
        windowMenu = new WindowsMenu(mainPanel);
        addMenu = new AddMenu(mainPanel);
        scriptsMenu = new ScriptsMenu(mainPanel);
        aboutMenu = new AboutMenu();

        directoryChangeNotifier.subscribe(() -> {
            updateDataSource(mainPanel);
        });

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(toolsMenu);

        menuBar.add(viewMenu);
        menuBar.add(MenuBar.teamColorMenu);
        menuBar.add(MenuBar.windowMenu);
        menuBar.add(addMenu);
        menuBar.add(scriptsMenu);
        menuBar.add(aboutMenu);

        for (int i = 0; i < menuBar.getMenuCount(); i++) {
            menuBar.getMenu(i).getPopupMenu().setLightWeightPopupEnabled(false);
        }
        recentMenu.updateRecent();
    }

    public static JMenuBar getMenuBar() {
        return menuBar;
    }

    public static void updateRecent() {
        recentMenu.updateRecent();
    }

    public static boolean closeAll() {
        return fileMenu.closeAll(mainPanel);
    }

    public static void setToolsMenuEnabled(boolean enabled) {
        toolsMenu.setEnabled(enabled);
    }

    public static void removeModelPanel(ModelPanel modelPanel) {
        windowMenu.removeModelPanelItem(modelPanel);
    }

    public static void addModelPanel(ModelPanel modelPanel) {
        windowMenu.addModelPanelItem(modelPanel);
    }


    public static void updateDataSource(MainPanel mainPanel) {
        GameDataFileSystem.refresh(SaveProfile.get().getDataSources());
        // cache priority order...
        UnitOptionPanel.dropRaceCache();
        DataTable.dropCache();
        ModelOptionPanel.dropCache();
        WEString.dropCache();
        BLPHandler.get().dropCache();
        MenuBar.teamColorMenu.updateTeamColors();
        traverseAndReloadData(mainPanel.getRootWindow());
    }


    static void traverseAndReloadData(DockingWindow window) {
        int childWindowCount = window.getChildWindowCount();
        for (int i = 0; i < childWindowCount; i++) {
            DockingWindow childWindow = window.getChildWindow(i);
            traverseAndReloadData(childWindow);
            if (childWindow instanceof View) {
                View view = (View) childWindow;
                Component component = view.getComponent();
                if (component instanceof JScrollPane) {
                    JScrollPane pane = (JScrollPane) component;
                    Component viewportView = pane.getViewport().getView();
                    if (viewportView instanceof UnitEditorTree) {
                        UnitEditorTree unitEditorTree = (UnitEditorTree) viewportView;
                        MutableObjectData.WorldEditorDataType dataType = unitEditorTree.getDataType();
                        if (dataType == MutableObjectData.WorldEditorDataType.UNITS) {
                            System.out.println("saw unit tree");
                            unitEditorTree.setUnitDataAndReloadVerySlowly(MainLayoutCreator.getUnitData());
                        } else if (dataType == MutableObjectData.WorldEditorDataType.DOODADS) {
                            System.out.println("saw doodad tree");
                            unitEditorTree.setUnitDataAndReloadVerySlowly(MenuBarActions.getDoodadData());
                        }
                    }
                } else if (component instanceof MPQBrowser) {
                    System.out.println("saw mpq tree");
                    MPQBrowser comp = (MPQBrowser) component;
                    comp.refreshTree();
                }
            }
        }
    }
}