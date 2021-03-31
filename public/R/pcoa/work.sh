Rscript pcoa-data.R -i /lustre/sdb/xueyj/samplepackage/pcoa/table.txt -o /lustre/sdb/xueyj/samplepackage/pcoa/data/

Rscript pcoa-plot.R -i ./data/PCOA.x.xls -si ./data/PCOA.sdev.xls -o ./image

Rscript pcoa-plot.R -i ./data/PCOA.x.xls -si ./data/PCOA.sdev.xls -o ./image -in pcoagroup -g group.txt





