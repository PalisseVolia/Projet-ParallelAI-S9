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
 * deux possibilités de joueurs : noir ou blanc. Comme pour {@link Case}, on
 * choisi de coder la couleur du joeur par un entier (même code que {@link Case}
 * sans la possibilité de VIDE)
 */
public enum Joueur {
    NOIR,BLANC;


    public Case toCase() {
        if (this == NOIR) {
            return Case.NOIR;
        } else {
            return Case.BLANC;
        }
    }
    
    public Joueur adversaire() {
        if (this == NOIR) {
            return BLANC;
        } else {
            return NOIR;
        }
    }

}
