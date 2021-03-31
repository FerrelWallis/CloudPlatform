Rscript pie_full_ring.R -i phylum.txt -o ./ -in pie_full
Rscript pie_full_ring.R -i phylum.txt -o ./ -in pie_ring -pt ring
Rscript pie_full_ring.R -i phylum.txt -o ./ -in pie_full_color -c red,black,pink,blue,yellow,green
Rscript pie_full_ring.R -i phylum.txt -o ./ -in pie_label -ts pietest:18:blue
Rscript pie_3d_fold.R -i phylum.txt -o ./ -pt 3d -in 3d -lc '0.5'
Rscript pie_3d_fold.R -i phylum.txt -o ./ -pt fold -in fold
