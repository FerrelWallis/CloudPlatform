#!/usr/bin/env python
# -*- coding: UTF-8 -*-


from collections import defaultdict


def count_terms(geneset, assoc, obo_dag):
    """count the number of terms in the study group
    """
    term_cnt = defaultdict(int)
    t2gs = dict()
    #print "#### times"
    #print geneset
    for gene in (g for g in geneset if g in assoc):
        #print "##\t" + gene
        for x in assoc[gene]:
            if x in obo_dag:
                #term_cnt[obo_dag[x].id] += 1
                #print "#" + obo_dag[x].id + "\t" + gene
                if t2gs.has_key(obo_dag[x].id):
                    if gene not in t2gs[obo_dag[x].id]:
                        t2gs[obo_dag[x].id].append(gene)
                        term_cnt[obo_dag[x].id] += 1
                else:
                    term_cnt[obo_dag[x].id] += 1
                    t2gs[obo_dag[x].id] = list()
                    t2gs[obo_dag[x].id].append(gene)
    return term_cnt, t2gs 

def is_ratio_different(min_ratio, study_go, study_n, pop_go, pop_n):
    """
    check if the ratio go /n is different between the study group and
    the population
    """
    if min_ratio is None:
        return True
    s = float(study_go) / study_n
    p = float(pop_go) / pop_n
    if s > p:
        return s / p > min_ratio
    return p / s > min_ratio
