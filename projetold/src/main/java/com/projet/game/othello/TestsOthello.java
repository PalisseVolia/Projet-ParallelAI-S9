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

import com.projet.game.apiJeux.ChoixCoup;
import com.projet.game.apiJeux.Joueur;
import com.projet.game.apiJeux.OracleStupide;
import java.util.Random;

/**
 *
 * @author francois
 */
public class TestsOthello {

    public static void testStupid() {
        JeuOthello jt = new JeuOthello();
        jt.partie(new OracleStupide<SituationOthello>(Joueur.J1),ChoixCoup.ALEA,
                new OracleStupide<SituationOthello>(Joueur.J2),ChoixCoup.ALEA,
                false, false,new Random(), true);

    }

    public static void main(String[] args) {
        testStupid();
    }

}
