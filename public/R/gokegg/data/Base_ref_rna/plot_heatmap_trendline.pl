#!/usr/bin/perl -w

use strict;
use warnings;
use Getopt::Long;
use Thread::Semaphore;
use Cwd 'abs_path';
use threads::shared;

my %opts;
my $VERSION = "2014-12-30";
GetOptions (\%opts,"i=s","log=i","o=s","sub=i","w=f","h=f","cls=f","rls=f","ml=s","mr=s","clt=s");
my $usage = <<"USAGE";
Program : $0
Contact : $VERSION
Author : yan.wang\@majorbio.com
Description: ploting the heatmap and the subcluster line
Usage:perl $0 [options]	     
Options: 
	 -i   *         STRING  matrix for heatmap plot
	                        ################################
			        # gene_id sample1 sample2 ...
                                # gene1 12 24
                                # gene2 45 7
                                # ...
	-o   *          STRING output file prefix,default: cluster.heatmap.pdf (prefix.heatmap.pdf)
	-log            INT    converted the input data matrix by log function (log2 or log10,default: ignore this step)         
	-sub		INT	wheather plot the subcluster trendlines(0 or 1,default 0)
	-w              FLOAT  plot width(defalt will be 12)                                                                      
	-h              FLOAT  plot height(defalt will be:16)
	-cls            FLOAT  colum text size,default 1
	-rls            FLOAT  row text size,default 1
	-ml             FLOAT  low margin,default 8
	-mr             FLOAT  right margin,default 8
	-num		INT input the subcluster number,default  10;
	-clt		STRING  cluster type:both,row,colum or none (default,both)
USAGE
	 
die $usage if (!$opts{i});

#define defalts
$opts{i}=abs_path($opts{i});
$opts{o}=$opts{o}?abs_path($opts{o}):"heatmap";
$opts{sub}=$opts{sub}?$opts{sub}:0;
$opts{log}=$opts{log}?$opts{log}:0;
$opts{num}=$opts{num}?$opts{num}:10;
$opts{w}=$opts{w}?$opts{w}:12;
$opts{h}=$opts{h}?$opts{h}:16;
$opts{cls}=$opts{cls}?$opts{cls}:1;
$opts{rls}=$opts{rls}?$opts{rls}:0;
$opts{ml}=$opts{ml}?$opts{ml}:12;
$opts{mr}=$opts{mr}?$opts{mr}:12;
$opts{clt}=$opts{clt}?$opts{clt}:"both";

my $script_dir = $0;
   $script_dir =~ s/[^\/]+$//;
   chop($script_dir);
   $script_dir = "./" unless ($script_dir);



   
open RCMD, ">$opts{o}.r";
print RCMD "
options(warn=-100)
input_matrix<-\"$opts{i}\"
logNorm<-$opts{log}
width<-$opts{w}
height<-$opts{h}
clsize<-$opts{cls}
rlsize<-$opts{rls}
mlow<-$opts{ml}
mright<-$opts{mr}
cltype<-\"$opts{clt}\" #### both row column none

library(cluster)
library(gplots)
library(Biobase)

data = read.delim(input_matrix, header=T, check.names=F, sep=\"\t\")
rownames(data) = data[,1] # set rownames to gene identifiers
data = data[,2:length(data[1,])] # remove the gene column since its now the rowname value
data = as.matrix(data) # convert to matrix
colnames(data)<-colnames(data)
myheatcol = redblue(75)[75:1]

if(logNorm!=0){
data = log(data+1,base=logNorm)
centered_data = t(scale(t(data), scale=F)) # center rows, mean substracted
hc_genes = agnes(centered_data, diss=FALSE, metric=\"euclidean\") # cluster genes
hc_samples = hclust(as.dist(1-cor(centered_data, method=\"spearman\")), method=\"complete\") # cluster conditions
final_data<-centered_data
}
if(logNorm==0){
hc_genes = agnes(data,diss=FALSE, metric=\"euclidean\") # cluster genes
hc_samples = hclust(as.dist(1-cor(data, method=\"spearman\")), method=\"complete\") # cluster conditions
final_data<-data
}
if(cltype==\"both\"){Rowv=as.dendrogram(hc_genes);Colv=as.dendrogram(hc_samples)}
if(cltype==\"row\"){Rowv=as.dendrogram(hc_genes);Colv=NA}
if(cltype==\"column\"){Rowv=NV;Colv=as.dendrogram(hc_samples)}
if(cltype==\"none\"){Rowv=NA;Colv=NA}

#gene_partition_assignments <- cutree(as.hclust(hc_genes), k=subclustNum);
#partition_colors = rainbow(length(unique(gene_partition_assignments)), start=0.4, end=0.95)
#gene_colors = partition_colors[gene_partition_assignments]
save(list=ls(all=TRUE), file=\"all.RData\")

### cexRow cexCol
if(rlsize==0){ 
NR<-nrow(final_data)
if(NR<=100){rlsize = 1.2/log10(NR);}
if(NR>100 && NR<=300){rlsize = 1/log10(NR)}
if(NR>300 && NR<=500){rlsize = 0.5/log10(NR)}
if(NR>500 && NR<=1000){rlsize = 0.3/log10(NR)}
if(NR>1000){rlsize = 0.3/log10(NR);height=24}
}
####### output the odered matrix after clustered
if(cltype==\"both\"){
	order_mat<-data[hc_genes\$order[nrow(data):1],hc_samples\$order]
	write.table(order_mat,paste(\"$opts{o}\",\".heatmap.pdf.orderedmat.xls\",sep=\"\"),sep=\"\t\",col.names=T,row.names=T,quote=F)
	}
if(cltype==\"row\"){
	order_mat<-data[hc_genes\$order[nrow(data):1],]
	write.table(order_mat,paste(\"$opts{o}\",\".heatmap.pdf.orderedmat.xls\",sep=\"\"),sep=\"\t\",col.names=T,row.names=T,quote=F)
	 }


if($opts{sub}==1){subclustNum<-ifelse(nrow(order_mat)>200, $opts{num}, 5)}
####### heatmap-plot
heatmap_filename<-paste(\"$opts{o}\",\".heatmap.pdf\",sep=\"\")
pdf(file=heatmap_filename, width=width,height=height, paper=\"special\")

if($opts{sub}==0){
	heatmap.2(final_data, dendrogram=cltype,Rowv=Rowv,Colv=Colv,col=myheatcol, scale=\"none\", density.info=\"none\", trace=\"none\",cexCol=clsize, cexRow=rlsize,lhei=c(0.3,2), lwid=c(2.5,4),margins=c(mlow,mright))
}
if($opts{sub}==1){
	gene_partition_assignments <- cutree(as.hclust(hc_genes), k=subclustNum);
	partition_colors = rainbow(length(unique(gene_partition_assignments)), start=0.4, end=0.95)
	gene_colors = partition_colors[gene_partition_assignments]
	heatmap.2(final_data, dendrogram=cltype,Rowv=Rowv,Colv=Colv,col=myheatcol, RowSideColors=gene_colors, scale=\"none\", density.info=\"none\", trace=\"none\",cexCol=clsize, cexRow=rlsize,lhei=c(0.3,2), lwid=c(2.5,4),margins=c(mlow,mright))
}
dev.off()

####### plot the subclusters trendlines
subcluster_out = paste(\"$opts{o}\",\"_subclusters_\",subclustNum,sep=\"\")
dir.create(subcluster_out)
gene_names = rownames(final_data)
num_cols = length(final_data[1,])
for (i in 1:subclustNum) {
	partition_i = (gene_partition_assignments == i)
	partition_centered_data = final_data[partition_i,]
	# if the partition involves only one row, then it returns a vector instead of a table
	if (sum(partition_i) == 1){
	dim(partition_centered_data) = c(1,num_cols)
	colnames(partition_centered_data) = colnames(final_data)
	rownames(partition_centered_data) = gene_names[partition_i]
	}
	outfile = paste(subcluster_out, \"/subcluster_\", i, sep='')
	write.table(partition_centered_data, file=outfile, quote=F, sep=\"\t\")
}
files = list.files(subcluster_out)
ncols<-ceiling(length(files)/2)
pdf(file=paste(\"$opts{o}\",\".trendlines_for_\",subclustNum,\"_subclusters.pdf\",sep=\"\"))
par(mfrow=c(2,2))
par(cex=0.6)
par(mar=c(7,4,4,2))
for (i in 1:length(files)) {
    data = read.delim(paste(subcluster_out,files[i],sep=\"/\"), header=T, row.names=1)
    ymin = min(data); ymax = max(data);
    plot_label = paste(files[i], ', ', length(data[,1]), \" genes\", sep='')
    plot(as.numeric(data[1,]), type='l', ylim=c(ymin,ymax), main=plot_label, col='lightgray', xaxt='n', xlab='', ylab='centered log2(fpkm+1)')
    axis(side=1, at=1:length(data[1,]), labels=colnames(data), las=2)
    for(r in 2:length(data[,1])) {
        points(as.numeric(data[r,]), type='l', col='lightgray')
    }
    points(as.numeric(colMeans(data)), type='o', col='blue')
}
dev.off()

";



#system ("R --restore --no-save < $opts{o}.r");
`R --restore --no-save < $opts{o}.r`;
#`R --restore --quiet --no-save <cmd.r`
#system('rm *.r');
if(-e "all.RData"){system("rm all.RData");}



