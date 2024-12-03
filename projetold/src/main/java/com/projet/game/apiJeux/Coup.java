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
 * Représentation d'un coup possible dans un jeux.
 * <p>
 * Cas classique sur un damier : contient la position joué sur le damier.
 * </p>
 * <p> 
 * Attention : doit inclure une valeur particulière pour le cas ou le joueur passe :
 * Certains jeux peuvent autoriser explicitement un joueur à passer (je n'ai pas
 * d'exemple). D'autres jeux n'autorise un joueur à passer que s'il n'a pas
 * de coup valide possible (exemple Othello)
 * <p>
 * Exemples : 
 * <ul>
 *   <li> Othello : un coup est une position sur le damier : ligne,col ou "passe"<li>
 *   <li> puissance 4 : un coup est un numéro de colonne (non pleine) <li>
 * </ul>
 * </p>
 * @author francois
 */
public interface Coup {
    /**
     * @return true iff le coup est "passe"
     */
    public boolean isPasse();
    
}
