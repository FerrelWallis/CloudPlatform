pacman::p_load(reshape2, dplyr, lazyopt, magrittr, vegan, maptools, cluster)

arg <- c("-i", "F:/CloudPlatform/R/nmds/nmds_table.txt",
         "-o", "F:/CloudPlatform/R/nmds"
)

#是否在p值的正态近似中应用连续性校正
opt <- matrix(c("inpath", "i", 2, "character", "input distance matrix", "",
                "outpath", "o", 2, "character", "output dir", "",
                "dimension", "dim", 1, "numeric", "Number of dimensions of NMDS space,default: 2", "2",
                "help", "h", 0, "numeric", "Help document", "1"
), byrow = TRUE, ncol = 6) %>% lazyopt()

# read otubased metics 
da <- read.table(opt$inpath, sep="\t", head=T, check.names=F) %>% set_rownames(.[,1]) %>% .[,-1] %>% t() %>% data.frame()

# if nmds analysis
nmds <- metaMDS(da, k=opt$dimension)
write.table(nmds$points, paste0(opt$outpath, "/nmds_sites.xls"), sep="\t", col.names = NA, quote = F)


