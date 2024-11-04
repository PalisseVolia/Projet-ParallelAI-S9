/*
Copyright 2000- Francois de Bertrand de Beuvron

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
package com.projet.game.othello;

import com.projet.game.othelloSimpleV2.Case;
import com.projet.game.othelloSimpleV2.Damier;
import com.projet.game.apiJeux.Situation;

/**
 * Un "proxy" de la classe Damier existante qui implementait le jeu d'othello.
 * @author francois
 */
public class SituationOthello implements Situation {
    
    private Damier damierReel;

    public SituationOthello(Damier damierReel) {
        this.damierReel = damierReel;
    }

    public Damier getDamierReel() {
        return damierReel;
    }
    
    @Override
    public String toCSV() {
        StringBuilder res = new StringBuilder();
        for (int lig = 0 ; lig < 8 ; lig ++) {
            for (int col = 0 ; col < 8 ; col ++) {
                Case cur = this.damierReel.getVal(lig, col);
                int val;
                if (cur ==  Case.VIDE) {
                    val = 0;
                } else if (cur == Case.NOIR) {
                    val = 1;
                } else {
                    val = -1;
                }
                res.append(val);
                if (lig != 7 || col != 7) {
                    res.append(",");
                }
            }
        }
        return res.toString();
    }

    @Override
    public String toString() {
        return damierReel.toString();
    }
    
    
    
}
