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

import com.projet.utils.Console;
import com.projet.utils.ConsoleFdB;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Title: micro othello Description: Création d'un petit programme d'othello. Ce
 * programme respecte les règles de l'othello, mais joue la première case
 * jouable, sans aucune stratégie
 *
 * @author F. de Beuvron
 * @version 1.0
 *
 */
public class Damier {

    /**
     * todoDoc.
     */
    private Case[][] cases;

    /**
     * crée un damier avec la position initiale.
     */
    public Damier() {
        this.cases = new Case[8][8];
        this.init();
    }

    /**
     * utilisé en interne pour la copie.
     *
     * @param cases
     */
    @SuppressWarnings("unused")
    private Damier(Case[][] cases) {
        this.cases = cases;
    }

    /**
     * initialise le damier contenant la position initiale d'une partie
     */
    public void init() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                this.cases[i][j] = Case.VIDE;
            }
        }
        this.cases[3][3] = Case.BLANC;
        this.cases[3][4] = Case.NOIR;
        this.cases[4][3] = Case.NOIR;
        this.cases[4][4] = Case.BLANC;
    }

    public Damier copie() {
        Case[][] dnew = new Case[this.cases.length][this.cases[0].length];
        for (int lig = 0; lig < this.cases.length; lig++) {
            for (int col = 0; col < this.cases[0].length; col++) {
                dnew[lig][col] = this.cases[lig][col];
            }
        }
        Damier res = new Damier();
        res.cases = dnew;
        return res;
    }

    public Case getVal(int lig, int col) {
        return this.cases[lig][col];
    }

    public Case getVal(Position p) {
        return this.getVal(p.getLigne(), p.getCol());
    }

    public String toString() {
        StringBuilder res = new StringBuilder();
        res.append("  |");
        for (int col = 0; col < 8; col++) {
            res.append(" " + (col + 1) + " |");
        }
        res.append("\n");
        res.append("==+");
        for (int col = 0; col < 8; col++) {
            res.append("===+");
        }
        res.append("\n");
        for (int lig = 0; lig < 8; lig++) {
            res.append((char) ((int) 'A' + lig));
            res.append(" |");
            for (int col = 0; col < 8; col++) {
                res.append(" " + this.cases[lig][col] + " |");
            }
            res.append("\n");
            if (lig != 7) {
                res.append("--+");
                for (int col = 0; col < 8; col++) {
                    res.append("---+");
                }
                res.append("\n");
            }
        }
        res.append("==+");
        for (int col = 0; col < 8; col++) {
            res.append("===+");
        }
        res.append("\n");
        return res.toString();
    }

    public String toStringBasic() {
        String res = " 12345678\n";
        for (int i = 0; i < 8; i++) {
            res = res + ((char) (((int) 'A') + i));
            for (int j = 0; j < 8; j++) {
                res = res + this.cases[i][j].toString();
            }
            res = res + "\n";
        }
        return res;
    }

    /**
     * todoDoc.
     */
    public static void test1() {
        Console.println(new Damier().toString());
    }

    /**
     * teste si une case est vide
     *
     *
     */
    public boolean estVide(Position p) {
        return this.cases[p.getLigne()][p.getCol()] == Case.VIDE;
    }

    /**
     * place un pion du joueur sur le damier
     *
     *
     */
    public void posePion(Position p, Joueur j) {
        this.cases[p.getLigne()][p.getCol()] = j.toCase();
    }

    /**
     * retourne un pion sur le damier
     *
     */
    public void retournePion(Position p) {
        if (this.cases[p.getLigne()][p.getCol()] == Case.NOIR) {
            this.cases[p.getLigne()][p.getCol()] = Case.BLANC;
        } else if (this.cases[p.getLigne()][p.getCol()] == Case.BLANC) {
            this.cases[p.getLigne()][p.getCol()] = Case.NOIR;
        } else {
            throw new Error("erreur interne : tentative de retourner un pion sur une case vide");
        }
    }

    /**
     * teste si une case contient un pion à la couleur du joueur
     *
     *
     *
     */
    public boolean estAuJoueur(Position p, Joueur j) {
        return this.cases[p.getLigne()][p.getCol()] == j.toCase();
    }

    /**
     * compte le nombre de pion d'un joueur
     *
     *
     */
    public int comptePions(Joueur j) {
        int res = 0;
        for (int lig = 0; lig < 8; lig++) {
            for (int col = 0; col < 8; col++) {
                Position p = new Position(lig, col);
                if (this.estAuJoueur(p, j)) {
                    res++;
                }
            }
        }
        return res;
    }

    /**
     * teste si un joueur retourne au moins un pion adverse dans une direction
     * donnée en jouant à une position donnée
     */
    public boolean jouableDirection(Position p, Direction d, Joueur j) {
        boolean res;
        Position posActuelle = p.bouge(d);
        int nbrAdverses = 0;
        while (posActuelle.estValide() && this.estAuJoueur(posActuelle, j.adversaire())) {
            posActuelle = posActuelle.bouge(d);
            nbrAdverses = nbrAdverses + 1;
        }
        if (nbrAdverses > 0 && posActuelle.estValide() && this.estAuJoueur(posActuelle, j)) {
            res = true;
        } else {
            res = false;
        }
        return res;
    }

    /**
     * teste si une position est jouable pour un joueur
     *
     *
     *
     */
    public boolean jouable(Position p, Joueur j) {
        boolean res = false;
        if (this.estVide(p)) {
            int dlig = -1;
            while (dlig <= 1 && !res) {
                int dcol = -1;
                while (dcol <= 1 && !res) {
                    Direction cur = new Direction(dlig, dcol);
                    if (cur.estValide()) {
                        res = this.jouableDirection(p, cur, j);
                    }
                    dcol = dcol + 1;
                }
                dlig = dlig + 1;
            }
        }
        return res;
    }

    //======================================================
    // quelques méthodes "évidentes", mais qui ne pouvaient pas 
    // être écrites en java 1.0
    public List<Position> coupsJouables(Joueur j) {
        List<Position> res = new ArrayList<>();
        for (int lig = 0; lig < 8; lig++) {
            for (int col = 0; col < 8; col++) {
                Position pos = new Position(lig, col);
                if (this.jouable(pos, j)) {
                    res.add(pos);
                }
            }
        }
        return res;
    }

    /**
     * Retourne la symétrie qui permet de retrouver l'élément typique de la
     * classe d'équivalence du damier par symétries.
     * <pre>
     * <p>
     * il y a 8 symétries qui laissent le damier inchangé à l'Othello (voir
     * {@link Position#symetriesOthello() } par rotation pi/2 ou symétrie horizontale/verticale.
     * </p>
     * <p> cela crée des classes d'équivalences sur l'ensemble des damiers </p>
     * <p> pour chaque classe d'équivalence, on veut avoir un damier typique
     * toujours le même de cette classe </p>
     * <p> Ce damier "typique" est le plus petit de la classe d'équivalence au
     * sens de l'ordre lexicographique lignes puis colonnes, sachant que le
     * contenu des cases (un enum) est implicitement ordonné avec {@code NOIR < BLANC < VIDE}
     * </p>
     * <p>
     * Note : On pourrait sans doute tester non pas sur toutes les positions mais
     * sur les classes d'équivalence de position par symétries. On risque donc
     * de faire un peu trop de "boulot" dans le cas ou le damier est symétrique.
     * </p>
     * </pre>
     *
     * @return la transformation qui produit le damier typique de la classe
     * d'équivalence
     */
    public Function<Position, Position> transPourDamierEquivalentParSymetrie() {
        var possibles = Position.symetriesOthello();
        boolean encore = true;
        int lig = 0;
        while (lig < 8 && encore) {
            int col = 0;
            while (col < 8 && encore) {
                Position cur = new Position(lig, col);
                // pour chaque position, je calcule le min des contenus par symetrie
                Case min = possibles.stream()
                        .map((sym) -> sym.apply(cur))
                        .map((p) -> this.getVal(p))
                        .min(Case::compareTo).get();
                // je ne garde que les symétries qui conduisent au min
                possibles = possibles.stream().filter(
                        (sym) -> this.getVal(sym.apply(cur)) == min).toList();
                col++;
            }
            lig++;
        }
        // s'il en reste plusieurs, c'est que le damier est totalement symétrique
        // pour toutes les transformation restante. Je peux donc prendre celle
        // que je veux. Je prend la première
        return possibles.get(0);
    }

    public Damier appliqueSymetrie(Function<Position, Position> trans) {
        Damier res = new Damier();
        for (int lig = 0; lig < 8; lig++) {
            for (int col = 0; col < 8; col++) {
                Position cur = new Position(lig, col);
                Position tcur = trans.apply(cur);
                res.cases[lig][col] = this.getVal(tcur);
            }
        }
        return res;
    }
    
    /**
     * Retourne l'élément typique de la classe d'équivalence du damier.
     * <pre>
     * <p> les damiers de l'othello sont équivalents par rotation pi/2 et par
     * symetrie. Lorsque l'on veut par exemple faire de l'apprentissage sur
     * la qualité des damier pour un joueur donné (est-ce que la situation
     * représentée par le damier est favorable ou défavorable au joueur), autant
     * utiliser toujours le même représentant de chaque classe d'équivalence.
     * </p>
     * </pre>
     * @return 
     */
    public Damier damierEquivalentTypique() {
        return appliqueSymetrie(transPourDamierEquivalentParSymetrie());
    }

    @SuppressWarnings("unused")
    private static void testEqClass() {
        boolean encore = true;
        while (encore) {
            Damier d = damierAlea();
            System.out.println("=========== damier origine ===========");
            System.out.println(d);
            System.out.println("=========== damier typique classe eq ===========");
            System.out.println(d.appliqueSymetrie(d.transPourDamierEquivalentParSymetrie()));
            encore = ConsoleFdB.entreeBooleanON("un autre (o/n) :");
        }
    }

    /**
     * renvoie un damier totalement aléatoire, qui ne correspond sans doute à
     * aucune situation possible du jeux.
     * <p>
     * simplement pour tester la détermination de la symétrie pour typique </p>
     *
     * @return
     */
    private static Damier damierAlea() {
        Damier res = new Damier();
        for (int lig = 0; lig < 8; lig++) {
            for (int col = 0; col < 8; col++) {
                res.cases[lig][col] = Case.caseAlea();
            }
        }
        return res;
    }

    public boolean isSituationFinale() {
        return this.coupsJouables(Joueur.NOIR).isEmpty() && this.coupsJouables(Joueur.BLANC).isEmpty();
    }

    //=====================================================
    // pas pour l'utilisation dans le jeu de base qui joue aléatoirement
    /**
     * tente de donner une approximation de la probabilité que le joueur j gagne
     * connaissant la position actuelle.
     * <pre>
     * <p> autrement dit, évalue la qualité de la position <p>
     * <p> on voudrait des valeurs entre 0 et 1 :
     * <ul>
     *   <li> 0 : le joueur est "sûr" de perdre </li>
     *   <li> 1 : le joueur est "sûr" de gagner </li>
     *   <li> 0.5 : la situation est totalement indécise </li>
     *   <li> 0.75 : le joueur estime que la situation lui est favorable
     *     il pense qu'il a 3 chances sur 4 de gagner
     *   </li>
     * </ul>
     * </p>
     * </pre>
     * <p>
     * on souhaite retourner une valeur entre 0 et 1 puisque c'est une
     * estimation de la probabilté de gagner</p>
     * <p>
     * Note : je ne suis pas du tout un joueur d'Othello : j'ai fait quelques
     * parties, mais pas plus. L'estimation implémentée dans cette méthode est
     * plus que sommaire.
     * </p>
     * </pre>
     *
     * @param j
     * @return {@code 0 <= x <= 1} estimation que le joueur j gagne la partie
     */
    public double evalueSituation(Joueur j) {
// TODO
        if (this.isSituationFinale()) {
            int nj1 = this.comptePions(j);
            int nj2 = this.comptePions(j.adversaire());
            if (nj1 > nj2) {
                return 1.0;
            } else if (nj2 > nj1) {
                return 0.0;
            } else {
                return 0.5;
            }
        } else {
            return 0.5;
        }
    }

    /**
     * modifie le damier suivant le coup de l'un des joueur
     *
     *
     */
    public void effectueCoup(Position p, Joueur j) {
        this.posePion(p, j);
        for (int dlig = -1; dlig <= 1; dlig++) {
            for (int dcol = -1; dcol <= 1; dcol++) {
                Direction d = new Direction(dlig, dcol);
                if (d.estValide() && this.jouableDirection(p, d, j)) {
                    Position posActuelle = p.bouge(d);
                    while (posActuelle.estValide() && this.estAuJoueur(posActuelle, j.adversaire())) {
                        this.retournePion(posActuelle);
                        posActuelle = posActuelle.bouge(d);
                    }
                }
            }
        }
    }

    /**
     * dans cette version, l'ordinateur joue aléatoirement une des cases
     * jouables.
     */
    public Position ordinateurJoue(Joueur j) {
        List<Position> jouables = this.coupsJouables(j);
        if (jouables.isEmpty()) {
            return new Position(-1, -1);
        } else {
            return jouables.get((int) (Math.random() * jouables.size()));
        }
    }

    /**
     * teste s'il existe au moins une case jouable pour le joueur
     *
     *
     */
    public boolean auMoinsUneCaseJouable(Joueur j) {
        boolean caseTrouvee = false;
//        Position p = new Position();
        int lig = 0;
        while (!caseTrouvee && lig < 8) {
            int col = 0;
            while (!caseTrouvee && col < 8) {
                Position p = new Position(lig, col);
                if (this.jouable(p, j)) {
                    caseTrouvee = true;
                } else {
                    col++;
                }
            }
            lig++;
        }
        return caseTrouvee;
    }

    /**
     * demande la case jouée par le joueur, vérifie qu'elle est valide et la
     * retourne retourne la position invalide (-1,-1) si le joueur doit passer
     *
     *
     */
    public Position humainJoue(Joueur j) {
        Position res = null;
        if (!this.auMoinsUneCaseJouable(j)) {
            Console.println("vous êtes obligés de passer");
            res = new Position(-1, -1);
        } else {
            boolean ok = false;
            while (!ok) {
                Console.println(j + " : où jouez-vous ?");
                res = Position.entree();
                ok = this.jouable(res, j);
                if (!ok) {
                    Console.println("coup non jouable");
                }
            }
        }
        return res;
    }

    /**
     * fait jouer un des joueurs, et modifie le damier renvoir false si le
     * joueur a du passer
     *
     *
     */
    public boolean joueUnCoup(Joueur j, boolean isOrdi) {
        boolean res;
        Position p;
        if (isOrdi) {
            p = this.ordinateurJoue(j);
        } else {
            p = this.humainJoue(j);
        }
        if (p.estValide()) {
            this.effectueCoup(p, j);
            Console.println(j + " joue en " + p);
            res = true;
        } else {
            Console.println(j + " passe");
            res = false;
        }
        return res;
    }

    /**
     * todoDoc.
     */
    public static void jouePartie() {
        Damier da = new Damier();
        Joueur jCourant = Joueur.NOIR;
        boolean[] ordis = new boolean[2];
        int curOrdi = 0;
        char rep;
        do {
            rep = Console.entreeChar("l'ordinateur joue les noirs (o/n) ?");
            rep = Character.toUpperCase(rep);
        } while (rep != 'O' && rep != 'N');
        ordis[0] = (rep == 'O');
        do {
            rep = Console.entreeChar("l'ordinateur joue les blancs (o/n) ?");
            rep = Character.toUpperCase(rep);
        } while (rep != 'O' && rep != 'N');
        ordis[1] = (rep == 'O');
        while (!da.isSituationFinale()) {
            Console.println(da.toString());
            da.joueUnCoup(jCourant, ordis[curOrdi]);
            jCourant = jCourant.adversaire();
            curOrdi = 1 - curOrdi;
        }
        Console.println(da.toString());
        int snoir = da.comptePions(Joueur.NOIR);
        int sblanc = da.comptePions(Joueur.BLANC);
        Console.println("score final : \n"
                + "joueur Noir : " + snoir + "\n"
                + "joueur Blanc : " + sblanc + "\n");
        if (snoir > sblanc) {
            System.out.println("Noir gagne");
        } else if (sblanc > snoir) {
            System.out.println("Blanc gagne");
        } else {
            System.out.println("Match nul");
        }
    }

    /**
     *
     *
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
//        testEqClass();
        jouePartie();
    }
}
