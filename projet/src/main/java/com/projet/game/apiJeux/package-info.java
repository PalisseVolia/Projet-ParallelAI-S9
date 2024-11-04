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
/**
 * Modélisation des entités principales pour représenter un jeux à deux joueurs
 * où les joueurs jouent l'un après l'autre jusqu'à obtenir soit la victoire
 * de l'un des joueurs, soit un match nul.
 * <pre>
 * <p> Ce package fourni des briques de base pour entrainer des oracles :
 * Un oracle a pour but d'estimer la qualité d'une situation (état courant
 * d'un jeu).
 * </p>
 * <p> L'utilisation de l'oracle est le suivant (on se place ci-dessous du
 * point de vue de J1, l'utilisation pour le joueur J2 est symétrique) : 
 * <ul>
 *   <li> on détermine l'ensemble des coups possibles pour J1 dans la situation
 *        courante (note : le fait de passer, permis dans certains jeux dans
 *        certaines circonstances est considéré comme un coup possible)
 *   </li>
 *   <li> on determine la qualité de la situation obtenue pour chacun des coups
 *        possibles
 *   </li>
 *   <li> la situation évaluée est donc une situation où c'est à J2 de jouer </li>
 *   <li> le joueur J1 utilise donc un oracle qui évalue les situations de J2 </li>
 *   <li> J1 choisira le coup qui MINIMISE la valeur de la situation telle
 *        qu'évaluée par l'oracle qui évalue les situations de J2
 *   </li>
 * </ul>
 * <p> Note aux étudiants : cette convention : un oracle pour J1 est utilisé par J2 et inversement
 * a tendance à embrouiller le code. Si vous repartez de zéro, n'hésitez pas à en changer.
 * </p>
 * </pre>
 */
package com.projet.game.apiJeux;
