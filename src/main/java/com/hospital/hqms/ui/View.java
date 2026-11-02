package com.hospital.hqms.ui;

import javafx.scene.Node;

/** A full screen shown in the main window. */
public interface View {

    Node getRoot();

    /** Called when the user navigates away - e.g. to stop auto-refresh timers. */
    default void onHide() {
    }
}
