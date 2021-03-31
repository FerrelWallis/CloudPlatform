#!/usr/bin/perl -w

use strict;
use warnings;
use Getopt::Long;
my %opts;
GetOptions (\%opts,"f=s","l=s","w=i","h=i","ls=f","ts=f");

my $usage = <<"USAGE";
        Program : $0
        Discription: plot venn for differently expressed genes.
        Usage:perl $0 [options]
                -f      files     a,b,c
                -l      string    labels x,y,z   
                -w      int       image width
                -h      int       image height
		-ls      float     lable size
		-ts      float     title size

USAGE
die $usage if ( !($opts{f}&&$opts{l}));
$opts{w}=$opts{w}?$opts{w}:10;
$opts{h}=$opts{h}?$opts{h}:10;
$opts{ls}=$opts{ls}?$opts{ls}:1;
$opts{ts}=$opts{ts}?$opts{ts}:1;


my @id=split /,/,$opts{f};my @num;
foreach my $k(@id){
    open A,$k;
    my $re=0;
    while(<A>){
        $re++;
    }
    close A;
    push @num,$re;
}

@id=split /,/,$opts{l};
#$opts{l}=reverse $opts{l};
my $id="";
my @c;
#print "$opts{l}";
foreach my $k(0..$#id){
#	print "$k";
    $id=$id[$k]."\n".$num[$k].",";
	push @c,$id;
#	$id=reverse $id;
#	$id=$id[$k]."\n".$num[$k].",".$id;
#	print "@c";
}




open RCMD, ">cmd.r";
print RCMD "
files<-unlist(strsplit(\"$opts{f}\",\",\",fix=T))
Lables<-unlist(strsplit(\"$opts{l}\",\",\",fix=T))
Lables2<-unlist(strsplit(\"@c\",\",\",fix=T))
InputList<-list()
for(i in 1:length(files)){
    genes<-scan(file=files[i],what=character())
    InputList[[i]]<-genes
}
names(InputList)<-Lables2
if(length(Lables)==2){fillColor<-c(\"dodgerblue\", \"goldenrod1\")}
if(length(Lables)==3){fillColor<-c(\"dodgerblue\", \"goldenrod1\",\"blue\")}
if(length(Lables)==4){fillColor<-c(\"dodgerblue\", \"goldenrod1\",\"blue\",\"red\")}
if(length(Lables)==5){fillColor<-c(\"dodgerblue\", \"goldenrod1\",\"blue\",\"red\",\"grey\")}

library(\"VennDiagram\")
outname<-paste(Lables,collapse=\"-\")
pdf(file = paste(outname,\".venn.pdf\",sep=\"\"),width=$opts{w},height=$opts{h})
venn.plot<-venn.diagram(InputList,filename = NULL,col = \"black\",fill = fillColor,alpha = 0.50,cat.cex = $opts{ls},cat.fontface = \"bold\",margin = 0.15,main.cex=$opts{ts},scale=TRUE)
grid.draw(venn.plot)
dev.off()

";
close RCMD;
system ('R --restore --no-save < cmd.r');
#system ('rm cmd.r');	







