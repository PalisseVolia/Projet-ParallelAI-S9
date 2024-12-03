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
 * Une petite classe abstraite pour gérer quel(s) joueur(s) sont associés à un
 * oracle.
 *
 * @author francois
 */
public abstract class AbstractOracle<Sit extends Situation> implements Oracle<Sit> {

    private List<Joueur> joueursCompatibles;
    private Joueur evaluePour;
    
    public AbstractOracle(Joueur seulementPour) {
        this(List.of(seulementPour),seulementPour);
    }

    public AbstractOracle(List<Joueur> joueursCompatibles,Joueur evaluePour) {
        if (! joueursCompatibles.contains(evaluePour)) {
            throw new Error("joueur " + evaluePour + " doit être dans la liste des compatibles");
        }
        this.evaluePour = evaluePour;
        this.joueursCompatibles = joueursCompatibles;       
    }

    @Override
    public List<Joueur> joueursCompatibles() {
        return this.joueursCompatibles;
    }

    @Override
    public Joueur getEvalueSituationApresCoupDe() {
        return this.evaluePour;
    }

    @Override
    public void setEvalueSituationApresCoupDe(Joueur j) {
        if (!this.joueursCompatibles.contains(j)) {
            throw new Error("Oracle " + this + " incompatible avec joueur " + j);
        }
        this.evaluePour = j;
    }

}
