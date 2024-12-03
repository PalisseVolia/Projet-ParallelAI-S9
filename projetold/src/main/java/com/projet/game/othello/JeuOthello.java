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

import com.projet.game.othelloSimpleV2.Damier;
import com.projet.game.othelloSimpleV2.Position;
import com.projet.game.apiJeux.Jeu;
import com.projet.game.apiJeux.Joueur;
import com.projet.game.apiJeux.StatutSituation;
import java.util.List;

/**
 * Une classe "proxy" pour donner les fonctionnalités d'un Jeu (au sens de l'api)
 * à une classe déjà existante {@link com.projet.game.othelloSimpleV2.Damier}
 * qui implémente effectivement le jeu d'Othello.
 * @author francois
 */
public class JeuOthello implements Jeu<SituationOthello, CoupOthello> {

    @Override
    public SituationOthello situationInitiale() {
        Damier res = new Damier();
        return new SituationOthello(res);
    }

   /**
     * La représentation d'un joueur n'est pas la même dans le jeux original
     * et dans l'api.
     * <p> une petite conversion s'impose</p>
     * <p> notez ici l'utilisation du nom complet de la classe : je ne peux
     * pas faire simplement un import puisque j'utilise deux classe qui 
     * ont exactement le même nom.
     * </p>
     * @param j un Joueur au sens de l'api apprentissage
     * @return un Joueur au sens du jeu original d'othello
     */
    private com.projet.game.othelloSimpleV2.Joueur convNewJoueurOldJoueur(Joueur j) {
        if (j == Joueur.J1) {
            return com.projet.game.othelloSimpleV2.Joueur.NOIR;
        } else {
            return com.projet.game.othelloSimpleV2.Joueur.BLANC;
        }

    }
    
    /**
     * La représentation d'un joueur n'est pas la même dans le jeux original
     * et dans l'api.
     * <p> une petite conversion s'impose</p>
     * <p> notez ici l'utilisation du nom complet de la classe : je ne peux
     * pas faire simplement un import puisque j'utilise deux classe qui 
     * ont exactement le même nom.
     * </p>
     * @param oldJ un Joueur au sens du jeu original d'othello
     * @return un Joueur au sens de l'api apprentissage 
     */
    @SuppressWarnings("unused")
    private Joueur convOldJoueurNewJoueur(com.projet.game.othelloSimpleV2.Joueur oldJ) {
        if (oldJ == com.projet.game.othelloSimpleV2.Joueur.NOIR) {
            return Joueur.J1;
        } else {
            return Joueur.J2;
        }
    }

    /**
     * Test toutes les cases pour déterminier les coups jouables.
     * @param s
     * @param j
     * @return 
     */
    @Override
    public List<CoupOthello> coupsJouables(SituationOthello s, Joueur j) {
        com.projet.game.othelloSimpleV2.Joueur jConv
                = convNewJoueurOldJoueur(j);
        List<Position> jouables = s.getDamierReel().coupsJouables(jConv);
        if (jouables.isEmpty()) {
            return List.of(CoupOthello.coupPasse());
        } else {
            return jouables.stream().map(CoupOthello::fromPos).toList();
        }
    }

    @Override
    public SituationOthello updateSituation(SituationOthello s, Joueur j, CoupOthello c) {
        if (c.isPasse()) {
            return s;
        } else {
            SituationOthello res = new SituationOthello(s.getDamierReel().copie());
            res.getDamierReel().effectueCoup(new Position(c.getLig(), c.getCol()), convNewJoueurOldJoueur(j));
            return res;
        }
    }

    @Override
    public StatutSituation statutSituation(SituationOthello s) {
        Damier d = s.getDamierReel();
        if (d.auMoinsUneCaseJouable(convNewJoueurOldJoueur(Joueur.J1)) 
                || d.auMoinsUneCaseJouable(convNewJoueurOldJoueur(Joueur.J2))) {
            return StatutSituation.ENCOURS;
        } else {
            int pionsNoirs = d.comptePions(convNewJoueurOldJoueur(Joueur.J1));
            int pionsBlancs = d.comptePions(convNewJoueurOldJoueur(Joueur.J2));
            if (pionsNoirs> pionsBlancs) {
                return StatutSituation.J1_GAGNE;
            } else if (pionsBlancs > pionsNoirs) {
                return StatutSituation.J2_GAGNE;
            } else {
                return StatutSituation.MATCH_NUL;
            }
        }
    }

}
