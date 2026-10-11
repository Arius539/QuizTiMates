package org.hsw.display.navigation;

import org.hsw.application.error.ServiceException;

public interface IViewController {
    /**
     * Initialisiert den Zustand der geladenen Ansicht.
     *
     * @throws ServiceException wenn die Ansicht nicht initialisiert werden kann
     */
    void initilizeView() throws ServiceException;

    /**
     * Löst lokale Bindungen einer Ansicht, deren Laden fehlgeschlagen ist.
     * Der aufrufende Controller entscheidet über den Sitzungsabschluss.
     */
    default void discardView() {
    }
}
