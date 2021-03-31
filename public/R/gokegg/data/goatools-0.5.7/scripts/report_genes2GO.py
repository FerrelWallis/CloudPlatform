#!/usr/bin/env python
# -*- coding: UTF-8 -*-

from __future__ import division
import sys
import os
import os.path as op
sys.path.insert(0, op.join(op.dirname(__file__), ".."))
from goatools.obo_parser import GODag
from collections import defaultdict


def read_geneset(study_fn):
    gs = set(_.strip() for _ in open(study_fn) if _.strip())
    return gs


def read_associations(assoc_fn):
    assoc = {}
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

    return assoc


if __name__ == "__main__":

    import argparse
    p = argparse.ArgumentParser(__doc__,
                 formatter_class=argparse.ArgumentDefaultsHelpFormatter)

    p.add_argument('--glist',  type=str, dest='glist', required=True,
                 help="Genes list file")

    p.add_argument('--input',  type=str, dest='input', required=True,
                 help="Corresponding GO annotaion file")

    args = p.parse_args()
    assoc = read_associations(args.input)
    gene_set = read_geneset(args.glist)

    script_path = os.path.normpath( os.path.join( os.getcwd(), os.path.dirname(__file__) ) )
    obo_file_path = os.path.join( script_path, "go-basic.obo" )
    obo_dag = GODag(obo_file=obo_file_path)
    obo_dag.update_association(assoc)

    #print obo_dag
    t2gs = dict()
    for gene in (g for g in gene_set if g in assoc):
        for x in assoc[gene]:
            if x in obo_dag:
                if t2gs.has_key(obo_dag[x].id):
                    t2gs[obo_dag[x].id].append(gene)
                else:
                    t2gs[obo_dag[x].id] = list()
                    t2gs[obo_dag[x].id].append(gene)

    print "#id\tGO_term\tnamespace\tdescription\tlevel(shortest distance from root node)\tdepth(longest distance from root node)"

    for t in t2gs.keys():
        term = obo_dag[t]
        for g in t2gs[t]:
            print "%s\t%s\t%s\t%s\tlevel-%02d\tdepth-%02d" % ( g, term.id, term.namespace, term.name, term.level, term.depth )


