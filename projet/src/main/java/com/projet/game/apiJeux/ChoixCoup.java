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
 *
 * @author francois
 */
public enum ChoixCoup {
    
    /**
     * le coup est choisi au hasard parmis les cooups possibles.
     * <p> L'oracle est donc ignoré </p>
     */
    ALEA,
    /**
     * On prend le coup qui minimise la valeur de la position pour le joueur
     * adverse après application du coup.
     * <pre>
     * Supposons que c'est au joueur J1 de jouer (sinon inverser J1/J2)
     * <ul>
     *   <li> on determine les coups possibles pour J1 dans la situation courante {@code =>} coups </li>
     *   <li> pour chacun des coups possibles 
     *   <ul>
     *     <li> on joue le coup <li>
     *     <li> ce serait maintenant à J2 de joueur </li>
     *     <li> on demande donc à l'oracle J2 d'évaluer la situation </li>
     *   </ul>
     *   <li> {@code =>} on obtient donc un évaluation (du point de vue de J2)
     *   pour chacun des coups possibles de J1
     *   </li>
     *   <li> on choisi donc le coup qui minimise l'évaluation par J2 </li>
     *   <li> dit autrement : J1 joue pour que après son coup, J2 se trouve dans
     *   la situation qu'il juge la plus défavorable
     *   </li>
     * </ul>
     * </pre>
     */
    ORACLE_MEILLEUR,
    /**
     * On pondère chacun des coup jouables par (1- l'évaluation par le joueur
     * J2 de la position après que J1 ai joué le coup), puis on fait un
     * tirage aléatoire pondéré.
     * <pre>
     * Supposons que c'est au joueur J1 de jouer (sinon inverser J1/J2)
     * <ul>
     *   <li> on determine les coups possibles pour J1 dans la situation courante {@code =>} coups </li>
     *   <li> pour chacun des coups possibles 
     *   <ul>
     *     <li> on joue le coup <li>
     *     <li> ce serait maintenant à J2 de joueur </li>
     *     <li> on demande donc à l'oracle J2 d'évaluer la situation </li>
     *   </ul>
     *   <li> {@code =>} on obtient donc un évaluation (du point de vue de J2)
     *   pour chacun des coups possibles de J1 {@code => evalsParJ2}
     *   </li>
     *   <li> comme les valuations sont normalement entre 0 et 1 (proba de gagner)
     *   et que plus la situation est favorable du point de vue de J2 plus elle
     *   est défavorable du point de vue de J1, on prend en fait (1 - evalParJ2)
     *   comme pondération de chaque coup.
     *   </li>
     * </ul>
     * @see fr.insa.beuvron.utils.probas.TiragesAlea2 pour détails sur tirage pondéré
     * </pre>
     */
    ORACLE_PONDERE
    
}
