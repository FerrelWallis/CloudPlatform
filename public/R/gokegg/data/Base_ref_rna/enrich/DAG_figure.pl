#!usr/bin/perl -w

use strict;

die "Note:sed -e 's/;/, /g' GO.list  >GO.list.tr	\nusage:perl $0 <diff.gene.list.tr> <enrich.tr> <output:prefix> <GO.list.tr>\n" unless @ARGV == 4;
print "library(\"Rgraphviz\")
library(\"topGO\")	
geneID2GO <- readMappings(file =\"$ARGV[3]\")	
GO2geneID <- inverseList(geneID2GO)	
BPterms <- ls(GOBPTerm)
CCterms <- ls(GOCCTerm)
MFterms <- ls(GOMFTerm)
geneNames<-names(geneID2GO)
diffgene <- readMappings(file = \"$ARGV[0]\")
myInter<-names(diffgene)	
geneList<-factor(as.integer(geneNames %in% myInter));
names(geneList)<-geneNames
GOdata_bp <- new(\"topGOdata\", ontology = \"BP\", allGenes = geneList,annot = annFUN.gene2GO, gene2GO = geneID2GO)
GOdata_cc <- new(\"topGOdata\", ontology = \"CC\", allGenes = geneList,annot = annFUN.gene2GO, gene2GO = geneID2GO)
GOdata_mf <- new(\"topGOdata\", ontology = \"MF\", allGenes = geneList,annot = annFUN.gene2GO, gene2GO = geneID2GO)
gos<-read.delim(\"$ARGV[1]\",header=TRUE,check.names=FALSE,sep=\"\\t\");		
enrich_mf<-gos[which(gos[,2]==\"MF\"),4];
names(enrich_mf)<-gos[which(gos[,2]==\"MF\"),1];		
enrich_bp<-gos[which(gos[,2]==\"BP\"),4];
names(enrich_bp)<-gos[which(gos[,2]==\"BP\"),1];		
enrich_cc<-gos[which(gos[,2]==\"CC\"),4];
names(enrich_cc)<-gos[which(gos[,2]==\"CC\"),1];
pdf(file='$ARGV[2]\_bp_DAG.pdf',width=16,height=7);
showSigOfNodes(GOdata_bp, enrich_bp, firstSigNodes = 5, useInfo = 'all')
dev.off()
pdf(file='$ARGV[2]\_mf_DAG.pdf',width=16,height=7);
showSigOfNodes(GOdata_mf, enrich_mf, firstSigNodes = 5, useInfo = 'all')
dev.off()
pdf(file='$ARGV[2]\_cc_DAG.pdf',width=16,height=7);
showSigOfNodes(GOdata_cc, enrich_cc, firstSigNodes = 5, useInfo = 'all')
dev.off()\n";
