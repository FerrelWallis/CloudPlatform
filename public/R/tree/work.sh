/lustre/sdb/xueyj/conda//lib/bin/Rscript /lustre/sdb/xueyj/samplepackage/tree/tree2.0.R -i xup.tre -o /lustre/sdb/xueyj/samplepackage/tree -in xup_nogroup -if png
/lustre/sdb/xueyj/conda//lib/bin/Rscript /lustre/sdb/xueyj/samplepackage/tree/tree2.0.R -i xup.tre -o /lustre/sdb/xueyj/samplepackage/tree -g /lustre/sdb/xueyj/samplepackage/tree/xup.group.txt -in xup_group_nolegend -if png -sl FALSE
/lustre/sdb/xueyj/conda//lib/bin/Rscript /lustre/sdb/xueyj/samplepackage/tree/tree2.0.R -i xup.tre -o /lustre/sdb/xueyj/samplepackage/tree -g /lustre/sdb/xueyj/samplepackage/tree/xup.group.txt -in xup_group_legend -if png -sl TRUE -ln 2
/lustre/sdb/xueyj/conda//lib/bin/Rscript /lustre/sdb/xueyj/samplepackage/tree/tree2.0.R -i 7.16s.tre -o /lustre/sdb/xueyj/samplepackage/tree -in 7.16s_longStr -ss 3 -if png -is 40:23
/lustre/sdb/xueyj/conda//lib/bin/Rscript /lustre/sdb/xueyj/samplepackage/tree/tree2.0.R -i sunxa_result.UBCG_gsi92.codon.50.label.nwk -o /lustre/sdb/xueyj/samplepackage/tree -in support -if png -ss 2.5 -ssb TRUE -fs 8