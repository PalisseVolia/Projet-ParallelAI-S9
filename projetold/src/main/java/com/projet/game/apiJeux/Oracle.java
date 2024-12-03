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

import java.util.List;

/**
 * Le stratège doit donner une évaluation d'une Situation pour un Joueur.
 * <p> Nous choisissons une évaluation numérique avec le sens "intuitif" :
 * connaissant une situation s et un joueur j, quelle est la probabilité 
 * que s aboutisse à la victoire de j</p>
 * <p> on souhaite donc que la valeur retournée v soit contenue dans l'intevalle [0,1] :
 * <ul>
 * <li> si s est une situation ou j a gagné (situation finale gagnante), on devrait avoir
 *      eval(s,j) = 1 </li>
 * <li> symétriquement, si s est une situation ou j a perdu (situation finale perdante), on devrait avoir
 *      eval(s,j) = 0 </li>
 * <li> une évaluation e = 0.5 correspond à une situation totalement indécise </li>
 * <li> une évaluation {@code 0.5 < e <= 1} correspond à une situation favorable pour le joueur j </li>
 * </ul>
 * <p>
 * <p> Mais cette probabilité va être estimée (dans notre cas par apprentissage
 * sur réseaux de neuronnes). Nous relachons donc les contraintes liées à
 * une vraie probabilité :
 * <ul>
 * <li> normalement, si s est une situation ou j a gagné on devrait avoir {@code evalSItuation(s,j) = 1}.
 * Ce n'est pas imposé. </li>
 * <li> De façon symétrique, si s est une situation ou j a perdu on devrait avoir {@code evalSItuation(s,j) = 0}.
 * Là encore, ce n'est pas imposé. </li>
 * </ul>
 * </p>
 * <p> Note : on pourrait penser que l'Oracle dépend du jeux. Et bien sur
 * un BON Oracle dépend du jeux. Mais supposons que deux jeux aient une
 * même représentation d'une situation. Exemple : l'othello et les dames
 * anglaises se jouent tous les deux sur un damier 8x8 avec des cases vides,
 * des cases contenant un pion noir, et des cases contenant un pion blanc.
 * Une représentation "Brutale" et commune à ces deux jeux est simplement
 * le contenu de chacune des cases du damier. On peut noter que cette 
 * représentation n'est pas optimale pour les dames anglaise puisque dans ce
 * jeux, le damier comporte des cases noires et des cases blanches mais que les
 * pions ne peuvent être placés que dans les cases noires. Mais ce n'est pas
 * ici notre propos : Ces deux jeux peuvent avoir la même représentation 
 * d'une situation. Dans ce cas, un Oracle de l'othello peut être utilisé
 * dans le jeu des dames anglaise. Le "!!petit!!" problème, c'est que si cet 
 * Oracle a été entrainé sur le jeu d'othello, et qu'on l'utilise pour le
 * jeu de dame anglaise, il va produire une évaluation, OK du point de vue
 * de la structure d'appel du programme, mais cette évaluation sera totalement
 * inadaptée.
 * </p>
 * @author francois
 */
public interface Oracle<Sit extends Situation> {
    
    /**
     * renvoie une estimation de la probabilité de gagner connaissant une situation.
     * @param s
     * @return {@code 0 <= eval <=1}
     */
    public double evalSituation(Sit s);
    
    
    /**
     * Retourne les joueurs compatibles avec l'oracle.
     * <pre>
     * <p> Un oracle peut être compatible avec le joueur J1, le joueur J2, ou
     * les deux.
     * </p>
     * <p> 
     * Dans certains jeux, les situations peuvent être les mêmes que ce soit au
     * joueur J1 ou au joueur J2 de jouer. Dans ce cas il est normal d'avoir un
     * oracle qui sait aussi bien évaluer les situations quand c'est à J1 de jouer
     * que les situations où c'est à J2 de jouer. Dans ce cas, il serait normal
     * (mais non requis) que pour une situation s, 
     * evalSituation(s) du point de vue de J1 = (1 - evalSituation(s)) du point de vue de J2
     * </p>
     * <p> dans d'autres jeux les situations que rencontrent les joueurs J1 et J2 sont
     * toujours différentes : par exemple dans le jeu puissance 4, le joueur J1 voit 
     * toujours des situations avec un nombre pair de pions sur le damier, alors que
     * le joueur J2 voit toujours des damiers avec un nombre impair de pions. Dans
     * ce cas il semble plus efficace d'entrainer séparément des oracles pour
     * J1 et des oracles pour J2.
     * </pre>
     * @return 
     */
    public List<Joueur> joueursCompatibles();
    
    /**
     * L'oracle évalue les situations du point de vue de ce joueur.
     * <pre>
     * <p> Les classes implémentant Oracle doivent s'assurer que 
 getEvalueSituationApresCoupDe toujours inclu dans joueursCompatibles
 </p>
     * </pre>
     * @return 
     */
    public Joueur getEvalueSituationApresCoupDe();
    
    /**
     * fixe le joueur du point de vue duquel l'oracle doit évaluer les situations.
     * @param j 
     */
    public void setEvalueSituationApresCoupDe(Joueur j);
    
}
