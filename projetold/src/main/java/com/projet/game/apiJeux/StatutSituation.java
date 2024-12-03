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
package com.projet.game.apiJeux;

/**
 * Represente l'état courant d'une partie.
 * <p>
 * plusieurs états possibles :
 * <ul>
 * <li> la partie n'est pas terminée : statut = ENCOURS ; gagnant est empty <li>
 * <li> la partie est terminée ; statut = FINIE. Dans ce cas plusieurs cas
 * possibles :
 * <ul>
 * <li> un des joueurs à gagné : gagnant contient le joueur gagnant</li>
 * <li> c'est un match nul : gagnant est empty</li>
 * </ul>
 * </li>
 * </ul>
 *
 * @author francois
 */
public enum StatutSituation {
    ENCOURS,MATCH_NUL,J1_GAGNE,J2_GAGNE;
}
