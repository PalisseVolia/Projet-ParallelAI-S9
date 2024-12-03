/*
 Copyright 2000-2011 Francois de Bertrand de Beuvron

 This file is part of CoursBeuvron.

 CoursBeuvron is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 CoursBeuvron is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with CoursBeuvron.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.projet.game.othelloSimpleV2;


/**
 * Title: micro othello Description: Création d'un petit programme d'othello. Ce
 * programme respecte les règles de l'othello, mais joue la première case
 * jouable, sans aucune stratégie Copyright: Copyright (c) 2001 Company: INSA
 * Strasbourg
 *
 * @author F. de Beuvron
 * @version 1.0
 *
 * représente un déplacement d'une case sur un damier : on garde la paire
 * (deltaligne, deltacolonne)
 */
public class Direction {

    /** todoDoc. */
    private int dligne;

    /** todoDoc. */
    private int dcol;

    public Direction(int dligne, int dcol) {
        this.dligne = dligne;
        this.dcol = dcol;
    }

    /**
     *
     * 
     */
    public String toString() {
        return "[Direction (" + this.getDligne() + "," + this.getDcol() + ")]";
    }

    /**
     * un deplacement est valide si l'on se déplace effectivement : delta-ligne
     * et delta-colonne ne sont pas nuls tous les deux
     * 
     */
    public boolean estValide() {
        return getDligne() != 0 || getDcol() != 0;
    }

    /**
     * @return the dligne
     */
    public int getDligne() {
        return dligne;
    }

    /**
     * @return the dcol
     */
    public int getDcol() {
        return dcol;
    }
}
