perl tr_go_relations.pl gene_ontology.1_2.obo go.list >kk8
perl step2_produce_express_info.pl go2.list kk2 /mnt/lustre/users/zengl/work/dengwd/work1220_color/go/GO.list /mnt/lustre/users/zengl/work/dengwd/work1220_color/diff_gene/transcripts.counts.matrix.two_months_black_vs_two_months_black_white.edgeR.DE_results >kk
perl go_relation.pl go-basic.obo >GO_root_GO.info

perl GO_level123.pl GO_root_GO.info2 GO_depth.info.txt >GO_level123.txt

