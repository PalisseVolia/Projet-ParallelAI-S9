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

import java.util.ArrayList;
import java.util.List;

/**
 * Quelques petits utilitaires en rapport avec la combinaison d'objets.
 *
 * @author francois
 */
public class Combinatoire {

    public static int factorielle(int n) {
        int res = 1;
        while (n > 0) {
            res = res * n;
            n--;
        }
        return res;
    }

    /**
     * calcule toutes les permutations possible d'une liste d'élements.
     * <pre> {@code
     * !!! genere n! listes pour une liste originale de n élements
     * applicable seulement sur de petites listes.
     * !!! Cette méthode n'est pas optimisée
     * } </pre>
     *
     * @param <E>
     * @param elems
     * @return
     */
    public static <E> List<List<E>> allPermutations(List<E> elems) {
        List<List<E>> collect = new ArrayList<>(factorielle(elems.size()));
        collectPerms(collect, new ArrayList<>(), elems);
        return collect;
    }

    private static <E> void collectPerms(List<List<E>> collect, List<E> done, List<E> toDo) {
        if (toDo.isEmpty()) {
            collect.add(done);
        } else {
            for (int i = 0; i < toDo.size(); i++) {
                E elem = toDo.get(i);
                List<E> ndone = new ArrayList<>(done);
                ndone.add(elem);
                List<E> ntoDo = new ArrayList<>(toDo.size() - 1);
                for (int j = 0; j < toDo.size(); j++) {
                    if (j != i) {
                        ntoDo.add(toDo.get(j));
                    }
                }
                collectPerms(collect, ndone, ntoDo);
            }
        }
    }

    public static void testPermutations() {
        int nbr = 4;
        List<Integer> test = new ArrayList<>(nbr);
        for (int i = 0; i < nbr; i++) {
            test.add(i);
        }
        List<List<Integer>> perms = allPermutations(test);
        System.out.println(perms);
        System.out.println(perms.size() +" permutations");
    }

    public static void main(String[] args) {
        testPermutations();
    }
}
