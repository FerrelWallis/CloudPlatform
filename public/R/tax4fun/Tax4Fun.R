pacman::p_load(reshape2, dplyr, lazyopt, magrittr, tibble, Tax4Fun)

arg <- c("-i", "F:/CloudPlatform/R/tax4fun/out/otu_taxa_table.format.txt",
         "-o", "F:/CloudPlatform/R/tax4fun/out",
         "-rp", "F:/CloudPlatform/R/tax4fun/db",
         "-r", "SILVA123",
         "-ref", "UProC"
)

#是否在p值的正态近似中应用连续性校正
opt <- matrix(c("tablepath", "i", 2, "character", "The path to the table data read", "",
                "outputfilepath", "o", 2, "character", "The package path of the output image", "",
                "Rpath", "rp", 2, "character", "The package path of the output image", "",
                "reference", "r", 1, "character", "Choose taxonomy annation database", "SILVA123",
                "fctProfiling", "fct", 1, "logical", "if TRUE (default) the functional capabilities of microbial communities based on 16S data samples are computed using the precomputed KEGG Ortholog eference profiles, and if FALSE the metabolic capabilities using the pre-computed KEGG Pathway reference profiles according to the MoP approach are computed.", "TRUE",
                "refProfile", "ref", 1, "character", "Giving the method for precomputing the functional reference profiles. This must be either 'UProC' (default) or 'PAUDA'.", "UProC",
                "shortReadMode", "srm", 1 , "logical", "if TRUE (default) the functional reference profiles are computed based on 100 bp reads, and if FALSE the reference profiles are computed based on 400 bp reads.", "TRUE",
                "help", "h", 0, "numeric", "Help document", "1"
), byrow = TRUE, ncol = 6) %>% lazyopt(arg)

# table <- read.table(opt$tablepath, header=T, quote="", sep="\t", check.names=F)
QIIMESingleData <- importQIIMEData(opt$tablepath)
Tax4FunOutput <- Tax4Fun(QIIMESingleData, paste0(opt$Rpath, "/", opt$reference), fctProfiling = opt$fctProfiling, refProfile = opt$refProfile, shortReadMode = opt$shortReadMode, normCopyNo = TRUE) 
KO_table <- t(Tax4FunOutput$Tax4FunProfile)
write.table(KO_table, file=paste0(opt$outputfilepath,"/KO_table.txt"), quote = FALSE, sep="\t", eol = "\n", na = "NA", dec = ".", row.names = TRUE,col.names = TRUE)
