/*
    Copyright 2000-2014 Francois de Bertrand de Beuvron

    This file is part of UtilsBeuvron.

    UtilsBeuvron is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    UtilsBeuvron is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with UtilsBeuvron.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.projet.utils.probas;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Quelques méthodes de tirage aléatoire sur des listes d'éléments.
 * <pre>
 * <p> on a une liste d'éléments, et une liste de poids (ceux-ci représentent
 * plus ou moins une probabilité relative que l'élément correspondant soit choisi).
 * </p>
 * <p>
 * On veut tirer aléatoirement un élément de la liste de valeur, ou p éléments
 * parmis les n éléments de la liste {@code p < n}
 * </p>
 * </pre>
 *
 * @author francois
 */
public class TiragesAlea2 {

    /**
     * renvoie un tableau contenant taille entiers distincts compris entre min
     * et max. Attention : à n'utiliser que si max-min n'est pas trop grand : on
     * va créer la liste de tous les entiers compris entre min et max.
     */
    public static List<Integer> ensembleAleaEntier(int taille, int min, int max, Random r) {
        if (max - min + 1 < taille) {
            throw new Error("pas assez d'entiers possibles");
        }
        // note : je crée une nouvelle ArrayList car la methode collect de stream ne garanti pas
        // que la liste renvoyée est modifiable (et donc shuffle pas forcement possible)
        List<Integer> possibles = new ArrayList<>(IntStream.rangeClosed(min, max).boxed().collect(Collectors.toList()));
        Collections.shuffle(possibles, r);
        return possibles.subList(0, taille);
    }

    public static int oneIntFromTab(int[] tab, Random r) {
        return tab[r.nextInt(tab.length)];
    }

    public static LocalDate dateAleaBetween(LocalDate min, LocalDate max, Random r) {
        long minDay = min.toEpochDay();
        long maxDay = max.toEpochDay();
        long delta = (long) (r.nextDouble() * (maxDay - minDay + 1));
        return min.plusDays(delta);
    }

    /**
     * partitionne aléatoirement une liste d'objets en n = tailles.length
     * sous-liste disjointes.
     * <pre> {@code
     *   la sous-liste N° i est de taille tailles[i]  0 <= i < tailles.length
     * il faut donc bien sur que :
     *     somme(i=0; i < tailles.length ; tailles[i]) == objects.size()
     * } </pre>
     */
    public static <E> List<List<E>> partitionAlea(Collection<E> objects, int[] tailles, Random r) {
        List<E> copie = new ArrayList<>(objects);
        Collections.shuffle(copie, r);
        List<List<E>> res = new ArrayList<>(tailles.length);
        int cur = 0;
        for (int i = 0; i < tailles.length; i++) {
            if (cur + tailles[i] > copie.size()) {
                throw new Error("not enough objects");
            }
            res.add(copie.subList(cur, cur + tailles[i]));
            cur = cur + tailles[i];
        }
        return res;
    }

    /**
     * construit une liste de taille doubles compris entre min et max.
     *
     * @param taille
     * @param min
     * @param max
     * @return
     */
    public static List<Double> valsAleaBetween(int taille, double min, double max, Random r) {
        return r.doubles().limit(taille).map((x) -> min + (max-min)*x).boxed().collect(Collectors.toList());
    }

    public static <V> V choixAlea(List<V> valeurs, Random r) {
        return valeurs.get(r.nextInt(valeurs.size()));
    }

    public static <V> List<V> choixAleaMultiple(List<V> valeurs, int nbr, Random r) {
        ArrayList<V> copie = new ArrayList<>(valeurs);
        Collections.shuffle(copie, r);
        return copie.subList(0, nbr);
    }

    /**
     * choisi aléatoirement une valeur vi en considérant que poidsi/somme(poids)
     * (en ne considérant que les poids positifs) est la proba que vi soit
     * choisi.
     * <pre>
     * <p> On remplace les poids négatifs par zéro </p>
     * <p> cas particulier : si somme(poids) == 0 (tous les poids étaient négatifs
     * ou nuls) on tire aléatoirement sans pondération.
     * </pre>
     *
     * @param choix
     * @return i l'indice de l'élément choisi dans la liste
     */
    public static <V> int choixIndicePondere(List<V> valeurs, List<Double> poids, Random r) {
        if(valeurs.size() != poids.size()) {
            throw new Error("valeurs et poids doivent être de même taille");
        }
        List<Double> noNeg = poids.stream().map((x) -> x < 0 ? 0.0 : x).toList();
        double tot = noNeg.stream().reduce(0.0, (a, b) -> a + b);
        if (tot == 0.0) {
            return r.nextInt(valeurs.size());
        } else {
            int res = 0;
            double alea = r.nextDouble() * tot;
            double sum = noNeg.get(0);
            while (res < valeurs.size() - 1 && alea > sum) {
                res++;
                sum = sum + noNeg.get(res);
            }
            return res;
        }
    }

    /**
     * choisi aléatoirement une valeur vi en considérant que poidsi/somme(poids)
     * (en ne considérant que les poids positifs) est la proba que vi soit
     * choisi.
     * <pre>
     * <p> On remplace les poids négatifs par zéro </p>
     * <p> cas particulier : si somme(poids) == 0 (tous les poids étaient négatifs
     * ou nuls) on tire aléatoirement sans pondération.
     * </pre>
     *
     * @param choix
     * @return vi l'élément choisi
     */
    public static <V> V choixAleaPondere(List<V> valeurs, List<Double> poids, Random r) {
        return valeurs.get(choixIndicePondere(valeurs, poids, r));
    }

    private static void testAleaPondere(int nbrTirages, int nbrVals, double minPoids, double maxPoids) {
        Random r = new Random();
        List<Integer> lesVals = IntStream.range(0, nbrVals).boxed().toList();
        List<Double> poidsAvecNegs = valsAleaBetween(lesVals.size(), minPoids, maxPoids, r);
        System.out.println("poids : " + poidsAvecNegs);
        List<Double> poids = poidsAvecNegs.stream().map((x) -> x < 0 ? 0.0 : x).toList();
        System.out.println("poids sans negs : " + poids);
        double tot = poids.stream().map((x) -> x < 0 ? 0.0 : x).reduce(0.0, (a, b) -> a + b);
        int[] expected = new int[nbrVals];
        for (int i = 0; i < nbrVals; i++) {
            expected[i] = (int) (poids.get(i) / tot * nbrTirages);
        }
        System.out.println("average expected : " + Arrays.toString(expected));
        int[] tirages = new int[nbrVals];
        for (int i = 0; i < nbrTirages; i++) {
            tirages[choixAleaPondere(lesVals,poids, r)]++;
        }
        System.out.println("         tirages : " + Arrays.toString(tirages));
        int[] diffs = new int[nbrVals];
        for (int i = 0; i < nbrVals; i++) {
            diffs[i] = tirages[i] - expected[i];
        }
        System.out.println("           diffs : " + Arrays.toString(diffs));
        double[] diffsP = new double[nbrVals];
        for (int i = 0; i < nbrVals; i++) {
            diffsP[i] = (double) diffs[i] / expected[i] * 100;
        }
        System.out.println("       diffs (%) : " + Arrays.toString(diffsP));
    }

    /**
     * même chose que {@link #choixAleaPondere(java.util.List, java.util.Random)
     * } mais on en choisi nbrChoix et non plus un seul.
     */
    public static <V> List<V> choixMultiplesAleaPondere(int nbrChoix, List<V> valeurs,List<Double> poids, Random r) {
        if(valeurs.size() != poids.size()) {
            throw new Error("valeurs et poids doivent être de même taille");
        }
        if (nbrChoix > valeurs.size()) {
            throw new Error("Pas assez de valeurs !!");
        }
        // copiée puisque modifiée
        List<V> curVals = new ArrayList<>(valeurs);
        List<Double> curPoids = new ArrayList<>(poids);
        List<V> res = new ArrayList<>(nbrChoix);
        for (int i = 0; i < nbrChoix; i++) {
            int unChoix = choixIndicePondere(curVals, curPoids, r);
            res.add(curVals.get(unChoix));
            curVals.remove(unChoix);
            curPoids.remove(unChoix);
        }
        return res;
    }

    private static void testChoixMultiplesAleaPondere(int nbrTirages, int nbrVals, int nbrChoix,double minPoids, double maxPoids) {
        Random r = new Random();
        List<Integer> lesVals = IntStream.range(0, nbrVals).boxed().collect(Collectors.toList());
        List<Double> poidsAvecNegs = valsAleaBetween(lesVals.size(), minPoids, maxPoids, r);
        System.out.println("poids : " + poidsAvecNegs);
        List<Double> poids = poidsAvecNegs.stream().map((x) -> x < 0 ? 0.0 : x).toList();
        System.out.println("poids sans negs : " + poids);
        double tot = poids.stream().reduce(0.0, (a, b) -> a + b);
        int[] expected = new int[nbrVals];
        for (int i = 0; i < nbrVals; i++) {
            expected[i] = (int) (poids.get(i) / tot * (nbrTirages * nbrChoix));
        }
        System.out.println("average expected : " + Arrays.toString(expected));
        int[] rep = new int[nbrVals];
        for (int i = 0; i < nbrTirages; i++) {
            List<Integer> choix = choixMultiplesAleaPondere(nbrChoix, lesVals,poids, r);
            for (int c : choix) {
                rep[c]++;
            }
//            System.out.println(Arrays.toString(choix));
        }
        System.out.println("         tirages : " + Arrays.toString(rep));
        int[] diffs = new int[nbrVals];
        for (int i = 0; i < nbrVals; i++) {
            diffs[i] = rep[i] - expected[i];
        }
        System.out.println("           diffs : " + Arrays.toString(diffs));
        double[] diffsP = new double[nbrVals];
        for (int i = 0; i < nbrVals; i++) {
            diffsP[i] = (double) diffs[i] / expected[i] * 100;
        }
        System.out.println("       diffs (%) : " + Arrays.toString(diffsP));
    }

    public static void main(String[] args) {
        System.out.println("-------------- Alea Pondere");
        testAleaPondere(1000000, 10, 0.0, 1.0);
        System.out.println("-------------- Alea multiple pondere");
        testChoixMultiplesAleaPondere(1000000, 10,3, 0.0, 1.0);
    }

}
