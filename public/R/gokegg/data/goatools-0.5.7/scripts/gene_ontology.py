#!/usr/bin/env python
# -*- coding: UTF-8 -*-

from __future__ import division
import sys
import os
import os.path as op
sys.path.insert(0, op.join(op.dirname(__file__), ".."))
from goatools.obo_parser import GODag
from collections import defaultdict


def read_data(assoc_fn):
    assoc = {}
    genes_set = set()
    for row in open(assoc_fn):
        atoms = row.split()
        if len(atoms) == 2:
            a, b = atoms
        elif len(atoms) > 2 and row.count('\t') == 1:
            a, b = row.split("\t")
        else:
            continue
        b = set(b.split(";"))
        assoc[a] = b
        genes_set.add(a)

    return assoc, genes_set

if __name__ == "__main__":

    import argparse
    p = argparse.ArgumentParser(__doc__,
                 formatter_class=argparse.ArgumentDefaultsHelpFormatter)

    p.add_argument('--level', default='1', type=str, dest='level',
                 help="GO level, shortest distance from root node, 0,1,2,3,4,5,6,7,8,9,a")

    p.add_argument('--input',  type=str, dest='input', required=True,
                 help="GO annotaion file")

    p.add_argument('--output',  type=str, dest='output', required=True,
                 help="output file")

    args = p.parse_args()
    sys.stdout = open(args.output, 'w')

    assoc, geneset = read_data(args.input)
    lv = args.level

    script_path = os.path.normpath( os.path.join( os.getcwd(), os.path.dirname(__file__) ) )
    obo_file_path = os.path.join( script_path, "go-basic.obo" )
    obo_dag = GODag(obo_file=obo_file_path)
    obo_dag.update_association(assoc)

    #print obo_dag

    term_cnt = defaultdict(int)
    t2gs = dict()
    for gene in (g for g in geneset if g in assoc):
        for x in assoc[gene]:
            if x in obo_dag:
                #term_cnt[obo_dag[x].id] += 1
                if t2gs.has_key(obo_dag[x].id):
                    if gene not in t2gs[obo_dag[x].id]:
                        t2gs[obo_dag[x].id].append(gene)
                        term_cnt[obo_dag[x].id] += 1
                else:
                    t2gs[obo_dag[x].id] = list()
                    t2gs[obo_dag[x].id].append(gene)
                    term_cnt[obo_dag[x].id] += 1

    gene_num = len( geneset )

    print "##total input seqs number with gos: %d" % gene_num
    print "namespace\tdescription\tnumber\tpercent\tid\tgenes"

    if lv == "a":
        for x in term_cnt.keys():
            term = obo_dag[x]
            percent = term_cnt[x] / gene_num
            genes = ";".join(t2gs[term.id])
            if not term.is_obsolete:
                print "%s\t%s\t%d\t%f\t%s\t%s" % ( term.namespace, term.name, term_cnt[x], percent, term.id,  genes )
    else:
        for x in term_cnt.keys():
            term = obo_dag[x]
            percent = term_cnt[x] / gene_num
            genes = ";".join(t2gs[term.id])
            if lv == "%d" % term.level and not term.is_obsolete:
                print "%s\t%s\t%d\t%f\t%s\t%s" % ( term.namespace, term.name, term_cnt[x], percent, term.id,  genes )















