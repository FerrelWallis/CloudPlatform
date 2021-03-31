summarize_taxa.py -i test/otu_table.biom -o test/tax_summary_a -L 1,2,3,4,5,6,7 -a

/mnt/sdb/ww/CloudPlatform/R/lefse2.0/plot-lefse.pl -i test/tax_summary_a/ -o test/ -m test/lefse_map.txt -g Group -l 7

/mnt/sdb/ww/CloudPlatform/R/lefse2.0/lefse_to_export/format_input.py  /mnt/sdb/ww/CloudPlatform/R/lefse2.0/test/lefse_input.txt  /mnt/sdb/ww/CloudPlatform/R/lefse2.0/test/lefse_format.txt  -f r -c 1 -u 2 -o 1000000

/mnt/sdb/ww/CloudPlatform/R/lefse2.0/lefse_to_export/run_lefse.py /mnt/sdb/ww/CloudPlatform/R/lefse2.0/test/lefse_format.txt /mnt/sdb/ww/CloudPlatform/R/lefse2.0/test/lefse_LDA.xls 

/mnt/sdb/ww/CloudPlatform/R/lefse2.0/lefse_to_export/plot_res.py /mnt/sdb/ww/CloudPlatform/R/lefse2.0/test/lefse_LDA.xls /mnt/sdb/ww/CloudPlatform/R/lefse2.0/test/lefse_LDA.pdf --dpi 300 --format pdf --width 8

/mnt/sdb/ww/CloudPlatform/R/lefse2.0/lefse_to_export/plot_cladogram.py /mnt/sdb/ww/CloudPlatform/R/lefse2.0/test/lefse_LDA.xls /mnt/sdb/ww/CloudPlatform/R/lefse2.0/test/lefse_LDA.cladogram.pdf --format pdf --dpi 300

/mnt/sdb/ww/CloudPlatform/R/lefse2.0/lefse_to_export/plot_features.py /mnt/sdb/ww/CloudPlatform/R/lefse2.0/test/lefse_format.txt /mnt/sdb/ww/CloudPlatform/R/lefse2.0/test/lefse_LDA.xls /mnt/sdb/ww/CloudPlatform/R/lefse2.0/test/lefse_LDA.features.pdf  --format pdf --dpi 300 --width 8

