if (!requireNamespace("BiocManager", quietly = TRUE))
  +     install.packages("BiocManager")
BiocManager::install("edgeR")
install.packages("statmod")
pacman::p_load(edgeR, statmod,ggplot2,lazyopt, EnhancedVolcano, dplyr, magrittr) 

args <- c("-i","F:/CloudPlatform/files/examples/PCA_table.txt",
          "-g","F:/CloudPlatform/files/examples/PCA_group.txt",
          "-o","F:/CloudPlatform/R/edger")

opt<-matrix(c("tablepath", "i", 2, "character", "The path to the table data read","",
              "grouppath", "g", 2, "character", "The path to the group data read","",
              "outpath", "o", 2, "character", "The package path of the output", "",
              "help", "h", 0, "numeric", "Help document", "1"
), byrow = TRUE, ncol = 6)%>%lazyopt(args)
              
              
targets <- read.delim(opt$tablepath,row.names = 1, sep = '\t', check.names = FALSE)
Group=read.delim(opt$grouppath,header=FALSE,row.names = 1, sep = '\t', check.names = FALSE)
group=Group$V2
dgelist <- DGEList(counts = targets, group = group)
dgelist_norm <- calcNormFactors(dgelist, method = 'TMM')
design <- model.matrix(~group)
dge <- estimateDisp(dgelist_norm, design, robust = TRUE)
fit <- glmFit(dge, design, robust = TRUE)
lrt <- topTags(glmLRT(fit), n = nrow(dgelist$counts))

write.table(lrt, paste0(opt$outpath, "/edger.xls"), sep = '\t', col.names = NA, quote = FALSE)

gene_diff <- lrt$table
# gene_diff <- read.delim(paste0(opt$outpath, "/edger.xls"), row.names = 1, sep = '\t', check.names = FALSE)
gene_diff <- gene_diff[order(gene_diff$FDR, gene_diff$logFC, decreasing = c(FALSE, TRUE)), ]

gene_diff[which(gene_diff$logFC >= 1 & gene_diff$FDR < 0.01),'sig'] <- 'up'
gene_diff[which(gene_diff$logFC <= -1 & gene_diff$FDR < 0.01),'sig'] <- 'down'
gene_diff[which(abs(gene_diff$logFC) <= 1 | gene_diff$FDR >= 0.01),'sig'] <- 'none'

gene_diff_select <- subset(gene_diff, sig %in% c('up', 'down'))
write.table(gene_diff_select, file = paste0(opt$outpath, "/edger.select.xls"),sep = '\t', col.names = NA, quote = FALSE)

gene_diff_up <- subset(gene_diff, sig == 'up')
gene_diff_down <- subset(gene_diff, sig == 'down')
write.table(gene_diff_up, file= paste0(opt$outpath, "/edger.up.xls"), sep = '\t', col.names = NA, quote = FALSE)
write.table(gene_diff_down,file= paste0(opt$outpath, "/edger.down.xls"), sep = '\t', col.names = NA, quote = FALSE)

df=subset(gene_diff,select = c(logFC,PValue,FDR))
Geneid=row.names(df)
rownames(df)=NULL
data=cbind(Geneid,df)
colnames(data)=c("Geneid","log2(fc)","pvalue","FDR")
write.table(data, file=paste0(opt$outpath, "/edger.gene_diff.txt"), sep = '\t', col.names = NA, quote = FALSE)
