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

import com.projet.utils.list.ListUtils;
import com.projet.utils.probas.TiragesAlea2;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Rassemble les informations permettant de jouer une partie.
 * <p>
 * dépend de la représentation des situations et des coups possibles </p>
 * <p>
 * Nous laissons la notion de situation la plus libre possible pour pouvoir
 * utiliser des classes déjà existantes qui gère des jeux particuliers :
 * </p>
 * <p>
 * Exemple : j'ai déjà une classe existante Damier pour le jeu d'Othello qui
 * sait déterminer les coups jouables, modifier le damier lorsqu'un joueur joue
 * à une position donné ...
 * </p>
 * <p>
 * Souvent les méthodes coupsJouables, updateSituation, et statutSituation ne
 * seront que des "proxy" qui appelle les méthodes correspondante dans la
 * représentation (Damier pour l'Othello) spécifique au jeu.
 * </p>
 *
 * @author francois
 */
public interface Jeu<Sit extends Situation, Co extends Coup> {

    public Sit situationInitiale();

    /**
     * Quels sont les coups possibles pour le joueur j connaissant la situation
     * s.
     * <p>
     * Rappel : passer est un coup particulier si cela est possible dans le jeu
     * </p>
     * <p>
     * si dans le jeu, le joueur est obligé de passer, cette fonction retournera
     * une liste singleton contenant uniquement le coup "passe" </p>
     *
     * @param s
     * @param j
     * @return
     */
    public List<Co> coupsJouables(Sit s, Joueur j);

    /**
     * Quelle est la nouvelle situation, après que le joueur j ait joué en c sur
     * la situation intiale s.
     * <p>
     * on suppose que c est jouable. ie c in coupsJouables(s,j)
     * <p>
     * <p>
     * attention : suivant le jeu, il faut prendre en compte le coup particulier
     * "passe" </p>
     *
     * @param s
     * @param j
     * @param c
     * @return
     */
    public Sit updateSituation(Sit s, Joueur j, Co c);

    /**
     * Teste si la situation correspond à une victoire d'un des joueurs, à un
     * match nul, ou à une partie en cours.
     *
     * @param s
     * @return
     */
    public StatutSituation statutSituation(Sit s);

    /**
     * Partie exécutée entre deux joueurs.
     * <pre>
     * <p> ATTENTION1 : rappelez vous que le joueur J1 utilise l'oracle o2 : le
     *     joueur J1 regarde ses coups possibles, et la situation obtenu s'il
     *     joue chacun de ses coups. Il évalue ensuite la situation obtenue
     *     du point de vue de J2, et pourra choisir le coup qui donne la
     *     situation la moins favorable pour J2.
     * </p>
     * <p> ATTENTION2 : bien que les oracles ne soient pas utilisés pour les
     * joueurs humain, ou dans le cas où le choix du coup est aléatoire
     * (ChoixCoup.ALEA), les oracles doivent tout de même être valides.
     * N'hesitez pas à utiliser la classe {@link OracleStupide} pour cela.
     * </p>
     * <p> nous denoterons un joueur par Ji (i = 1 ou 2) et son adversaire
     * par J-i. De même pour les oracles. Ji utilise l'oracle O-i.
     * </p>
     * <p>
     * Cette méthode est la méthode de base pour gérer une partie. Elle
     * possède de nombreux paramètres permettant de fixer le type de partie
     * en fonction de l'utilisation que l'on veut en faire :
     * <ul>
     *   <li> possibilité d'avoir un/des joueurs humains avec entrée des coups
     *        à la console.
     *   <li>
     *   <li> définir le choix du coup (voir {@link ChoixCoup}).
     * </ul>
     * Quelques utilisation typiques sont définies par des méthodes plus
     * spécifiques
     * </p>
     * </pre>
     *
     * @param o1 un Oracle pour guider le joueur J1 : doit évaluer la situation
     * après que J1 ait joué (c'est donc au tour de J2) : le
     *     joueur J1 regarde ses coups possibles, et la situation obtenu s'il
     *     joue chacun de ses coups. Il évalue ensuite la situation obtenue
     *     du point de vue de J2, et pourra choisir le coup qui donne la
     *     situation la moins favorable pour J2.
     * @param cc1 fixe comment est utilisé l'oracle o1. o1 peut être null si cc1
     * == ChoixCoup.ALEA
     * @param o2 un Oracle pour guider le joueur J1
     * @param cc2 fixe comment est utilisé l'oracle o2. o2 peut être null si cc2
     * == ChoixCoup.ALEA
     * @param j1Humain true iff J1 est joué par un humain
     * @param j2Humain true iff J2 est joué par un humain
     * @param rand random générator : permet par exemple d'avoir 2 fois
     * exactement la même partie même si les coups sont choisis au hazard (en
     * fournissant un random initialisé avec le même "seed")
     * @param trace if true, affiche les situations successives. Conseillé si au
     * moins un joueur est humain
     * @return
     */
    default public ResumeResultat<Co> partie(
            Oracle<Sit> o1, ChoixCoup cc1,
            Oracle<Sit> o2, ChoixCoup cc2,
            boolean j1Humain, boolean j2Humain,
            Random rand,
            boolean trace) {
        List<Co> res = new ArrayList<>();
        Sit curSit = this.situationInitiale();
        var joueurs = List.of(Joueur.J1, Joueur.J2);
        var oracles = List.of(o1, o2);
        var ccs = List.of(cc1, cc2);
        var humains = List.of(j1Humain, j2Humain);
        int numJoueur = 0;   // 0 pour J1, 1 pour J2
        while (this.statutSituation(curSit) == StatutSituation.ENCOURS) {
            if (trace) {
                System.out.println("----- Sit actuelle -------");
                System.out.println(curSit);
            }
            Oracle<Sit> curOracle = oracles.get(numJoueur);
            ChoixCoup curCC = ccs.get(numJoueur);
            Joueur curJoueur = joueurs.get(numJoueur);
            boolean humain = humains.get(numJoueur);
            List<Co> possibles = this.coupsJouables(curSit, curJoueur);
            Co coupChoisi;
            if (humain) {
                coupChoisi = ListUtils.selectOne("choisissez votre coup : ", possibles, Object::toString);
            } else {
                if (curCC == ChoixCoup.ALEA) {
                    // on ne tient aucun compte de l'oracle
                    // tirage aléatoire entre les coups possibles
                    int numCoup = rand.nextInt(possibles.size());
                    coupChoisi = possibles.get(numCoup);
                } else {
                    // je demande au stratege courant d'évaluer les coups jouables
                    // pour cela, je joue effectivement les coups, je calcule la
                    // nouvelle situation, et je demande à l'oracle ADVERSE de l'évaluer
                    List<Double> evals = new ArrayList<>();
                    for (Co c : possibles) {
                        Sit nouvelle = this.updateSituation(curSit, curJoueur, c);
                        evals.add(curOracle.evalSituation(nouvelle));
                    }
                    if (curCC == ChoixCoup.ORACLE_MEILLEUR) {
                        // on prend le coup correspondant au MIN des évaluation
                        // car on a fait faire l'évaluation à l'adversaire
                        // je prend donc le coup qui amène à la situation la
                        // moins favorable pour l'adversaire
                        int imin = 0;
                        double min = evals.get(imin);
                        for (int i = 1; i < evals.size(); i++) {
                            if (evals.get(i) < min) {
                                imin = i;
                                min = evals.get(imin);
                            }
                        }
                        coupChoisi = possibles.get(imin);
                    } else {
                        // curCC == ChoixCoup.ORACLE_PONDERE
                        coupChoisi = TiragesAlea2.choixAleaPondere(possibles, evals, rand);
                    }
                }
            }
            if (trace) {
                System.out.println("je joueur " + curJoueur + " joue en " + coupChoisi);
            }
            // je change la situation en fonction du coup réellement choisi
            curSit = this.updateSituation(curSit, curJoueur, coupChoisi);
            res.add(coupChoisi);
            // et je passe au joueur suivant sauf si passe
            numJoueur = 1 - numJoueur;
        }
        if (trace) {
            System.out.println("----- Sit finale -------");
            System.out.println(curSit);
            StatutSituation statut = this.statutSituation(curSit);
            if (statut == StatutSituation.MATCH_NUL) {
                System.out.println("Match nul");
            } else if (statut == StatutSituation.J1_GAGNE) {
                System.out.println("Gagnant : " + Joueur.J1);
            } else {
                System.out.println("Gagnant : " + Joueur.J2);
            }
        }
        return new ResumeResultat<>(this.statutSituation(curSit), res);
    }

    /**
     * Organise nbrParties entre un oracle et un adversaire qui joue aléatoirement.
     * <p> 
     * @param oracle
     * @param nbrParties
     * @param r
     * @return le nombre de victoire, et le nombre de match nuls
     */
    default public int[] partieVsAlea(Oracle<Sit> oracle, int nbrParties, Random r) {
        Joueur jOracle = oracle.getEvalueSituationApresCoupDe();
        Oracle<Sit> o1, o2;
        ChoixCoup cc1, cc2;
        if (jOracle == Joueur.J1) {
            o2 = new OracleStupide<Sit>(Joueur.J2);
            o1 = oracle;
            cc1 = ChoixCoup.ALEA;
            cc2 = ChoixCoup.ORACLE_MEILLEUR;
        } else {
            o2 = oracle;
            o1 = new OracleStupide<Sit>(Joueur.J1);
            cc1 = ChoixCoup.ORACLE_MEILLEUR;
            cc2 = ChoixCoup.ALEA;
        }
        int[] res = new int[2];
        for (int i = 0; i < nbrParties; i++) {
            ResumeResultat<Co> resOne = this.partie(o1, cc1, o2, cc2, false, false, r, false);
            if (resOne.getStatutFinal() == StatutSituation.MATCH_NUL) {
                res[1]++;
            } else if (resOne.getStatutFinal() == StatutSituation.J1_GAGNE && jOracle == Joueur.J1) {
                res[0]++;
            } else if (resOne.getStatutFinal() == StatutSituation.J2_GAGNE && jOracle == Joueur.J2) {
                res[0]++;
            }
        }
        return res;
    }

}
