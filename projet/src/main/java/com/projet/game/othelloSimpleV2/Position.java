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

import com.projet.utils.Console;
import com.projet.utils.matrice.MatriceToText;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Title: micro othello Description: Création d'un petit programme d'othello. Ce
 * programme respecte les règles de l'othello, mais joue la première case
 * jouable, sans aucune stratégie Copyright: Copyright (c) 2001 Company: INSA
 * Strasbourg
 *
 * @author F. de Beuvron
 * @version 1.0
 *
 * représentation d'une position sur un {@link Damier}
 */
public class Position {

    /**
     * @return the ligne
     */
    public int getLigne() {
        return ligne;
    }

    /**
     * @return the col
     */
    public int getCol() {
        return col;
    }

    /**
     * todoDoc.
     */
    private int ligne;

    /**
     * todoDoc.
     */
    private int col;

    public Position(int ligne, int col) {
        this.ligne = ligne;
        this.col = col;
    }

    /**
     *
     *
     */
    public String toString() {
        return "[Pos " + this.getLigne() + "," + this.getCol() + "]";
    }

    /**
     *
     *
     */
    public static Position entree() {
        char clig = 'A';
        char ccol = '0';
        boolean ok;
        do {
            ok = false;
            String rep = Console.entreeString("entrez une position (exemple B3 pour 2ième ligne, 3ième colonne) : ");
            rep = rep.trim().toUpperCase();
            if (rep.length() == 2) {
                clig = rep.charAt(0);
                if (clig >= 'A' && clig <= 'H') {
                    ccol = rep.charAt(1);
                    if (ccol >= '1' && ccol <= '8') {
                        ok = true;
                    }
                }
            }
            if (!ok) {
                Console.println("Position non reconnue");
            }
        } while (!ok);
        int lig = ((int) clig) - ((int) 'A');
        int col = ((int) ccol) - ((int) '1');
        return new Position(lig, col);
    }

    /**
     * une Position est valide si elle correspond effectivement à une case du
     * damier d'othello
     *
     */
    public boolean estValide() {
        return this.getLigne() >= 0 && this.getLigne() < 8 && this.getCol() >= 0 && this.getCol() < 8;
    }

    /**
     * pour tenir compte des symetries du damier en rotation.
     *
     * @param p
     * @return
     */
    public Position rotPiSurDeux() {
        return new Position(7 - this.col, this.ligne);
    }

    /**
     * pour tenir compte des symetries du damier en rotation.
     *
     * @param p
     * @return
     */
    public Position rotPi() {
        return this.rotPiSurDeux().rotPiSurDeux();
    }

    /**
     * pour tenir compte des symetries du damier en rotation.
     *
     * @param p
     * @return
     */
    public Position rotTroisPiSurDeux() {
        return this.rotPi().rotPiSurDeux();
    }

    /**
     * pour tenir compte des symetries du damier en symétrie horizontale.
     *
     * @param p
     * @return
     */
    public Position symetrieHorizontale() {
        return new Position(7 - this.ligne, this.col);
    }
    
    public static List<Function<Position, Position>> symetriesOthello() {
        List<Function<Position, Position>> noSymH = List.of(
                Function.identity(),
                Position::rotPiSurDeux,
                Position::rotPi,
                Position::rotTroisPiSurDeux);
        List<Function<Position, Position>> res = new ArrayList<>();
        Function<Position, Position> symetrie = Position::symetrieHorizontale;
        for (var f : noSymH) {
            res.add(f);
            res.add(symetrie.compose(f));
        }
        return res;
    }

    private static String[][] appliqueSymetrie(String[][] tab, Function<Position, Position> trans) {
        String[][] res = new String[tab.length][tab[0].length];
        for (int lig = 0; lig < tab.length; lig++) {
            for (int col = 0; col < tab[lig].length; col++) {
                Position cur = new Position(lig, col);
                Position tcur = trans.apply(cur);
                res[tcur.ligne][tcur.col] = tab[lig][col];
            }
        }
        return res;
    }

    private static void testRot() {
        int size = 8;
        String[][] matin = new String[size][size];
        int cur = 0;
        for (int lig = 0; lig < size; lig++) {
            for (int col = 0; col < size; col++) {
                matin[lig][col] = "" + cur;
                cur ++;
            }
        }
        int compteur = 0;
        for (var f : symetriesOthello()) {
            System.out.println("--------- sym " + compteur + " ---------\n");
            String[][] trans = appliqueSymetrie(matin, f);
            System.out.println(MatriceToText.formatMat(trans, false));
            compteur ++;
        }
    }

    public static void main(String[] args) {
        testRot();
    }

    /**
     */
    public Position bouge(Direction d) {
        return new Position(this.getLigne() + d.getDligne(), this.getCol() + d.getDcol());
    }
}
